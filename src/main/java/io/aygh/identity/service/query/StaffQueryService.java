package io.aygh.identity.service.query;

import io.aygh.identity.dto.response.RoleOptionResponse;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.UserRole;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StaffQueryService {

    PagedResponse<StaffResponse> findAll(String search, UserRole role, Boolean isActive, Pageable pageable);

    StaffResponse findById(UUID id);

    /**
     * The roles the signed-in user is allowed to hand out — drives the role picker.
     */
    List<RoleOptionResponse> assignableRoles();
}
