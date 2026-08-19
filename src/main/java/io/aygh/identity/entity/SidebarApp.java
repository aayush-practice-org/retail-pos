package io.aygh.identity.entity;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * The two front-ends a mart runs, and which roles land in each.
 * <p>
 * The back office is the admin console — catalogue, staff, reports. The counter is
 * the selling app the till runs on: a cashier signs in there and gets a dashboard,
 * the sale screen and payments, and nothing else.
 * <p>
 * A role can belong to both: an owner or manager walks the floor and works the till.
 */
@Getter
public enum SidebarApp {

    BACK_OFFICE("Back office",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.STOREKEEPER, UserRole.ACCOUNTANT)),

    COUNTER("Counter",
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.CASHIER));

    private final String title;
    private final Set<UserRole> allowedRoles;

    SidebarApp(String title, Set<UserRole> allowedRoles) {
        this.title = title;
        this.allowedRoles = allowedRoles;
    }

    public boolean isOpenTo(UserRole role) {
        return allowedRoles.contains(role);
    }
}
