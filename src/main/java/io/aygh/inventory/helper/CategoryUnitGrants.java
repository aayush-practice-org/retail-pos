package io.aygh.inventory.helper;

import io.aygh.inventory.entity.Category;
import io.aygh.inventory.entity.CategoryUnit;
import io.aygh.inventory.entity.Unit;
import io.aygh.inventory.entity.UnitUsage;
import io.aygh.inventory.repository.CategoryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * A category's unit permissions, granted as they turn out to be needed.
 * <p>
 * These used to be a gate: configuring a product to sell in kilograms failed
 * unless someone had first authorised kilograms on its category. The gate
 * caught very little. Selling milk by the metre is already impossible —
 * measurement types do not mix — so all it stood between the user and was
 * selling rice by the Dozen, which is a typo, not a policy breach. Against that
 * it made two setup calls mandatory before any product could be priced, and a
 * real "Grocery" category holding both loose rice and packaged noodles ends up
 * permitting everything anyway, at which point the policy is decoration that
 * still charges rent.
 * <p>
 * So the permission is now a record of what the category is actually used for,
 * accumulated from the products in it. The table keeps its shape and its
 * readers — the UI still orders a unit dropdown by what this category trades in
 * — it just stops being something a human has to fill in first.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryUnitGrants {

    private final CategoryUnitRepository categoryUnitRepository;

    /**
     * Records that {@code category} trades in {@code unit} on this side, if it
     * is not on record already. Idempotent, so every product created in the
     * category may call it without checking first.
     */
    public void grantIfAbsent(Category category, Unit unit, UnitUsage usage) {
        if (categoryUnitRepository.existsByCategoryIdAndUnitIdAndUsage(
                category.getId(), unit.getId(), usage)) {
            return;
        }
        categoryUnitRepository.save(CategoryUnit.builder()
                .category(category)
                .unit(unit)
                .usage(usage)
                .build());
        log.info("Category '{}' now trades in '{}' for {}", category.getName(), unit.getName(), usage);
    }
}
