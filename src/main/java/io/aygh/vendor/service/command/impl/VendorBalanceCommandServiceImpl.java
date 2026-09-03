package io.aygh.vendor.service.command.impl;

import io.aygh.vendor.dto.request.VendorLedgerEntryRequest;
import io.aygh.vendor.dto.request.VendorSettlementRequest;
import io.aygh.vendor.dto.response.VendorBalanceResponse;
import io.aygh.vendor.entity.BalanceType;
import io.aygh.vendor.entity.Vendor;
import io.aygh.vendor.entity.VendorBalance;
import io.aygh.vendor.helper.VendorResolver;
import io.aygh.vendor.helper.VendorValidation;
import io.aygh.vendor.mapper.VendorBalanceMapper;
import io.aygh.vendor.repository.VendorBalanceRepository;
import io.aygh.vendor.service.command.VendorBalanceCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Every write here is an insert. Nothing in this service edits or removes an
 * entry, which is what makes a vendor's balance reconstructable from its ledger
 * at any point rather than only as it stands now.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorBalanceCommandServiceImpl implements VendorBalanceCommandService {

    private final VendorBalanceRepository vendorBalanceRepository;
    private final VendorResolver resolver;
    private final VendorValidation validation;
    private final VendorBalanceMapper vendorBalanceMapper;

    @Override
    public VendorBalanceResponse post(Long vendorId, VendorLedgerEntryRequest request) {
        validation.rejectSettlementEntry(request.balanceType());
        return record(resolver.vendor(vendorId), request.amount(), request.balanceType());
    }

    @Override
    public VendorBalanceResponse settle(Long vendorId, VendorSettlementRequest request) {
        return record(resolver.vendor(vendorId), request.amount(), BalanceType.SETTLEMENT);
    }

    private VendorBalanceResponse record(Vendor vendor, BigDecimal amount, BalanceType type) {
        VendorBalance saved = vendorBalanceRepository.save(VendorBalance.builder()
                .vendor(vendor)
                .amount(amount)
                .balanceType(type)
                .build());

        log.info("Posted {} of {} against vendor '{}'", type, amount, vendor.getName());
        return vendorBalanceMapper.toResponse(saved);
    }
}
