package io.aygh.identity.service.query;

import io.aygh.identity.dto.response.SelfResponse;

/** The signed-in account describing itself. */
public interface SelfQueryService {

    SelfResponse currentUser();
}
