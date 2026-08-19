package io.aygh.identity.service.command;

import io.aygh.identity.dto.request.StaffCreateRequest;
import io.aygh.identity.dto.request.StaffUpdateRequest;
import io.aygh.identity.dto.response.StaffResponse;

import java.util.UUID;

public interface StaffCommandService {

    StaffResponse create(StaffCreateRequest request);

    StaffResponse update(UUID id, StaffUpdateRequest request);

    StaffResponse setActive(UUID id, boolean active);

    void delete(UUID id);
}
