package io.aygh.identity.service.query;

import io.aygh.identity.dto.response.CbmsInternalResponse;

import java.util.List;

public interface CbmsInternalQueryService {

    List<CbmsInternalResponse> getAll();

    CbmsInternalResponse getById(Long id);
}
