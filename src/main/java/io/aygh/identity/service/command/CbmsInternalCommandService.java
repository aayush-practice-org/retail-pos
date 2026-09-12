package io.aygh.identity.service.command;

import io.aygh.identity.dto.request.CbmsInternalRequest;
import io.aygh.identity.dto.request.CbmsInternalUpdateRequest;
import io.aygh.identity.dto.response.CbmsInternalResponse;

public interface CbmsInternalCommandService {

    CbmsInternalResponse createCbmsInternal(CbmsInternalRequest request);

    CbmsInternalResponse updateCbmsInternal(Long id, CbmsInternalUpdateRequest request);
}
