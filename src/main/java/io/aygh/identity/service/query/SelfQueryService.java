package io.aygh.identity.service.query;

import io.aygh.identity.dto.request.ChangePasswordRequest;
import io.aygh.identity.dto.response.SelfResponse;

public interface SelfQueryService {

    SelfResponse getSelfInfo();

    void changePassword(ChangePasswordRequest request);
}
