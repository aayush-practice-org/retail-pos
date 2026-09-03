package io.aygh.purchase.service.query.impl;

import io.aygh.purchase.dto.response.PurchaseDetailResponse;
import io.aygh.purchase.dto.response.PurchaseSummaryResponse;
import io.aygh.purchase.entity.Purchase;
import io.aygh.purchase.helper.PurchaseResolver;
import io.aygh.purchase.mapper.PurchaseMapper;
import io.aygh.purchase.repository.PurchaseRepository;
import io.aygh.purchase.service.query.PurchaseQueryService;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseQueryServiceImpl implements PurchaseQueryService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseResolver resolver;
    private final PurchaseMapper purchaseMapper;

    @Override
    public PurchaseDetailResponse findById(Long id) {
        return purchaseMapper.toDetail(resolver.purchase(id));
    }

    @Override
    public PagedResponse<PurchaseSummaryResponse> findAll(String search, Long vendorId,
                                                          LocalDate from, LocalDate to,
                                                          Pageable pageable) {
        String pattern = (search == null || search.isBlank())
                ? null : "%" + search.trim().toLowerCase() + "%";

        Page<Purchase> page = purchaseRepository.search(pattern, vendorId, from, to, pageable);

        List<PurchaseSummaryResponse> content = page.getContent().stream()
                .map(purchase -> purchaseMapper.toSummary(purchase, purchaseRepository.countItems(purchase.getId())))
                .toList();

        return PaginationUtils.toPagedResponse(page, content);
    }
}
