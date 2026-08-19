package io.aygh.shared.response;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageableRequest(
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {

    public PageableRequest {
        page = (page == null || page <= 1) ? 0 : page - 1;
        size = size == null || size <= 0 ? 20 : size;

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }

        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "desc";
        }
    }

    public Pageable toPageable() {
        return PageRequest.of(
                page,
                size,
                sortDirection.equalsIgnoreCase("desc")
                        ? Sort.by(sortBy).descending()
                        : Sort.by(sortBy).ascending()
        );
    }
}
