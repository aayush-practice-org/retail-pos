package io.aygh.identity.dto.response;

/**
 * A child link under a sidebar item. It carries its own menu key because it is
 * filtered on its own: a manager and an accountant can both see "Customers" and
 * be offered a different set of pages beneath it.
 */
public record SidebarSubItemResponse(
        String name,
        String path,
        String menuKey
) {
}
