package com.example.resilient_api.domain.model;

public record PaginationRequest(
        int page,
        int size,
        SortField sortBy,
        SortDirection sortDirection
) {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    public PaginationRequest {
        if (page < 0) {
            page = DEFAULT_PAGE;
        }
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        if (sortBy == null) {
            sortBy = SortField.NAME;
        }
        if (sortDirection == null) {
            sortDirection = SortDirection.ASC;
        }
    }

    public long getOffset() {
        return (long) page * size;
    }

    public enum SortField {
        NAME,
        TECHNOLOGY_COUNT
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}

