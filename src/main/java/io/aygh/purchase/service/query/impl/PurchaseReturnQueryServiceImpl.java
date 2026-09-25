package io.aygh.purchase.service.query.impl;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.purchase.dto.response.PurchaseReturnResponse;
import io.aygh.purchase.entity.PurchaseReturn;
import io.aygh.purchase.mapper.PurchaseReturnMapper;
import io.aygh.purchase.repository.PurchaseReturnRepository;
import io.aygh.purchase.service.query.PurchaseReturnQueryService;
import io.aygh.shared.response.DateRange;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseReturnQueryServiceImpl implements PurchaseReturnQueryService {

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnMapper purchaseReturnMapper;

    @Override
    public PurchaseReturnResponse findById(Long id) {
        return purchaseReturnRepository.findDetailById(id)
                .map(purchaseReturnMapper::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return", "id", id));
    }

    @Override
    public List<PurchaseReturnResponse> findByPurchase(Long purchaseId) {
        return purchaseReturnRepository.findByPurchaseIdOrderByCreatedAtAsc(purchaseId).stream()
                .map(purchaseReturnMapper::toDetail)
                .toList();
    }

    @Override
    public PagedResponse<PurchaseReturnResponse> findAll(String search, Long vendorId,
                                                         DateRange dateRange, Pageable pageable) {
        String pattern = (search == null || search.isBlank())
                ? null : "%" + search.trim().toLowerCase() + "%";

        Page<PurchaseReturn> page = purchaseReturnRepository.search(
                pattern, vendorId, DateRange.getStartDate(dateRange), DateRange.getEndDate(dateRange), pageable);

        return PaginationUtils.toPagedResponse(page,
                page.getContent().stream().map(purchaseReturnMapper::toSummary).toList());
    }
}
