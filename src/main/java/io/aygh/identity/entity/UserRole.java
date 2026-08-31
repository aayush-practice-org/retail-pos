package io.aygh.identity.entity;

import java.util.Set;

/**
 * What an account is, and by extension what it may do.
 * <p>
 * The set is a hierarchy of three tiers rather than a flat list, and the tier is
 * what the service layer enforces:
 * <ul>
 *   <li>{@link #SUPER_ADMIN} owns the installation. It belongs to no tenant, and
 *       the only accounts it creates are admins.</li>
 *   <li>{@link #ADMIN} owns one mart and the schema behind it. The only accounts
 *       it creates are staff, inside that mart.</li>
 *   <li>Everything below is staff: it works inside one mart and creates nobody.</li>
 * </ul>
 * Keeping the tier here rather than in a table means an account's reach can
 * always be explained by naming its role, and that adding a role is a compile
 * step rather than a data migration.
 */
public enum UserRole {

    /**
     * The installation owner, seeded at first start. Exactly one exists.
     */
    SUPER_ADMIN,

    /**
     * The mart owner: runs one tenant end to end, staff accounts included.
     */
    ADMIN,

    /**
     * Runs the floor day to day: the till, sales, customers, staff and stock. Not the books.
     */
    STORE_MANAGER,

    /**
     * Works the till and nothing else — no reach into the sales history behind it.
     */
    CASHIER,

    /**
     * Takes orders and quotes away from the till, and keeps the customer record current.
     */
    SALES_EXECUTIVE,

    /**
     * Owns stock and procurement end to end: catalogue, suppliers, purchase orders, write-offs.
     */
    INVENTORY_MANAGER,

    /**
     * Minds the shelves and the back room: receives goods and adjusts stock.
     */
    STORE_KEEPER,

    /**
     * Raises purchase orders against suppliers.
     */
    PURCHASE_OFFICER,

    /**
     * Keeps the books: ledgers, expenses, taxes, reconciliation and financial reports.
     */
    ACCOUNTANT,

    /**
     * Staff records, attendance, shifts, leave and payroll input.
     */
    HR_MANAGER,

    /**
     * The customer desk: loyalty, credit, complaints and returns follow-up.
     */
    CUSTOMER_SUPPORT;

    private static final Set<UserRole> PLATFORM_ROLES = Set.of(SUPER_ADMIN, ADMIN);

    /**
     * The Spring Security authority for the role, e.g. {@code ROLE_STORE_MANAGER}.
     */
    public String authority() {
        return "ROLE_" + name();
    }

    /**
     * Whether the role works inside a tenant schema rather than owning or spanning tenants.
     */
    public boolean isStaff() {
        return !PLATFORM_ROLES.contains(this);
    }

    /**
     * Whether an account holding this role must be attached to a tenant. Only the
     * super admin is not — it stands outside them all.
     */
    public boolean requiresTenant() {
        return this != SUPER_ADMIN;
    }

    /**
     * The roles an admin may hand out to its own staff: everything below itself.
     */
    public static Set<UserRole> assignableByAdmin() {
        return Set.of(STORE_MANAGER, CASHIER, SALES_EXECUTIVE, INVENTORY_MANAGER,
                STORE_KEEPER, PURCHASE_OFFICER, ACCOUNTANT, HR_MANAGER, CUSTOMER_SUPPORT);
    }
}
