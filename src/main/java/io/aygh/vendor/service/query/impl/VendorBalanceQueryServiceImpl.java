package io.aygh.vendor.service.query.impl;

import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import io.aygh.vendor.dto.response.VendorBalanceResponse;
import io.aygh.vendor.dto.response.VendorBalanceSummaryResponse;
import io.aygh.vendor.entity.BalanceType;
import io.aygh.vendor.helper.VendorResolver;
import io.aygh.vendor.mapper.VendorBalanceMapper;
import io.aygh.vendor.repository.VendorBalanceRepository;
import io.aygh.vendor.repository.VendorBalanceTotals;
import io.aygh.vendor.service.query.VendorBalanceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorBalanceQueryServiceImpl implements VendorBalanceQueryService {

    private final VendorBalanceRepository vendorBalanceRepository;
    private final VendorResolver resolver;
    private final VendorBalanceMapper vendorBalanceMapper;

    @Override
    public PagedResponse<VendorBalanceResponse> findTransactions(Long vendorId, Pageable pageable) {
        resolver.requireVendor(vendorId);

        Page<VendorBalanceResponse> page = vendorBalanceRepository.findByVendorId(vendorId, pageable)
                .map(vendorBalanceMapper::toResponse);

        return PaginationUtils.toPagedResponse(page);
    }

    /**
     * The stored totals are one-directional; what a caller wants is who is behind
     * and by how much. A negative net means the mart overpaid, so it is reported
     * as a receivable rather than as a negative payable — a minus sign in front
     * of "payable" is the kind of thing that gets read wrong on a screen.
     */
    @Override
    public VendorBalanceSummaryResponse findSummary(Long vendorId) {
        resolver.requireVendor(vendorId);

        VendorBalanceTotals totals = vendorBalanceRepository.totalsFor(vendorId);
        BigDecimal net = totals.outstanding();

        return new VendorBalanceSummaryResponse(
                vendorId,
                net.abs(),
                net.signum() < 0 ? BalanceType.RECEIVABLE : BalanceType.PAYABLE,
                totals.payable(),
                totals.receivable(),
                totals.settled());
    }
}
