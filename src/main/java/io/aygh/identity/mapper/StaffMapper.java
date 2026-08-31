package io.aygh.identity.mapper;

import io.aygh.identity.dto.request.StaffCreateRequest;
import io.aygh.identity.dto.request.StaffUpdateRequest;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Between a staff account and its DTOs.
 * <p>
 * Three things are never mapped in either direction, and each for its own
 * reason: the password is hashed by the service on the way in and never leaves
 * on the way out; the tenant fields come from the caller's token, so a request
 * body cannot move someone into another mart; and the audit timestamps belong to
 * the persistence layer.
 */
@Mapper(componentModel = "spring")
public interface StaffMapper {

    StaffResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "tenantSlug", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    User toUser(StaffCreateRequest request);

    /**
     * Applies an update onto the account already loaded, so the row keeps its id,
     * its hash and its history instead of being replaced by a fresh one.
     */
    @Mapping(target = "id", ignore = true)
    // Read-only on the entity: it is derived from the role, but MapStruct sees a
    // collection getter and would otherwise try to fill it.
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "tenantSlug", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void applyUpdate(StaffUpdateRequest request, @MappingTarget User user);
}
