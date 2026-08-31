package io.aygh.identity.mapper;

import io.aygh.identity.entity.Admin;
import io.aygh.identity.dto.response.AdminResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * A mart, flattened for the super admin's screens: the company fields sit on
 * {@link Admin}, the account fields on the {@code User} it shares its id with,
 * and the two arrive as one object.
 */
@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "mobileNumber", source = "user.mobileNumber")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "lastLoginAt", source = "user.lastLoginAt")
    AdminResponse toResponse(Admin admin);
}
