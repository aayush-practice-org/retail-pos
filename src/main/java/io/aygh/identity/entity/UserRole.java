package io.aygh.identity.entity;

import lombok.Getter;

/**
 * Roles a mart actually staffs, replacing the restaurant's kitchen/waiter/kiosk set.
 *
 * <ul>
 *   <li>{@code ADMIN} — the mart owner: everything, including other admins</li>
 *   <li>{@code MANAGER} — runs the floor: catalogue, staff, sales, reports</li>
 *   <li>{@code CASHIER} — works the till: rings up sales, nothing else</li>
 *   <li>{@code STOREKEEPER} — minds the shelves: products, categories, units</li>
 *   <li>{@code ACCOUNTANT} — reads the money: sales history and reports, no edits</li>
 * </ul>
 */
@Getter
public enum UserRole {
    ADMIN("ROLE_ADMIN"),
    MANAGER("ROLE_MANAGER"),
    CASHIER("ROLE_CASHIER"),
    STOREKEEPER("ROLE_STOREKEEPER"),
    ACCOUNTANT("ROLE_ACCOUNTANT");

    private final String authority;

    UserRole(String authority) {
        this.authority = authority;
    }
}
