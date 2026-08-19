package io.aygh.unit.service.query;

import io.aygh.shared.response.PagedResponse;
import io.aygh.unit.dto.response.UnitResponse;
import io.aygh.unit.entity.SystemUnit;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SystemUnitQueryService {

    PagedResponse<UnitResponse> getAllSystemUnits(Pageable pageable);

    List<UnitResponse> findAll();

    SystemUnit getById(UUID id);
}
