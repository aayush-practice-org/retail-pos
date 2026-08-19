package io.aygh.identity.dto.response;

import java.util.List;

/**
 * One entry in the role picker: what to send back, what to show, and what the
 * role would unlock — so whoever is staffing the mart can see the consequence
 * of the choice before making it.
 */
public record RoleOptionResponse(
        String value,
        String label,
        List<String> apps,
        List<String> modules
) {
}
