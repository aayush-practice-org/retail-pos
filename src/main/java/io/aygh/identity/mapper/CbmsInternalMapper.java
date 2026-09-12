package io.aygh.identity.mapper;

import io.aygh.identity.dto.response.CbmsInternalResponse;
import io.aygh.identity.entity.CbmsInternalEntity;
import org.springframework.stereotype.Component;

@Component
public class CbmsInternalMapper {

    public CbmsInternalResponse toResponse(CbmsInternalEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CbmsInternalResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getTenantSlug(),
                entity.getCbmsUsername(),
                entity.getCbmsPassword(),
                entity.getTaxRegistration(),
                entity.isTaxIncluded(),
                entity.getPan()
        );
    }
}
