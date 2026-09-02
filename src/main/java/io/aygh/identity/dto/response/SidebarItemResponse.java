package io.aygh.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * One top-level sidebar entry. {@code subItems} is omitted from the JSON when
 * the entry is a plain link, so the console can treat "has children" as
 * "the field is there".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SidebarItemResponse(
        String name,
        String path,
        String icon,
        String menuKey,
        List<SidebarSubItemResponse> subItems
) {
}
