package io.aygh.inventory.service.command;

import io.aygh.inventory.dto.request.UnitRequest;
import io.aygh.inventory.dto.response.UnitResponse;

/** Writes to the mart's unit dictionary. Seeded units are not writable here. */
public interface UnitCommandService {

    UnitResponse create(UnitRequest request);

    UnitResponse update(Long id, UnitRequest request);

    void delete(Long id);
}
