package io.aygh.identity.dto.response;

import java.util.List;

/**
 * A titled block of the sidebar. A group only reaches the caller when at least
 * one item inside it survived the role filter.
 */
public record SidebarGroupResponse(
        String title,
        List<SidebarItemResponse> items
) {
}
