package io.aygh.identity.service.query;

import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.entity.UserStatus;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Reading the staff list of one mart. Scoped to the caller's tenant; soft-deleted
 * accounts are never returned.
 */
public interface StaffQueryService {

    StaffResponse findById(UUID id);

    PagedResponse<StaffResponse> findAll(String search, UserRole role, UserStatus status, Pageable pageable);
}
