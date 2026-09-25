package io.aygh.sales.service.query.impl;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.sales.dto.response.SalesReturnResponse;
import io.aygh.sales.entity.SalesReturn;
import io.aygh.sales.mapper.SalesReturnMapper;
import io.aygh.sales.repository.SalesReturnRepository;
import io.aygh.sales.service.query.SalesReturnQueryService;
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
public class SalesReturnQueryServiceImpl implements SalesReturnQueryService {

    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnMapper salesReturnMapper;

    @Override
    public SalesReturnResponse findById(Long id) {
        return salesReturnRepository.findDetailById(id)
                .map(salesReturnMapper::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Sales return", "id", id));
    }

    @Override
    public List<SalesReturnResponse> findBySale(Long saleId) {
        return salesReturnRepository.findBySaleIdOrderByReturnedAtAsc(saleId).stream()
                .map(salesReturnMapper::toDetail)
                .toList();
    }

    @Override
    public PagedResponse<SalesReturnResponse> findAll(String search, DateRange dateRange, Pageable pageable) {
        String pattern = (search == null || search.isBlank())
                ? null : "%" + search.trim().toLowerCase() + "%";

        Page<SalesReturn> page = salesReturnRepository.search(
                pattern, DateRange.getStart(dateRange), DateRange.getEnd(dateRange), pageable);

        return PaginationUtils.toPagedResponse(page,
                page.getContent().stream().map(salesReturnMapper::toSummary).toList());
    }
}
