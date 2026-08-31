package io.aygh.inventory.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * What every inventory response carries: its identity and when it last moved.
 * <p>
 * A class rather than a record because the responses form a hierarchy —
 * {@code Summary} for lists, {@code Detail} for the one-row view — and records
 * cannot extend each other. Subclasses inherit these three fields instead of
 * repeating them nine times.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class BaseResponse {

    private Long id;
    private Instant createdAt;
    private Instant updatedAt;
}
