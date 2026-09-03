package io.aygh.vendor.mapper;

import io.aygh.vendor.dto.response.VendorHistoryResponse;
import io.aygh.vendor.entity.VendorHistory;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Reads the vendor's name as well as its id, so every read that feeds this must
 * have fetched the association — see the entity graphs on
 * {@link io.aygh.vendor.repository.VendorHistoryRepository}.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VendorHistoryMapper {

    @Mapping(target = "vendorId", source = "vendor.id")
    @Mapping(target = "vendorName", source = "vendor.name")
    VendorHistoryResponse toResponse(VendorHistory history);
}
