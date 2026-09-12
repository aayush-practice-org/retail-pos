package io.aygh.customer.service.query.impl;

import io.aygh.customer.dto.response.CustomerOutstandingResponse;
import io.aygh.customer.dto.response.CustomerResponse;
import io.aygh.customer.entity.Customer;
import io.aygh.customer.helper.CustomerResolver;
import io.aygh.customer.mapper.CustomerMapper;
import io.aygh.customer.repository.CustomerRepository;
import io.aygh.customer.service.query.CustomerQueryService;
import io.aygh.exception.ResourceNotFoundException;
import io.aygh.sales.dto.response.SaleSummaryResponse;
import io.aygh.sales.entity.Sale;
import io.aygh.sales.mapper.SaleMapper;
import io.aygh.sales.repository.SaleRepository;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerQueryServiceImpl implements CustomerQueryService {

    private final CustomerRepository customerRepository;
    private final CustomerResolver resolver;
    private final CustomerMapper customerMapper;
    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;

    @Override
    public CustomerResponse findById(Long id) {
        return customerMapper.toResponse(resolver.customer(id));
    }

    @Override
    public CustomerResponse findByPhone(String phone) {
        return customerRepository.findByPhone(phone)
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "phone", phone));
    }

    @Override
    public PagedResponse<CustomerResponse> findAll(String search, Pageable pageable) {
        Specification<Customer> spec = matches(search);
        Page<Customer> page = spec == null
                ? customerRepository.findAll(pageable)
                : customerRepository.findAll(spec, pageable);

        return PaginationUtils.toPagedResponse(page, page.map(customerMapper::toResponse).getContent());
    }

    @Override
    public List<CustomerResponse> findAllForSelection() {
        return customerMapper.toResponses(customerRepository.findAll(Sort.by("name")));
    }

    @Override
    public CustomerOutstandingResponse getOutstanding(Long id) {
        Customer customer = resolver.customer(id);

        List<Sale> unpaidSales = saleRepository.findUnpaidSalesByCustomerId(id);
        BigDecimal totalOutstanding = unpaidSales.stream()
                .map(Sale::dueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditLimit = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal availableCredit = creditLimit.subtract(totalOutstanding).max(BigDecimal.ZERO);

        List<SaleSummaryResponse> unpaidSummaries = unpaidSales.stream()
                .map(sale -> saleMapper.toSummary(sale, 0))
                .toList();

        return new CustomerOutstandingResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                creditLimit,
                totalOutstanding,
                availableCredit,
                unpaidSales.size(),
                unpaidSummaries
        );
    }

    private static Specification<Customer> matches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("panNumber")), pattern));
    }
}
