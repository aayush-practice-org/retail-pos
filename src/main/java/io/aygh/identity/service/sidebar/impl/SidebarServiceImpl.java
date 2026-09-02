package io.aygh.identity.service.sidebar.impl;

import io.aygh.exception.BusinessException;
import io.aygh.identity.dto.response.SidebarGroupResponse;
import io.aygh.identity.dto.response.SidebarItemResponse;
import io.aygh.identity.dto.response.SidebarSubItemResponse;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.service.sidebar.SidebarCatalog;
import io.aygh.identity.service.sidebar.SidebarService;
import io.aygh.shared.UserHolder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class SidebarServiceImpl implements SidebarService {

    private final Map<UserRole, List<SidebarGroupResponse>> byRole = new EnumMap<>(UserRole.class);


    @PostConstruct
    void buildSidebars() {
        SidebarCatalog.validate();

        for (UserRole role : UserRole.values()) {
            byRole.put(role, filterFor(role));
        }
        log.info("Sidebar prepared for {} roles from {} groups",
                byRole.size(), SidebarCatalog.master().size());
    }

    @Override
    public List<SidebarGroupResponse> currentUserSidebar() {
        UserRole role = UserHolder.getRole();
        if (role == null) {
            throw new BusinessException("No signed-in account on this request. Try logging in again.");
        }
        return sidebarFor(role);
    }

    public List<SidebarGroupResponse> sidebarFor(UserRole role) {
        if (role == null) {
            return List.of();
        }
        return byRole.getOrDefault(role, List.of());
    }

    private List<SidebarGroupResponse> filterFor(UserRole role) {
        List<SidebarGroupResponse> sidebar = new ArrayList<>();

        for (SidebarCatalog.Group group : SidebarCatalog.master()) {
            List<SidebarItemResponse> items = new ArrayList<>();

            for (SidebarCatalog.Item item : group.items()) {
                if (!item.menu().hasAccess(role)) {
                    continue;
                }

                if (item.subItems().isEmpty()) {
                    items.add(new SidebarItemResponse(
                            item.name(), item.path(), item.icon(), item.menu().name(), null));
                    continue;
                }

                List<SidebarSubItemResponse> subItems = item.subItems().stream()
                        .filter(sub -> sub.menu().hasAccess(role))
                        .map(sub -> new SidebarSubItemResponse(sub.name(), sub.path(), sub.menu().name()))
                        .toList();

                if (subItems.isEmpty()) {
                    continue;
                }

                items.add(new SidebarItemResponse(
                        item.name(),
                        // The parent's declared path may be a child this role lost —
                        // /staff is the directory, which only an admin may open — so a
                        // parent with children always lands on the first child left.
                        subItems.getFirst().path(),
                        item.icon(),
                        item.menu().name(),
                        subItems));
            }

            if (!items.isEmpty()) {
                sidebar.add(new SidebarGroupResponse(group.title(), List.copyOf(items)));
            }
        }

        return Collections.unmodifiableList(sidebar);
    }
}
