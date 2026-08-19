package io.aygh.identity.entity;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * The modules a mart back-office has, and which roles may reach them.
 * <p>
 * This is the single source of truth for authorisation: the sidebar is built from
 * it, and {@code @PreAuthorize("@rbac.canAccess('...')")} on the controllers reads
 * the same table — so a role can never call something it cannot see.
 */
@Getter
public enum SidebarMenu {

    DASHBOARD("Dashboard",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.CASHIER, UserRole.STOREKEEPER,
                    UserRole.ACCOUNTANT)),

    /** The sale screen itself — ringing items up at the till. */
    POS("Point of Sale",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.CASHIER)),

    /** Taking the money: cash, card, wallet, and the day's transaction list. */
    PAYMENTS("Payments",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.CASHIER, UserRole.ACCOUNTANT)),

    SALES("Sales",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.CASHIER, UserRole.ACCOUNTANT)),

    PRODUCTS("Products",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.STOREKEEPER)),

    CATEGORIES("Categories",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.STOREKEEPER)),

    UNITS("Units",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.STOREKEEPER)),

    REPORTS("Reports",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.ACCOUNTANT)),

    STAFF("Staff",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER));

    private final String title;
    private final Set<UserRole> allowedRoles;

    SidebarMenu(String title, Set<UserRole> allowedRoles) {
        this.title = title;
        this.allowedRoles = allowedRoles;
    }

    public boolean hasAccess(UserRole role) {
        return allowedRoles.contains(role);
    }
}
