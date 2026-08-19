package io.aygh.config;

import io.aygh.identity.entity.SidebarMenu;
import io.aygh.identity.entity.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Backs {@code @PreAuthorize("@rbac.canAccess('SALES')")} on the admin controllers.
 * Access is derived from the same {@link SidebarMenu} table that builds the sidebar,
 * so what a role can see and what it can call never drift apart.
 */
@Component("rbac")
@Slf4j
public class DynamicRbacService {

    public boolean canAccess(String sidebarItem) {
        final UserRole currentRole = getCurrentUserRole();
        if (currentRole == null) {
            return false;
        }

        try {
            SidebarMenu menu = SidebarMenu.valueOf(sidebarItem);
            boolean hasAccess = menu.hasAccess(currentRole);
            if (!hasAccess) {
                log.warn("Access Denied: Role {} does not have access to {}", currentRole, sidebarItem);
            }
            return hasAccess;
        } catch (IllegalArgumentException e) {
            log.warn("Unknown SidebarMenu module: {}", sidebarItem);
            return false;
        }
    }

    public UserRole getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities.isEmpty()) {
            return null;
        }

        String authorityName = authorities.iterator().next().getAuthority();
        try {
            if (authorityName.startsWith("ROLE_")) {
                return UserRole.valueOf(authorityName.substring(5));
            } else {
                return UserRole.valueOf(authorityName);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown role authority: {}", authorityName);
            return null;
        }
    }
}
