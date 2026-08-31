package io.aygh.identity.service.auth;

import io.aygh.identity.dto.request.ChangePasswordRequest;
import io.aygh.identity.dto.request.LoginRequest;
import io.aygh.identity.dto.response.LoginResponse;

public interface UserAuthService {

    /** Verifies credentials and issues a bearer token, or refuses. */
    LoginResponse login(LoginRequest request);

    /** Changes the signed-in account's own password. */
    void changeOwnPassword(ChangePasswordRequest request);
}
