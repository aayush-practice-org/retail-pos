package io.aygh.identity.service.sidebar;

import io.aygh.identity.entity.UserRole;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The master sidebar: every group, item and sub-item the console can render, in
 * the order it renders them, before anyone's role is considered.
 * <p>
 * Each entry points at a {@link SidebarMenu} rather than naming one in a string,
 * so a menu key that does not exist is a compile error instead of an entry that
 * quietly disappears at runtime. The structure is a static constant — the same
 * immutable list for the life of the process, from which
 * {@code SidebarServiceImpl} derives one filtered copy per role.
 * <p>
 * Paths are the frontend's routes, not this API's endpoints: the console decides
 * what to call when the page opens.
 */
public final class SidebarCatalog {

    /**
     * A titled block of the sidebar. Dropped entirely when nothing inside survives filtering.
     */
    public record Group(String title, List<Item> items) {
    }

    /**
     * A top-level entry. {@code menu} gates the entry as a whole; when
     * {@code subItems} is non-empty it is the union gate over them, and each
     * sub-item is then checked on its own.
     */
    public record Item(String name, String path, String icon, SidebarMenu menu, List<SubItem> subItems) {

        /**
         * A leaf entry — a link with no children.
         */
        public Item(String name, String path, String icon, SidebarMenu menu) {
            this(name, path, icon, menu, List.of());
        }
    }

    /**
     * A child link under an {@link Item}, gated by its own menu.
     */
    public record SubItem(String name, String path, SidebarMenu menu) {
    }

    private static final List<Group> MASTER = List.of(

            new Group("Platform", List.of(
                    new Item("Dashboard", "/platform/dashboard", "LayoutDashboard", SidebarMenu.PLATFORM_DASHBOARD),
                    new Item("Marts", "/platform/marts", "Store", SidebarMenu.MARTS)
            )),

            new Group("Overview", List.of(
                    new Item("Dashboard", "/dashboard", "LayoutDashboard", SidebarMenu.DASHBOARD)
            )),

            new Group("Sales & POS", List.of(
                    new Item("Point of Sale", "/pos", "ScanBarcode", SidebarMenu.POS),
                    new Item("Sales History", "/sales", "ReceiptText", SidebarMenu.SALES),
                    new Item("Customers", "/customers", "Users", SidebarMenu.CUSTOMERS)
            )),

            new Group("Catalogue & Inventory", List.of(
                    new Item("Products", "/catalogue/products", "Package", SidebarMenu.PRODUCTS),
                    new Item("Categories", "/catalogue/categories", "FolderTree", SidebarMenu.CATEGORIES),
                    new Item("Units", "/catalogue/units", "Scale", SidebarMenu.UNITS),
                    new Item("Stock Levels", "/inventory/stock", "Boxes", SidebarMenu.STOCK_LEVELS),
                    new Item("Stock Adjustments", "/inventory/stock/adjustments", "SlidersHorizontal", SidebarMenu.STOCK_ADJUSTMENTS)
            )),

            new Group("Procurement", List.of(
                    new Item("Purchases", "/purchases", "ShoppingCart", SidebarMenu.PURCHASES),
                    new Item("Vendors", "/vendors", "Truck", SidebarMenu.VENDORS)
            )),

            new Group("Reports", List.of(
                    new Item("Sales Reports", "/reports/sales", "ChartColumn", SidebarMenu.REPORTS)
            )),

            new Group("Administration", List.of(
                    new Item("Staff", "/staff", "UserCog", SidebarMenu.STAFF),
                    new Item("Mart Settings", "/settings", "Settings", SidebarMenu.SETTINGS),
                    new Item("My Account", "/account", "CircleUser", SidebarMenu.ACCOUNT)
            ))
    );

    private SidebarCatalog() {
    }

    /**
     * The unfiltered structure, in render order.
     */
    public static List<Group> master() {
        return MASTER;
    }

    /**
     * Checks the structure against its own rules and throws if it breaks one.
     * Called once at startup, so a menu wired wrong fails the boot rather than
     * showing the wrong people the wrong pages:
     * <ul>
     *   <li>a group holds at least one item, and an item's sub-items are all distinct;</li>
     *   <li>a sub-item's roles are a subset of its parent's, since the parent gates it —
     *       otherwise the sub-item is unreachable for the roles only it allows.</li>
     * </ul>
     */
    public static void validate() {
        Set<String> seen = new HashSet<>();

        for (Group group : MASTER) {
            if (group.items().isEmpty()) {
                throw new IllegalStateException("Sidebar group '" + group.title() + "' has no items");
            }
            for (Item item : group.items()) {
                if (!seen.add(group.title() + " > " + item.name())) {
                    throw new IllegalStateException("Duplicate sidebar item '" + item.name()
                            + "' in group '" + group.title() + "'");
                }
                Set<UserRole> parentRoles;
                parentRoles = item.menu().allowedRoles();
                for (SubItem sub : item.subItems()) {
                    if (!parentRoles.containsAll(sub.menu().allowedRoles())) {
                        throw new IllegalStateException("Sidebar sub-item '" + sub.name() + "' under '"
                                + item.name() + "' allows roles its parent " + item.menu()
                                + " does not: " + minus(sub.menu().allowedRoles(), parentRoles));
                    }
                }
            }
        }
    }

    private static Set<UserRole> minus(Set<UserRole> roles, Set<UserRole> allowed) {
        Set<UserRole> extra = new HashSet<>(roles);
        extra.removeAll(allowed);
        return extra;
    }
}
