package io.aygh.identity.service.command.impl;

import io.aygh.identity.dto.request.StaffCreateRequest;
import io.aygh.identity.dto.request.StaffUpdateRequest;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.helper.UserResolver;
import io.aygh.identity.helper.UserValidation;
import io.aygh.identity.mapper.UserMapper;
import io.aygh.identity.repository.UserRepository;
import io.aygh.identity.service.command.StaffCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
class StaffCommandServiceImpl implements StaffCommandService {

    private final UserRepository userRepository;
    private final UserResolver userResolver;
    private final UserValidation userValidation;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffResponse create(StaffCreateRequest request) {
        userValidation.validateCanAssignRole(request.role());
        userValidation.validateUniqueUsername(request.username(), null);
        userValidation.validateUniqueEmail(request.email(), null);

        User staff = User.builder()
                .username(request.username())
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .isActive(true)
                .build();

        User saved = userRepository.save(staff);
        log.info("Created staff id={} username={} role={}", saved.getId(), saved.getUsername(), saved.getRole());

        return userMapper.toStaffResponse(saved);
    }

    /**
     * A partial update: only the fields the caller actually sent are touched.
     */
    @Override
    @Transactional
    public StaffResponse update(UUID id, StaffUpdateRequest request) {
        User staff = userResolver.resolve(id);

        userValidation.validateCanManage(staff);

        applyUsername(staff, request.username());
        applyEmail(staff, request.email());
        applyRole(staff, request.role());
        applyActive(staff, request.isActive());

        if (request.fullName() != null) {
            staff.setFullName(request.fullName());
        }

        if (request.phone() != null) {
            staff.setPhone(request.phone());
        }

        if (request.password() != null && !request.password().isBlank()) {
            staff.setPassword(passwordEncoder.encode(request.password()));
            log.info("Password reset for staff id={}", id);
        }

        log.info("Updated staff id={}", id);

        return userMapper.toStaffResponse(staff);
    }

    @Override
    @Transactional
    public StaffResponse setActive(UUID id, boolean active) {
        User staff = userResolver.resolve(id);

        userValidation.validateCanManage(staff);
        applyActive(staff, active);

        log.info("Staff id={} is now {}", id, active ? "active" : "deactivated");

        return userMapper.toStaffResponse(staff);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        User staff = userResolver.resolve(id);

        userValidation.validateCanManage(staff);
        userValidation.validateNotSelf(staff, "delete");
        userValidation.validateNotLastAdmin(staff);

        // Soft delete — the row stays, and the username and email free up for reuse
        userRepository.delete(staff);
        log.info("Deleted staff id={} username={}", id, staff.getUsername());
    }

    private void applyUsername(User staff, String username) {
        if (username == null || username.equals(staff.getUsername())) {
            return;
        }

        userValidation.validateUniqueUsername(username, staff.getId());
        staff.setUsername(username);
    }

    private void applyEmail(User staff, String email) {
        if (email == null || email.equals(staff.getEmail())) {
            return;
        }

        userValidation.validateUniqueEmail(email, staff.getId());
        staff.setEmail(email);
    }

    private void applyRole(User staff, UserRole role) {
        if (role == null || role == staff.getRole()) {
            return;
        }

        userValidation.validateCanAssignRole(role);
        // Demoting the last owner would leave the mart with nobody who can let people back in
        userValidation.validateNotLastAdmin(staff);

        staff.setRole(role);
    }

    private void applyActive(User staff, Boolean active) {
        if (active == null || active == staff.isActive()) {
            return;
        }

        if (!active) {
            userValidation.validateNotSelf(staff, "deactivate");
            userValidation.validateNotLastAdmin(staff);
        }

        staff.setActive(active);
    }
}
