package io.aygh.inventory.helper;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.dto.request.PackSizeRequest;
import io.aygh.inventory.dto.request.ProductUnitLineRequest;
import io.aygh.inventory.entity.MeasurementType;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * The arithmetic and the guesses that let a product be created without its
 * author stating a base unit or a pack quantity.
 * <p>
 * All of it is derivation, which is why it sits apart from both the validation
 * (which only ever says no) and the service (which only ever orchestrates):
 * these are the questions with a right answer the server can work out for
 * itself, and each one it answers is a field a shopkeeper does not have to.
 */
@Component
@RequiredArgsConstructor
public class ProductUnitFactory {

    private final UnitRepository unitRepository;

    /**
     * A unit from whatever the caller had to hand: an id, a symbol, or a name.
     * <p>
     * Returns null when the line names no unit at all, which the caller reads
     * as "the base unit" — that is what makes {@code sellingPrice: 25} with no
     * unit mean what it obviously means.
     */
    public Unit resolve(Long unitId, String token) {
        if (unitId != null) {
            return unitRepository.findById(unitId)
                    .orElseThrow(() -> new BusinessException("No unit with id " + unitId));
        }
        if (token == null || token.isBlank()) {
            return null;
        }
        String cleaned = token.trim();
        return unitRepository.findBySymbolIgnoreCase(cleaned)
                .or(() -> unitRepository.findByNameIgnoreCase(cleaned))
                .orElseThrow(() -> new BusinessException(
                        "No unit called '" + cleaned + "'. Add it under units first, "
                                + "or use one of the built-in ones (g, kg, ml, l, pc, dz, cm, m)"));
    }

    public Unit resolve(ProductUnitLineRequest line) {
        return resolve(line.unitId(), line.unit());
    }

    /**
     * The unit stock will be counted in, given the unit the product trades in.
     * <p>
     * Always the reference unit of that measurement type, never the trading
     * unit itself: a product sold by the kilogram and one sold by the gram then
     * hold their stock in the same thing, and a later decision to also sell
     * 250g packets needs no restatement of what is already on the shelf.
     */
    public Unit baseUnitFor(MeasurementType measurementType) {
        return unitRepository.findByMeasurementTypeAndReferenceUnitIsTrue(measurementType)
                .orElseThrow(() -> new BusinessException(
                        "No reference unit is configured for " + measurementType
                                + ", so stock in it cannot be counted"));
    }

    /**
     * How many base units one of {@code unit} holds.
     * <p>
     * Resolution order is narrowest first: what the caller stated outright,
     * then what they described in human terms, then what the unit itself knows.
     * Only if all three come up empty is it an error — and it is an error
     * rather than a default of 1, because a Sack silently worth one gram is the
     * kind of wrong that is not noticed until a stocktake.
     */
    public BigDecimal packQuantity(Unit baseUnit, Unit unit, ProductUnitLineRequest line) {
        if (line.packQuantity() != null) {
            return line.packQuantity();
        }
        PackSizeRequest contains = line.contains();
        if (contains != null) {
            Unit inner = resolve(contains.unitId(), contains.unit());
            if (inner == null) {
                return contains.qty();
            }
            requireSameMeasurement(baseUnit, inner);
            return contains.qty().multiply(factorToBase(baseUnit, inner));
        }
        return factorToBase(baseUnit, unit);
    }

    /**
     * A unit's size in base units, or a refusal naming the way out of it.
     */
    private BigDecimal factorToBase(Unit baseUnit, Unit unit) {
        if (unit.getId().equals(baseUnit.getId())) {
            return BigDecimal.ONE;
        }
        BigDecimal factor = unit.getConversionFactor();
        if (factor == null) {
            throw new BusinessException("'" + unit.getName() + "' has no fixed size, so how much of it "
                    + "this product holds has to be stated — send \"contains\": {\"qty\": 50, \"unit\": \""
                    + baseUnit.getSymbol() + "\"}");
        }
        return factor;
    }

    private void requireSameMeasurement(Unit baseUnit, Unit unit) {
        if (baseUnit.getMeasurementType() != unit.getMeasurementType()) {
            throw new BusinessException("'" + unit.getName() + "' measures " + unit.getMeasurementType()
                    + ", but this product is counted in " + baseUnit.getMeasurementType());
        }
    }
}
