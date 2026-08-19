package io.aygh.identity.dto.response.sidebar;

import java.util.List;

public record SidebarGroupResponse(
        String title,
        List<SidebarItemResponse> items
) {
}
