package com.srms.api.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Builds a {@link Pageable} from optional request params. Returns null when the caller passed
 * neither page nor size, so controllers can fall back to their existing unpaginated method —
 * pagination is opt-in per request, not a breaking change to the endpoint. */
public final class PageRequestUtil {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    private PageRequestUtil() {}

    public static Pageable build(Integer page, Integer size, String sortBy, String sortDir) {
        if (page == null && size == null) return null;
        int resolvedPage = page == null ? 0 : Math.max(0, page);
        int resolvedSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(1, size), MAX_SIZE);
        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(resolvedPage, resolvedSize);
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, sortBy));
    }
}
