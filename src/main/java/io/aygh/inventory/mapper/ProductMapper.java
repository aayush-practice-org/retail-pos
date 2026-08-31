package io.aygh.inventory.mapper;

import io.aygh.inventory.dto.request.ProductCreateRequest;
import io.aygh.inventory.dto.request.ProductUpdateRequest;
import io.aygh.inventory.dto.response.ProductDetailResponse;
import io.aygh.inventory.dto.response.ProductSummaryResponse;
import io.aygh.inventory.entity.Product;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * The category and base unit are resolved by the service from their ids, so no
 * request ever maps onto them — a body cannot move a product into a category by
 * carrying a nested object.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = {UnitMapper.class, ProductUnitMapper.class})
public interface ProductMapper {

    /**
     * Flattens the category to a name and the price to the default selling
     * unit's, so a listed row carries no nested object.
     */
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "sellingPrice", ignore = true)
    @Mapping(target = "sellingUnitSymbol", ignore = true)
    ProductSummaryResponse toSummary(Product product);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "sellingPrice", ignore = true)
    @Mapping(target = "sellingUnitSymbol", ignore = true)
    ProductDetailResponse toDetail(Product product);

    /**
     * Both response shapes advertise the default selling unit's price. Reading it
     * here rather than in two mapping expressions keeps "which price do we show"
     * in one place.
     */
    @AfterMapping
    default void fillDefaultPrice(Product product, @MappingTarget ProductSummaryResponse response) {
        product.defaultSellingUnit().ifPresent(unit -> {
            response.setSellingPrice(unit.getSellingPrice());
            response.setSellingUnitSymbol(unit.getUnit() == null ? null : unit.getUnit().getSymbol());
        });
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "baseUnit", ignore = true)
    @Mapping(target = "purchaseUnits", ignore = true)
    @Mapping(target = "sellingUnits", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Product toEntity(ProductCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "baseUnit", ignore = true)
    @Mapping(target = "purchaseUnits", ignore = true)
    @Mapping(target = "sellingUnits", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void applyUpdate(ProductUpdateRequest request, @MappingTarget Product product);
}
