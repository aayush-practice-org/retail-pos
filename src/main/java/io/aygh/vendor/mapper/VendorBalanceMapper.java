package io.aygh.vendor.mapper;

import io.aygh.vendor.dto.response.VendorBalanceResponse;
import io.aygh.vendor.entity.VendorBalance;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Only the vendor's id is read off the association, so a lazy proxy answers it
 * without going back to the database.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VendorBalanceMapper {

    @Mapping(target = "vendorId", source = "vendor.id")
    VendorBalanceResponse toResponse(VendorBalance balance);
}
