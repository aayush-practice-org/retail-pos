package io.aygh.unit.service.command;

import io.aygh.unit.dto.request.CreateCustomUnitRequest;
import io.aygh.unit.dto.response.UnitResponse;

import java.util.UUID;

public interface CustomUnitCommandService {

    UnitResponse createCustomUnit(CreateCustomUnitRequest request);

    UnitResponse updateCustomUnit(UUID id, CreateCustomUnitRequest request);

    void deleteCustomUnit(UUID id);
}
