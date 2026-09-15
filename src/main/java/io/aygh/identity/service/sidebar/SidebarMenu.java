package io.aygh.identity.service.sidebar;

import io.aygh.identity.entity.UserRole;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Every page the console can show, and the roles allowed to see it.
 * <p>
 * This enum is the access table and nothing else: no label a designer might
 * want to reword, no path a router might move — those live in
 * {@link SidebarCatalog}, which arranges these keys into groups. Keeping the two
 * apart means a permission change is a one-line edit here, and re-ordering the
 * menu never touches permissions.
 * <p>
 * A key is granted to the roles that can actually work the page. Where an
 * endpoint already exists the set mirrors the controller's
 * {@code @PreAuthorize} guard, because a menu entry that leads to a 403 is worse
 * than no entry at all. Menus for modules not yet built carry the role set they
 * will be guarded with.
 * <p>
 * {@link UserRole#SUPER_ADMIN} appears only on the platform keys: it belongs to
 * no tenant, so a mart page has nothing to show it.
 */
public enum SidebarMenu {

    // ── Platform: the installation owner, outside every tenant ─────────────

    PLATFORM_DASHBOARD(EnumSet.of(UserRole.SUPER_ADMIN)),
    MARTS(EnumSet.of(UserRole.SUPER_ADMIN)),

    // ── Overview ──────────────────────────────────────────────────────────

    DASHBOARD(RoleSets.EVERY_MART_ROLE),

    // ── Sales & POS ───────────────────────────────────────────────────────

    POS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.CASHIER)),
    SALES(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.SALES_EXECUTIVE,
            UserRole.ACCOUNTANT, UserRole.CASHIER, UserRole.CUSTOMER_SUPPORT)),
    CUSTOMERS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.SALES_EXECUTIVE,
            UserRole.ACCOUNTANT, UserRole.CASHIER, UserRole.CUSTOMER_SUPPORT)),

    // ── Catalogue & Inventory ─────────────────────────────────────────────

    PRODUCTS(RoleSets.CATALOGUE),
    CATEGORIES(RoleSets.CATALOGUE),
    UNITS(RoleSets.CATALOGUE),
    STOCK_LEVELS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.INVENTORY_MANAGER, UserRole.STORE_KEEPER)),
    STOCK_ADJUSTMENTS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.INVENTORY_MANAGER, UserRole.STORE_KEEPER)),

    // ── Procurement ───────────────────────────────────────────────────────

    PURCHASES(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.INVENTORY_MANAGER,
            UserRole.PURCHASE_OFFICER, UserRole.STORE_KEEPER)),
    VENDORS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.INVENTORY_MANAGER,
            UserRole.PURCHASE_OFFICER, UserRole.ACCOUNTANT)),

    // ── Reports ───────────────────────────────────────────────────────────

    REPORTS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.ACCOUNTANT)),

    // ── Administration & Settings ─────────────────────────────────────────

    STAFF(EnumSet.of(UserRole.ADMIN, UserRole.HR_MANAGER)),
    SETTINGS(EnumSet.of(UserRole.ADMIN)),
    ACCOUNT(EnumSet.allOf(UserRole.class));

    private final Set<UserRole> allowedRoles;

    SidebarMenu(Set<UserRole> allowedRoles) {
        this.allowedRoles = Collections.unmodifiableSet(EnumSet.copyOf(allowedRoles));
    }

    /**
     * The roles this menu is granted to, unmodifiable.
     */
    public Set<UserRole> allowedRoles() {
        return allowedRoles;
    }

    /**
     * Whether {@code role} may see this menu. A null role sees nothing.
     */
    public boolean hasAccess(UserRole role) {
        return role != null && allowedRoles.contains(role);
    }

    /**
     * Role sets shared by several menus. They sit in a nested class because an
     * enum constant's arguments may not read the enum's own static fields —
     * those are still uninitialised while the constants are being built.
     */
    private static final class RoleSets {

        /**
         * Everyone who works inside a mart: every role but the super admin.
         */
        private static final Set<UserRole> EVERY_MART_ROLE =
                EnumSet.complementOf(EnumSet.of(UserRole.SUPER_ADMIN));

        /**
         * The guard already on {@code ProductController}, {@code CategoryController}, {@code UnitController}.
         */
        private static final Set<UserRole> CATALOGUE =
                EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.INVENTORY_MANAGER);

        private RoleSets() {
        }
    }
}
