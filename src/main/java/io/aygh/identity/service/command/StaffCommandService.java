package io.aygh.identity.service.command;

import io.aygh.identity.dto.request.ResetPasswordRequest;
import io.aygh.identity.dto.request.StaffCreateRequest;
import io.aygh.identity.dto.request.StaffUpdateRequest;
import io.aygh.identity.dto.response.StaffResponse;

import java.util.UUID;


public interface StaffCommandService {

    StaffResponse createStaff(StaffCreateRequest request);

    StaffResponse updateStaff(UUID id, StaffUpdateRequest request);

    void resetStaffPassword(UUID id, ResetPasswordRequest request);

    void deleteStaff(UUID id);
}
