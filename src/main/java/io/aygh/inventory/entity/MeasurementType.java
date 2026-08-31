package io.aygh.inventory.entity;

/**
 * The physical dimension a unit measures. Two units are only convertible when
 * they share one, which is what stops a request converting Litres into Pieces.
 */
public enum MeasurementType {
    WEIGHT,
    VOLUME,
    COUNT,
    LENGTH
}
