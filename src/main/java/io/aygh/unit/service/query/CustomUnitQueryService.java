package io.aygh.unit.service.query;

import io.aygh.shared.response.PagedResponse;
import io.aygh.unit.dto.response.UnitResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CustomUnitQueryService {

    PagedResponse<UnitResponse> getAllCustomUnits(Pageable pageable);

    List<UnitResponse> findAll();

    UnitResponse getCustomUnit(UUID id);
}
