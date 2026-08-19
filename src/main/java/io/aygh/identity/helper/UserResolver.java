package io.aygh.identity.helper;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.exception.UserNotFoundException;
import io.aygh.identity.entity.User;
import io.aygh.identity.repository.UserRepository;
import io.aygh.security.context.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserResolver {

    private final UserRepository userRepository;

    public User resolve(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found with id: " + id));
    }

    /**
     * The account behind the token on this request.
     */
    public User resolveCurrent() {
        String username = UserHolder.getCurrentUsername();

        if (username == null) {
            throw new UserNotFoundException("No authenticated user on this request. Try logging in again.");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found. Try logging in again."));
    }
}
