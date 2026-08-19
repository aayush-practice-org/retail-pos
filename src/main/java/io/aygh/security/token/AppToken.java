package io.aygh.security.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Data
@ToString
public class AppToken {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("username")
    private String userName;

    @JsonProperty("role")
    private String role;

    @JsonProperty("expires_date")
    private Instant expiresDate;

    public void setExpiresAt(long epochMilli) {
        this.expiresDate = Instant.ofEpochMilli(epochMilli);
    }
}
