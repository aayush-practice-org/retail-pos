package io.aygh.shared.response;

import org.springframework.data.domain.Page;

import java.util.List;

public class PaginationUtils {

    private PaginationUtils() {
    }

    public static <T, R> PagedResponse<R> toPagedResponse(Page<T> page, List<R> content) {
        return PagedResponse.<R>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public static <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return toPagedResponse(page, page.getContent());
    }
}
