package io.aygh.inventory.mapper;

import io.aygh.inventory.dto.request.UnitRequest;
import io.aygh.inventory.entity.Unit;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * {@code referenceUnit} and {@code systemDefined} are never mapped from a
 * request: the first is decided when a measurement type is seeded, the second
 * marks rows the mart is not allowed to have authored.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UnitMapper {

    io.aygh.inventory.dto.response.UnitResponse toResponse(Unit unit);

    List<io.aygh.inventory.dto.response.UnitResponse> toResponses(List<Unit> units);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceUnit", ignore = true)
    @Mapping(target = "systemDefined", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Unit toEntity(UnitRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceUnit", ignore = true)
    @Mapping(target = "systemDefined", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void applyUpdate(UnitRequest request, @MappingTarget Unit unit);
}
