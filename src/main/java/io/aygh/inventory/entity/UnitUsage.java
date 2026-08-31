package io.aygh.inventory.entity;

/**
 * Which side of the trade a {@link CategoryUnit} authorises. A unit permitted
 * for both is two rows, not a third enum constant: one row per permission keeps
 * "may this category be sold in kg" a single indexed lookup.
 */
public enum UnitUsage {
    PURCHASE,
    SELLING
}
