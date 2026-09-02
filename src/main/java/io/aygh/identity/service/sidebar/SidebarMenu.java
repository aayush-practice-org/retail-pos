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
    MART_PROVISIONING(EnumSet.of(UserRole.SUPER_ADMIN)),

    // ── Overview ──────────────────────────────────────────────────────────

    /**
     * The one page every account inside a mart may open.
     */
    DASHBOARD(RoleSets.EVERY_MART_ROLE),

    // ── Sales ─────────────────────────────────────────────────────────────

    POS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.CASHIER)),
    SALES(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.SALES_EXECUTIVE,
            UserRole.ACCOUNTANT, UserRole.CUSTOMER_SUPPORT)),
    ORDERS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.SALES_EXECUTIVE)),
    INVOICES(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.SALES_EXECUTIVE,
            UserRole.ACCOUNTANT)),
    RETURNS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.SALES_EXECUTIVE,
            UserRole.CUSTOMER_SUPPORT)),

    CUSTOMERS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.SALES_EXECUTIVE,
            UserRole.CUSTOMER_SUPPORT, UserRole.ACCOUNTANT)),
    CUSTOMER_DIRECTORY(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.SALES_EXECUTIVE, UserRole.CUSTOMER_SUPPORT)),
    LOYALTY(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.CUSTOMER_SUPPORT)),
    CUSTOMER_CREDIT(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.ACCOUNTANT, UserRole.CUSTOMER_SUPPORT)),

    // ── Catalogue — mirrors the guards on the /inventory/* controllers ─────

    PRODUCTS(RoleSets.CATALOGUE),
    CATEGORIES(RoleSets.CATALOGUE),
    UNITS(RoleSets.CATALOGUE),

    // ── Inventory ─────────────────────────────────────────────────────────

    INVENTORY(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.INVENTORY_MANAGER,
            UserRole.STORE_KEEPER)),
    STOCK_LEVELS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.INVENTORY_MANAGER, UserRole.STORE_KEEPER)),
    STOCK_ADJUSTMENTS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.INVENTORY_MANAGER, UserRole.STORE_KEEPER)),
    STOCK_WRITE_OFFS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.INVENTORY_MANAGER)),

    // ── Purchasing ────────────────────────────────────────────────────────

    PURCHASE(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.INVENTORY_MANAGER,
            UserRole.PURCHASE_OFFICER, UserRole.STORE_KEEPER)),
    PURCHASE_ORDERS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.INVENTORY_MANAGER, UserRole.PURCHASE_OFFICER)),
    GOODS_RECEIPTS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.INVENTORY_MANAGER, UserRole.PURCHASE_OFFICER, UserRole.STORE_KEEPER)),
    VENDOR(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.INVENTORY_MANAGER,
            UserRole.PURCHASE_OFFICER, UserRole.ACCOUNTANT)),

    // ── Finance ───────────────────────────────────────────────────────────

    ACCOUNTING(EnumSet.of(UserRole.ADMIN, UserRole.ACCOUNTANT)),
    LEDGER(EnumSet.of(UserRole.ADMIN, UserRole.ACCOUNTANT)),
    EXPENSES(EnumSet.of(UserRole.ADMIN, UserRole.ACCOUNTANT)),
    TAXES(EnumSet.of(UserRole.ADMIN, UserRole.ACCOUNTANT)),

    REPORTS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.INVENTORY_MANAGER,
            UserRole.ACCOUNTANT)),
    SALES_REPORTS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER, UserRole.ACCOUNTANT)),
    INVENTORY_REPORTS(EnumSet.of(UserRole.ADMIN, UserRole.STORE_MANAGER,
            UserRole.INVENTORY_MANAGER, UserRole.ACCOUNTANT)),
    FINANCE_REPORTS(EnumSet.of(UserRole.ADMIN, UserRole.ACCOUNTANT)),

    // ── People ────────────────────────────────────────────────────────────

    /**
     * The gate on the People group as a whole; the pages inside it are narrower.
     */
    STAFF(EnumSet.of(UserRole.ADMIN, UserRole.HR_MANAGER, UserRole.STORE_MANAGER,
            UserRole.ACCOUNTANT)),
    /**
     * {@code /admin/staff} is admin-only, so the directory is too.
     */
    STAFF_DIRECTORY(EnumSet.of(UserRole.ADMIN)),
    ATTENDANCE(EnumSet.of(UserRole.ADMIN, UserRole.HR_MANAGER, UserRole.STORE_MANAGER)),
    PAYROLL(EnumSet.of(UserRole.ADMIN, UserRole.HR_MANAGER, UserRole.ACCOUNTANT)),

    // ── Settings ──────────────────────────────────────────────────────────

    SETTINGS(EnumSet.of(UserRole.ADMIN)),
    /**
     * Own profile and password: every signed-in account, super admin included.
     */
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
