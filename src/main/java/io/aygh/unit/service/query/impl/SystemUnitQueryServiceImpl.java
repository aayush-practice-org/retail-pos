package io.aygh.unit.service.query.impl;

import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import io.aygh.unit.dto.response.UnitResponse;
import io.aygh.unit.entity.SystemUnit;
import io.aygh.unit.mapper.SystemUnitMapper;
import io.aygh.unit.repository.SystemUnitRepository;
import io.aygh.unit.resolver.UnitResolver;
import io.aygh.unit.service.query.SystemUnitQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/*
 *  System units are seeded by migration and never mutated at runtime,
 *  so they are safe to cache for the lifetime of the application.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SystemUnitQueryServiceImpl implements SystemUnitQueryService {

    private final SystemUnitRepository systemUnitRepository;
    private final SystemUnitMapper systemUnitMapper;
    private final UnitResolver unitResolver;

    @Override
    @Cacheable(cacheNames = "system-units", key = "{'page', #pageable}")
    public PagedResponse<UnitResponse> getAllSystemUnits(Pageable pageable) {
        Page<UnitResponse> units = systemUnitRepository.findAll(pageable)
                .map(systemUnitMapper::toUnitResponse);
        return PaginationUtils.toPagedResponse(units);
    }

    @Override
    @Cacheable(cacheNames = "system-units", key = "'all'")
    public List<UnitResponse> findAll() {
        return systemUnitRepository.findAll(Sort.by("measurementType").ascending().and(Sort.by("conversionFactor").ascending()))
                .stream()
                .map(systemUnitMapper::toUnitResponse)
                .toList();
    }

    @Override
    public SystemUnit getById(UUID id) {
        return unitResolver.resolveSystemUnit(id);
    }
}
