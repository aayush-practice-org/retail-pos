package io.aygh.identity.service.command;

import io.aygh.identity.dto.request.AdminCreateRequest;
import io.aygh.identity.dto.request.AdminUpdateRequest;
import io.aygh.identity.dto.request.ResetPasswordRequest;
import io.aygh.identity.dto.response.AdminResponse;

import java.util.List;
import java.util.UUID;


public interface AdminCommandService {

    AdminResponse createAdmin(AdminCreateRequest request);

    AdminResponse updateAdmin(UUID id, AdminUpdateRequest request);

    AdminResponse provisionAdmin(UUID id);

    void resetAdminPassword(UUID id, ResetPasswordRequest request);

    void deleteAdmin(UUID id);

    List<String> migrateAllTenants();
}
