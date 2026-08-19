package io.aygh.unit.service.query.impl;

import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import io.aygh.unit.dto.response.UnitResponse;
import io.aygh.unit.mapper.CustomUnitMapper;
import io.aygh.unit.repository.CustomUnitRepository;
import io.aygh.unit.resolver.UnitResolver;
import io.aygh.unit.service.query.CustomUnitQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CustomUnitQueryServiceImpl implements CustomUnitQueryService {

    private final CustomUnitRepository customUnitRepository;
    private final CustomUnitMapper customUnitMapper;
    private final UnitResolver unitResolver;

    @Override
    public PagedResponse<UnitResponse> getAllCustomUnits(Pageable pageable) {
        Page<UnitResponse> units = customUnitRepository.findAll(pageable)
                .map(customUnitMapper::toUnitResponse);
        return PaginationUtils.toPagedResponse(units);
    }

    @Override
    public List<UnitResponse> findAll() {
        return customUnitRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(customUnitMapper::toUnitResponse)
                .toList();
    }

    @Override
    public UnitResponse getCustomUnit(UUID id) {
        return customUnitMapper.toUnitResponse(unitResolver.resolveCustomUnit(id));
    }
}
