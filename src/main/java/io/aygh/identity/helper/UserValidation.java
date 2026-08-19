package io.aygh.identity.helper;

import io.aygh.config.DynamicRbacService;
import io.aygh.exception.BusinessException;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.repository.UserRepository;
import io.aygh.security.context.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserValidation {

    private final UserRepository userRepository;
    private final DynamicRbacService rbacService;

    public void validateUniqueUsername(String username, UUID excludeId) {
        if (userRepository.existsByUsername(username, excludeId)) {
            throw new BusinessException("Username already taken: " + username);
        }
    }

    public void validateUniqueEmail(String email, UUID excludeId) {
        if (userRepository.existsByEmail(email, excludeId)) {
            throw new BusinessException("Email already registered: " + email);
        }
    }

    /**
     * A manager may staff the floor but may not mint admins or fellow managers —
     * only the owner hands out the senior roles.
     */
    public List<UserRole> assignableRoles() {
        UserRole currentRole = rbacService.getCurrentUserRole();

        if (currentRole == UserRole.ADMIN) {
            return List.of(UserRole.values());
        }

        return Arrays.stream(UserRole.values())
                .filter(role -> role != UserRole.ADMIN && role != UserRole.MANAGER)
                .toList();
    }

    public void validateCanAssignRole(UserRole targetRole) {
        if (!assignableRoles().contains(targetRole)) {
            throw new BusinessException("Only an ADMIN can assign the %s role".formatted(targetRole));
        }
    }

    /**
     * Managers cannot edit or remove an admin's account either.
     */
    public void validateCanManage(User target) {
        UserRole currentRole = rbacService.getCurrentUserRole();

        if (currentRole == UserRole.ADMIN) {
            return;
        }

        if (target.getRole() == UserRole.ADMIN || target.getRole() == UserRole.MANAGER) {
            throw new BusinessException("Only an ADMIN can manage %s accounts".formatted(target.getRole()));
        }
    }

    /**
     * Nobody locks themselves out — deleting or deactivating your own account is
     * a mistake, not an intention.
     */
    public void validateNotSelf(User target, String action) {
        UUID currentUserId = UserHolder.getCurrentUserId();

        if (currentUserId != null && currentUserId.equals(target.getId())) {
            throw new BusinessException("You cannot %s your own account".formatted(action));
        }
    }

    /**
     * The mart must always keep at least one active owner who can get back in.
     */
    public void validateNotLastAdmin(User target) {
        if (target.getRole() != UserRole.ADMIN) {
            return;
        }

        if (userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new BusinessException("Cannot remove or demote the only ADMIN account");
        }
    }
}
