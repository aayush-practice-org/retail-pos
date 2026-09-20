package io.aygh.inventory.mapper;

import io.aygh.inventory.dto.request.CategoryRequest;
import io.aygh.inventory.dto.response.CategoryDetailResponse;
import io.aygh.inventory.dto.response.CategorySummaryResponse;
import io.aygh.inventory.entity.Category;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * The two response shapes are separate methods rather than one: the summary is
 * what a list returns, and it must not touch {@code categoryUnits} — that is a
 * lazy collection, and rendering it per row is a query per row.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = UnitMapper.class)
public interface CategoryMapper {

    CategorySummaryResponse toSummary(Category category);

    List<CategorySummaryResponse> toSummaries(List<Category> categories);

    /** The unit lists and product count are filled by the query service. */
    @Mapping(target = "purchaseUnits", ignore = true)
    @Mapping(target = "sellingUnits", ignore = true)
    @Mapping(target = "productCount", ignore = true)
    CategoryDetailResponse toDetail(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryUnits", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "defaultCategory", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Category toEntity(CategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryUnits", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "defaultCategory", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void applyUpdate(CategoryRequest request, @MappingTarget Category category);
}
