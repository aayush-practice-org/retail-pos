package io.aygh.identity.helper;

import io.aygh.exception.BusinessException;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class UserValidation {

    private final UserRepository userRepository;

    /**
     * Refuses a username already in use. On an update, pass the account's own id
     * so keeping its current name is not read as a clash.
     */
    public void requireUsernameAvailable(String username, UUID excludingId) {
        boolean taken = excludingId == null
                ? userRepository.existsByUsernameIgnoreCase(username)
                : userRepository.existsByUsernameIgnoreCaseAndIdNot(username, excludingId);

        if (taken) {
            log.warn("Username already taken: {}", username);
            throw new BusinessException("Username already taken: " + username);
        }
    }

    /**
     * As {@link #requireUsernameAvailable}, for the email address.
     */
    public void requireEmailAvailable(String email, UUID excludingId) {
        if (email == null || email.isBlank()) {
            return;
        }
        boolean taken = excludingId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, excludingId);

        if (taken) {
            log.warn("Email already registered: {}", email);
            throw new BusinessException("Email already registered: " + email);
        }
    }

    /**
     * Holds the tier boundary an admin may not cross.
     * <p>
     * An admin hands out staff roles and nothing else. Without this, an admin
     * could create a second admin — an account outside its own tenant that it
     * would then have no right to see — or worse, a super admin.
     */
    public void requireAssignableByAdmin(UserRole role) {
        if (role == null || !UserRole.assignableByAdmin().contains(role)) {
            throw new BusinessException(
                    "A mart admin may only create staff accounts; " + role + " is not one of them");
        }
    }

    /**
     * Guards the seeded account: losing it, or changing what it is, is how an
     * installation locks itself out for good.
     */
    public void rejectSuperAdminChange(User user, String action) {
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            throw new BusinessException("The super admin account cannot be " + action);
        }
    }
}
