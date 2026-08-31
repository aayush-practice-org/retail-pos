package io.aygh.identity.helper;

import io.aygh.exception.ResourceNotFoundException;
import io.aygh.identity.entity.Admin;
import io.aygh.identity.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Loading marts, with their owning account already fetched. */
@Component
@RequiredArgsConstructor
public class AdminResolver {

    private final AdminRepository adminRepository;

    public Admin byId(UUID id) {
        return adminRepository.findWithUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "id", id));
    }

    public Admin bySlug(String slug) {
        return adminRepository.findWithUserBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "slug", slug));
    }
}
