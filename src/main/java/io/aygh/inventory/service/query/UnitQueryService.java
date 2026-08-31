package io.aygh.inventory.service.query;

import io.aygh.inventory.dto.response.UnitResponse;
import io.aygh.inventory.entity.MeasurementType;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UnitQueryService {

    UnitResponse findById(Long id);

    PagedResponse<UnitResponse> findAll(String search, MeasurementType measurementType, Pageable pageable);

    /** Unpaged, for the dropdowns that pick a unit. */
    List<UnitResponse> findAllForSelection(MeasurementType measurementType);
}
