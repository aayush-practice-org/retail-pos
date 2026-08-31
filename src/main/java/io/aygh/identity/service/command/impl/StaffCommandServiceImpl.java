package io.aygh.identity.service.command.impl;

import io.aygh.exception.BusinessException;
import io.aygh.identity.dto.request.ResetPasswordRequest;
import io.aygh.identity.dto.request.StaffCreateRequest;
import io.aygh.identity.dto.request.StaffUpdateRequest;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserStatus;
import io.aygh.identity.helper.UserResolver;
import io.aygh.identity.helper.UserValidation;
import io.aygh.identity.mapper.StaffMapper;
import io.aygh.identity.repository.UserRepository;
import io.aygh.identity.service.command.StaffCommandService;
import io.aygh.shared.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StaffCommandServiceImpl implements StaffCommandService {

    private final UserRepository userRepository;
    private final UserResolver userResolver;
    private final UserValidation userValidation;
    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public StaffResponse createStaff(StaffCreateRequest request) {
        UUID tenantId = UserHolder.getTenantId();

        userValidation.requireAssignableByAdmin(request.role());
        userValidation.requireUsernameAvailable(request.username(), null);
        userValidation.requireEmailAvailable(request.email(), null);

        User staff = staffMapper.toUser(request);
        staff.setPassword(passwordEncoder.encode(request.password()));
        staff.setStatus(request.status() == null ? UserStatus.ACTIVE : request.status());
        staff.setTenantId(tenantId);
        staff.setTenantSlug(UserHolder.getTenantSlug());

        User saved = userRepository.save(staff);
        log.info("Created {} account '{}' in tenant '{}'",
                saved.getRole(), saved.getUsername(), saved.getTenantSlug());
        return staffMapper.toResponse(saved);
    }

    @Override
    public StaffResponse updateStaff(UUID id, StaffUpdateRequest request) {
        User staff = requireOwnStaff(id);

        userValidation.requireAssignableByAdmin(request.role());
        userValidation.requireUsernameAvailable(request.username(), id);
        userValidation.requireEmailAvailable(request.email(), id);

        staffMapper.applyUpdate(request, staff);

        User saved = userRepository.save(staff);
        log.info("Updated staff account '{}' in tenant '{}'", saved.getUsername(), saved.getTenantSlug());
        return staffMapper.toResponse(saved);
    }

    @Override
    public void resetStaffPassword(UUID id, ResetPasswordRequest request) {
        User staff = requireOwnStaff(id);
        staff.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(staff);
        log.info("Reset the password for staff account '{}'", staff.getUsername());
    }

    @Override
    public void deleteStaff(UUID id) {
        User staff = requireOwnStaff(id);
        userRepository.delete(staff);
        log.info("Retired staff account '{}' in tenant '{}'", staff.getUsername(), staff.getTenantSlug());
    }

    /**
     * The account behind {@code id}, provided it is staff of the caller's own
     * mart. An admin's own account is excluded too: editing or deleting itself
     * through the staff screens would let it change its own role.
     */
    private User requireOwnStaff(UUID id) {
        UUID tenantId = UserHolder.getTenantId();

        if (id.equals(UserHolder.getUserId())) {
            throw new BusinessException("Use the profile endpoints to change your own account");
        }

        User staff = userResolver.byIdInTenant(id, tenantId);

        if (!staff.getRole().isStaff()) {
            throw new BusinessException("Only staff accounts can be managed here");
        }
        return staff;
    }


}
