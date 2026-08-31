package io.aygh.identity.helper;

import io.aygh.exception.UserNotFoundException;
import io.aygh.identity.entity.User;
import io.aygh.identity.repository.UserRepository;
import io.aygh.shared.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
@RequiredArgsConstructor
public class UserResolver {

    private final UserRepository userRepository;

    public User byId(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user found with id " + id));
    }

    public User byIdInTenant(UUID id, UUID tenantId) {
        return userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UserNotFoundException("No user found with id " + id));
    }

    /**
     * The account behind the current request.
     */
    public User current() {
        UUID id = UserHolder.getUserId();
        if (id == null) {
            throw new UserNotFoundException("No authenticated account on this request");
        }
        return byId(id);
    }

    /**
     * A sign-in identifier: a username or an email address, whichever was typed.
     */
    public User byIdentifier(String identifier) {
        return userRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new UserNotFoundException("No user found matching " + identifier));
    }
}
