package io.aygh.unit.mapper;

import io.aygh.unit.dto.response.UnitResponse;
import io.aygh.unit.entity.SystemUnit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SystemUnitMapper {

    @Mapping(target = "source", constant = "SYSTEM")
    @Mapping(target = "isBaseUnit", source = "baseUnit")
    UnitResponse toUnitResponse(SystemUnit systemUnit);
}
