package io.aygh.customer.service.command.impl;

import io.aygh.customer.dto.request.CustomerRequest;
import io.aygh.customer.dto.request.CustomerSettlementRequest;
import io.aygh.customer.dto.response.CustomerResponse;
import io.aygh.customer.dto.response.CustomerSettlementResponse;
import io.aygh.customer.dto.response.SettledInvoiceSummary;
import io.aygh.customer.entity.Customer;
import io.aygh.customer.helper.CustomerResolver;
import io.aygh.customer.helper.CustomerValidation;
import io.aygh.customer.mapper.CustomerMapper;
import io.aygh.customer.repository.CustomerRepository;
import io.aygh.customer.service.command.CustomerCommandService;
import io.aygh.exception.BusinessException;
import io.aygh.sales.entity.Sale;
import io.aygh.sales.repository.SaleRepository;
import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerCommandServiceImpl implements CustomerCommandService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final CustomerResolver resolver;
    private final CustomerValidation validation;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponse create(CustomerRequest request) {
        validation.requirePhoneAvailable(request.phone(), null);
        validation.requirePanAvailable(request.panNumber(), null);

        Customer customer = customerMapper.toEntity(request);
        if (request.active() != null) {
            customer.setActive(request.active());
        }
        Customer saved = customerRepository.save(customer);
        log.info("Registered customer '{}' ({})", saved.getName(), saved.getPhone());
        return customerMapper.toResponse(saved);
    }

    @Override
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = resolver.customer(id);

        validation.requirePhoneAvailable(request.phone(), id);
        validation.requirePanAvailable(request.panNumber(), id);

        customerMapper.applyUpdate(request, customer);
        if (request.active() != null) {
            customer.setActive(request.active());
        }
        Customer saved = customerRepository.save(customer);
        log.info("Updated customer '{}' ({})", saved.getName(), saved.getPhone());
        return customerMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Customer customer = resolver.customer(id);
        customerRepository.delete(customer);
        log.info("Removed customer '{}' ({})", customer.getName(), customer.getPhone());
    }

    @Override
    public CustomerSettlementResponse settle(Long id, CustomerSettlementRequest request) {
        Customer customer = resolver.customer(id);

        List<Sale> unpaidSales = saleRepository.findUnpaidSalesByCustomerId(id);
        BigDecimal totalDebt = unpaidSales.stream()
                .map(Sale::dueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Customer '" + customer.getName() + "' has no outstanding credit to settle");
        }

        if (request.amount().compareTo(totalDebt) > 0) {
            throw new BusinessException(String.format(
                    "Settlement amount %s exceeds total outstanding balance %s for customer '%s'",
                    request.amount(), totalDebt, customer.getName()));
        }

        PaymentMethod paymentMethod = request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.CASH;
        BigDecimal remainingToAllocate = request.amount();
        List<SettledInvoiceSummary> settledInvoices = new ArrayList<>();

        for (Sale sale : unpaidSales) {
            if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal due = sale.dueAmount();
            BigDecimal allocate = remainingToAllocate.min(due);
            BigDecimal previouslyPaid = sale.getPaidAmount();

            sale.settle(previouslyPaid.add(allocate));

            if (sale.getPaymentStatus() == PaymentStatus.PAID) {
                sale.setPaymentMethod(paymentMethod);
            }
            if (request.remark() != null && !request.remark().isBlank()) {
                String existingRemark = sale.getRemark() == null ? "" : sale.getRemark() + " | ";
                sale.setRemark(existingRemark + "Settlement: " + request.remark());
            }

            saleRepository.save(sale);

            settledInvoices.add(new SettledInvoiceSummary(
                    sale.getId(),
                    sale.getInvoiceNumber(),
                    sale.getNetTotal(),
                    previouslyPaid,
                    allocate,
                    sale.dueAmount(),
                    sale.getPaymentStatus()
            ));

            remainingToAllocate = remainingToAllocate.subtract(allocate);
        }

        BigDecimal remainingBalance = totalDebt.subtract(request.amount());
        log.info("Settled {} for customer '{}' ({}) across {} invoice(s), remaining balance {}",
                request.amount(), customer.getName(), customer.getId(), settledInvoices.size(), remainingBalance);

        return new CustomerSettlementResponse(
                customer.getId(),
                customer.getName(),
                request.amount(),
                totalDebt,
                remainingBalance,
                paymentMethod,
                settledInvoices
        );
    }
}
