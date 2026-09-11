package io.aygh.sales.service.command.impl;

import io.aygh.customer.entity.Customer;
import io.aygh.customer.helper.CustomerResolver;
import io.aygh.exception.BusinessException;
import io.aygh.inventory.entity.Product;
import io.aygh.inventory.entity.ProductSellingUnit;
import io.aygh.sales.dto.request.SaleItemRequest;
import io.aygh.sales.dto.request.SalePaymentRequest;
import io.aygh.sales.dto.request.SaleRequest;
import io.aygh.sales.dto.response.SaleDetailResponse;
import io.aygh.sales.entity.Sale;
import io.aygh.sales.entity.SaleChannel;
import io.aygh.sales.entity.SaleItem;
import io.aygh.sales.helper.InvoiceNumberGenerator;
import io.aygh.sales.helper.SaleCalculator;
import io.aygh.sales.helper.SaleResolver;
import io.aygh.sales.mapper.SaleMapper;
import io.aygh.sales.repository.SaleRepository;
import io.aygh.sales.service.command.SaleCommandService;
import io.aygh.shared.entity.PaymentMethod;
import io.aygh.shared.entity.PaymentStatus;
import io.aygh.stock.entity.StockMovementType;
import io.aygh.stock.entity.StockReferenceType;
import io.aygh.stock.service.command.StockLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SaleCommandServiceImpl implements SaleCommandService {

    private final SaleRepository saleRepository;
    private final SaleResolver resolver;
    private final CustomerResolver customerResolver;
    private final SaleCalculator calculator;
    private final SaleMapper saleMapper;
    private final InvoiceNumberGenerator invoiceNumbers;
    private final StockLedgerService stockLedger;

    @Override
    public SaleDetailResponse create(SaleRequest request) {
        Customer customer = null;
        if (request.customerId() != null) {
            customer = customerResolver.customer(request.customerId());
        } else if (request.paymentMethod() == PaymentMethod.CREDIT) {
            throw new BusinessException("A registered customer is required for credit sales");
        }

        String customerName = request.customerName() != null ? request.customerName()
                : (customer != null ? customer.getName() : null);
        String customerPhone = request.customerPhone() != null ? request.customerPhone()
                : (customer != null ? customer.getPhone() : null);
        String customerPan = request.customerPan() != null ? request.customerPan()
                : (customer != null ? customer.getPanNumber() : null);

        Sale sale = Sale.builder()
                .invoiceNumber(invoiceNumbers.next())
                .soldAt(Instant.now())
                .channel(request.channel() == null ? SaleChannel.POS : request.channel())
                .taxScheme(request.taxScheme())
                .paymentMethod(request.paymentMethod())
                .customer(customer)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .customerPan(customerPan)
                .discountAmount(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount())
                .remark(request.remark())
                .build();

        List<SaleItem> items = new ArrayList<>();
        for (SaleItemRequest line : request.items()) {
            SaleItem item = buildItem(line);
            sale.addItem(item);
            items.add(item);
        }

        calculator.applyTotals(sale, items);

        if (sale.getDiscountAmount().compareTo(sale.getSubTotal()) > 0) {
            throw new BusinessException("The discount is more than the basket comes to");
        }

        // Credit leaves the bill owing whatever was not handed over; everything
        // else is taken as tendered in full unless the caller said otherwise.
        BigDecimal tendered = request.tenderedAmount() != null
                ? request.tenderedAmount()
                : (request.paymentMethod() == PaymentMethod.CREDIT ? BigDecimal.ZERO : sale.getNetTotal());
        sale.settle(tendered);

        Sale saved = saleRepository.save(sale);

        // After the id exists, so every movement points back at the bill that
        // caused it. This is also where an oversell is caught: the ledger checks
        // each line against the locked stock row and fails the whole sale.
        for (SaleItem item : saved.getItems()) {
            stockLedger.post(
                    item.getProduct(),
                    StockMovementType.SALE_OUT,
                    item.getQuantityInBaseUnits(),
                    item.getQuantity(),
                    item.getSellingUnit().getUnit(),
                    saved.getId(),
                    StockReferenceType.SALE,
                    "Invoice " + saved.getInvoiceNumber());
        }

        log.info("Billed {} — {} line(s), net {}, {}",
                saved.getInvoiceNumber(), saved.getItems().size(),
                saved.getNetTotal(), saved.getPaymentStatus());

        return saleMapper.toDetail(saved);
    }

    @Override
    public SaleDetailResponse pay(Long saleId, SalePaymentRequest request) {
        Sale sale = resolver.sale(saleId);

        if (sale.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Invoice " + sale.getInvoiceNumber() + " is already settled");
        }

        // settle() takes the total handed over across the life of the bill, not
        // this instalment, so the running total goes in.
        sale.settle(sale.getPaidAmount().add(request.amount()));

        if (request.paymentMethod() != null) {
            sale.setPaymentMethod(request.paymentMethod());
        }

        Sale saved = saleRepository.save(sale);
        log.info("Took {} against invoice {} — now {}",
                request.amount(), saved.getInvoiceNumber(), saved.getPaymentStatus());

        return saleMapper.toDetail(saved);
    }

    private SaleItem buildItem(SaleItemRequest line) {
        Product product = resolver.product(line.productId());
        ProductSellingUnit unit = resolver.sellingUnit(product, line.sellingUnitId());

        BigDecimal rate = line.rate() != null ? line.rate() : unit.getSellingPrice();
        if (rate == null) {
            throw new BusinessException("No price for '" + product.getName() + "' in "
                    + unit.getUnit().getName());
        }

        BigDecimal packQuantity = unit.getPackQuantity();
        if (packQuantity == null || packQuantity.signum() <= 0) {
            throw new BusinessException("'" + unit.getUnit().getName() + "' on '" + product.getName()
                    + "' has no pack quantity, so the stock it uses cannot be worked out");
        }

        BigDecimal discount = line.discountAmount() == null ? BigDecimal.ZERO : line.discountAmount();
        BigDecimal gross = line.quantity().multiply(rate).setScale(2, RoundingMode.HALF_UP);

        if (discount.compareTo(gross) > 0) {
            throw new BusinessException("The discount on '" + product.getName()
                    + "' is more than the line comes to");
        }

        return SaleItem.builder()
                .product(product)
                .sellingUnit(unit)
                // Copied so a reprint reads as it was handed over, whatever the
                // catalogue has been renamed to since.
                .productName(product.getName())
                .unitSymbol(unit.getUnit().getSymbol())
                .quantity(line.quantity())
                .packQuantity(packQuantity)
                .quantityInBaseUnits(line.quantity().multiply(packQuantity))
                .rate(rate)
                .mrp(unit.getMrp())
                .discountAmount(discount)
                .lineTotal(gross.subtract(discount))
                .build();
    }
}
