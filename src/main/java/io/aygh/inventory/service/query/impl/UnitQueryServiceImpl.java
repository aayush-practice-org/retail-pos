package io.aygh.inventory.service.query.impl;

import io.aygh.inventory.dto.response.UnitResponse;
import io.aygh.inventory.entity.MeasurementType;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.helper.InventoryResolver;
import io.aygh.inventory.mapper.UnitMapper;
import io.aygh.inventory.repository.UnitRepository;
import io.aygh.inventory.service.query.UnitQueryService;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitQueryServiceImpl implements UnitQueryService {

    private final UnitRepository unitRepository;
    private final InventoryResolver resolver;
    private final UnitMapper unitMapper;

    @Override
    public UnitResponse findById(Long id) {
        resolver.requireTenantContext();
        return unitMapper.toResponse(resolver.unit(id));
    }

    @Override
    public PagedResponse<UnitResponse> findAll(String search, MeasurementType measurementType, Pageable pageable) {
        resolver.requireTenantContext();

        List<Specification<Unit>> filters = Stream.of(matches(search), ofType(measurementType))
                .filter(Objects::nonNull)
                .toList();

        Page<Unit> page = unitRepository.findAll(Specification.allOf(filters), pageable);
        return PaginationUtils.toPagedResponse(page, page.map(unitMapper::toResponse).getContent());
    }

    @Override
    public List<UnitResponse> findAllForSelection(MeasurementType measurementType) {
        resolver.requireTenantContext();

        List<Unit> units = measurementType == null
                ? unitRepository.findAll(Sort.by("name"))
                : unitRepository.findByMeasurementType(measurementType);

        return unitMapper.toResponses(units);
    }

    // ── Filters ───────────────────────────────────────────────────────────

    private static Specification<Unit> matches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("symbol")), pattern));
    }

    private static Specification<Unit> ofType(MeasurementType measurementType) {
        return measurementType == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("measurementType"), measurementType);
    }
}
