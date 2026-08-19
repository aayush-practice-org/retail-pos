package io.aygh.identity.dto.response.sidebar;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SidebarItemResponse(
        String name,
        String path,
        String icon,
        String menuKey,
        List<SidebarSubItemResponse> subItems
) {
}
