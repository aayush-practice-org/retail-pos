package io.aygh.unit.entity;

/**
 * The physical dimension a unit measures. Conversion is only ever allowed
 * between units of the same measurement type.
 */
public enum MeasurementType {
    WEIGHT,
    VOLUME,
    COUNT,
    LENGTH
}
