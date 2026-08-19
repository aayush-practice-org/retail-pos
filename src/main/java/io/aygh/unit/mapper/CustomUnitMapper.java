package io.aygh.unit.mapper;

import io.aygh.unit.dto.request.CreateCustomUnitRequest;
import io.aygh.unit.dto.response.UnitResponse;
import io.aygh.unit.entity.CustomUnit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomUnitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    CustomUnit toEntity(CreateCustomUnitRequest request);

    @Mapping(target = "source", constant = "CUSTOM")
    @Mapping(target = "isBaseUnit", constant = "false")
    UnitResponse toUnitResponse(CustomUnit customUnit);
}
