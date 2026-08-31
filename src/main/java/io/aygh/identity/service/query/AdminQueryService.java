package io.aygh.identity.service.query;

import io.aygh.identity.dto.response.AdminResponse;
import io.aygh.identity.entity.ProvisioningStatus;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Reading marts. The super admin's view of every tenant in the installation. */
public interface AdminQueryService {

    AdminResponse findById(UUID id);

    PagedResponse<AdminResponse> findAll(String search, ProvisioningStatus provisioningStatus, Pageable pageable);
}
