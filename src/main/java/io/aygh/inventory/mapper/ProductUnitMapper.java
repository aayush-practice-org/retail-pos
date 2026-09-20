package io.aygh.inventory.mapper;

import io.aygh.inventory.dto.request.ProductPurchaseUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductPurchaseUnitUpdateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitCreateRequest;
import io.aygh.inventory.dto.request.ProductSellingUnitUpdateRequest;
import io.aygh.inventory.dto.response.ProductPurchaseUnitDetailResponse;
import io.aygh.inventory.dto.response.ProductPurchaseUnitResponse;
import io.aygh.inventory.dto.response.ProductSellingUnitResponse;
import io.aygh.inventory.dto.response.ProductVatResponse;
import io.aygh.inventory.entity.ProductPurchaseUnit;
import io.aygh.inventory.entity.ProductPurchaseVat;
import io.aygh.inventory.entity.ProductSellingUnit;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * The product's trading configuration and its VAT rates.
 * <p>
 * {@code product} is never mapped in either direction: outbound it would
 * recurse back into the product that owns the row, and inbound it is decided by
 * the path, not the body.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = UnitMapper.class)
public interface ProductUnitMapper {

    // ── Purchase ──────────────────────────────────────────────────────────

    @Mapping(target = "currentVatRate", ignore = true)
    ProductPurchaseUnitResponse toResponse(ProductPurchaseUnit purchaseUnit);

    List<ProductPurchaseUnitResponse> toPurchaseResponses(List<ProductPurchaseUnit> purchaseUnits);

    @Mapping(target = "currentVatRate", ignore = true)
    @Mapping(target = "vatRates", source = "vatRates")
    ProductPurchaseUnitDetailResponse toDetail(ProductPurchaseUnit purchaseUnit);

    /**
     * Flattens the active rate onto the row. Read from the loaded collection rather
     * than queried, because every caller of this has already fetched it.
     */
    @AfterMapping
    default void fillCurrentVat(ProductPurchaseUnit source, @MappingTarget ProductPurchaseUnitResponse target) {
        if (source.getVatRates() != null && !source.getVatRates().isEmpty()) {
            ProductPurchaseVat latest = source.getVatRates().get(source.getVatRates().size() - 1);
            target.setCurrentVatRate(latest.getRate());
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "vatRates", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    ProductPurchaseUnit toEntity(ProductPurchaseUnitCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "vatRates", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void applyUpdate(ProductPurchaseUnitUpdateRequest request, @MappingTarget ProductPurchaseUnit purchaseUnit);

    // ── Selling ───────────────────────────────────────────────────────────

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    ProductSellingUnitResponse toResponse(ProductSellingUnit sellingUnit);

    List<ProductSellingUnitResponse> toSellingResponses(List<ProductSellingUnit> sellingUnits);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    ProductSellingUnit toEntity(ProductSellingUnitCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void applyUpdate(ProductSellingUnitUpdateRequest request, @MappingTarget ProductSellingUnit sellingUnit);

    // ── VAT ───────────────────────────────────────────────────────────────

    ProductVatResponse toResponse(ProductPurchaseVat vat);

    List<ProductVatResponse> toVatResponses(List<ProductPurchaseVat> rates);
}
