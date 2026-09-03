package io.aygh.vendor.mapper;

import io.aygh.vendor.dto.request.VendorRequest;
import io.aygh.vendor.dto.response.VendorResponse;
import io.aygh.vendor.entity.Vendor;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * {@code histories} is never mapped from a request: what was bought from a
 * vendor is recorded by purchasing, not restated whenever someone corrects an
 * address.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VendorMapper {

    VendorResponse toResponse(Vendor vendor);

    List<VendorResponse> toResponses(List<Vendor> vendors);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "histories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Vendor toEntity(VendorRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "histories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void applyUpdate(VendorRequest request, @MappingTarget Vendor vendor);
}
