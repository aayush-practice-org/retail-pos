package io.aygh.identity.mapper;

import io.aygh.identity.dto.response.RoleOptionResponse;
import io.aygh.identity.dto.response.SelfResponse;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.entity.SidebarApp;
import io.aygh.identity.entity.SidebarMenu;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class UserMapper {

    public StaffResponse toStaffResponse(User user) {
        return new StaffResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public SelfResponse toSelfResponse(User user) {
        return new SelfResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name()
        );
    }

    /**
     * The apps and modules come straight off {@link SidebarApp} / {@link SidebarMenu},
     * so a role option always describes exactly what the role will really see.
     */
    public RoleOptionResponse toRoleOption(UserRole role) {
        List<String> apps = Arrays.stream(SidebarApp.values())
                .filter(app -> app.isOpenTo(role))
                .map(SidebarApp::getTitle)
                .toList();

        List<String> modules = Arrays.stream(SidebarMenu.values())
                .filter(menu -> menu.hasAccess(role))
                .map(SidebarMenu::getTitle)
                .toList();

        return new RoleOptionResponse(role.name(), toLabel(role), apps, modules);
    }

    /** {@code STOREKEEPER} reads as "Storekeeper" once it reaches a dropdown. */
    private String toLabel(UserRole role) {
        String name = role.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
