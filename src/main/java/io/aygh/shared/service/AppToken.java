package io.aygh.shared.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * The claims carried inside a PASETO bearer token.
 * <p>
 * This is the only thing the server trusts about a request: the tenant it routes
 * to, the role it authorises against and the account it attributes the work to
 * all come from here, never from a header or a body the caller controls. The
 * token is encrypted and authenticated (PASETO v4.local), so a client can
 * neither read nor edit these.
 * <p>
 * Timestamps are epoch seconds rather than {@code Instant}s because this is a
 * wire format: a long means the same thing whatever the JSON date settings
 * happen to be on the day someone changes them.
 */
@Builder
public record AppToken(

        @JsonProperty("sub") UUID userId,
        @JsonProperty("username") String username,
        @JsonProperty("role") String role,

        /** The admin this account works for; null for the super admin. */
        @JsonProperty("tid") UUID tenantId,
        /** The schema this account's work is routed to; null for the super admin. */
        @JsonProperty("slug") String tenantSlug,

        @JsonProperty("iat") long issuedAtEpochSeconds,
        @JsonProperty("exp") long expiresAtEpochSeconds,

        /**
         * When the account itself lapses — a subscription or contract end. Zero
         * means it does not. Held separately from {@code exp} so that shortening
         * token lifetime and ending a subscription stay independent decisions.
         */
        @JsonProperty("aexp") long accountExpiresAtEpochSeconds
) {

    public Instant expiresAt() {
        return Instant.ofEpochSecond(expiresAtEpochSeconds);
    }

    public Instant accountExpiresAt() {
        return accountExpiresAtEpochSeconds <= 0 ? null : Instant.ofEpochSecond(accountExpiresAtEpochSeconds);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt());
    }

    /**
     * Whether the subscription or contract behind the account has run out.
     */
    public boolean isAccountExpired(Instant now) {
        Instant accountExpiry = accountExpiresAt();
        return accountExpiry != null && !now.isBefore(accountExpiry);
    }
}
