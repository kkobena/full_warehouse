package com.kobe.warehouse.service.mobile.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for handling pagination in mobile API responses.
 * Provides reusable methods for paginating lists and building paginated responses.
 */
public final class PaginationHelper {

    public static final String HEADER_TOTAL_COUNT = "X-Total-Count";
    public static final String HEADER_TOTAL_PAGES = "X-Total-Pages";
    public static final String HEADER_CURRENT_PAGE = "X-Current-Page";
    public static final String HEADER_PAGE_SIZE = "X-Page-Size";
    public static final String HEADER_HAS_NEXT = "X-Has-Next";
    public static final String HEADER_HAS_PREVIOUS = "X-Has-Previous";

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PaginationHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Paginate a list and return a ResponseEntity with pagination headers.
     *
     * @param items Full list of items
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param <T> Type of items
     * @return ResponseEntity with paginated list and headers
     */
    public static <T> ResponseEntity<List<T>> createPaginatedResponse(List<T> items, int page, int size) {
        int validatedSize = validateSize(size);
        int totalCount = items.size();
        int totalPages = calculateTotalPages(totalCount, validatedSize);
        int validatedPage = validatePage(page, totalPages);

        List<T> paginatedItems = paginateList(items, validatedPage, validatedSize);
        HttpHeaders headers = createPaginationHeaders(totalCount, totalPages, validatedPage, validatedSize);

        return ResponseEntity.ok()
            .headers(headers)
            .body(paginatedItems);
    }

    /**
     * Paginate a list and return a ResponseEntity with pagination headers.
     * Uses lazy loading for the total count.
     *
     * @param itemsSupplier Supplier for paginated items
     * @param totalCountSupplier Supplier for total count
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param <T> Type of items
     * @return ResponseEntity with paginated list and headers
     */
    public static <T> ResponseEntity<List<T>> createPaginatedResponse(
        Supplier<List<T>> itemsSupplier,
        Supplier<Integer> totalCountSupplier,
        int page,
        int size
    ) {
        int validatedSize = validateSize(size);
        int totalCount = totalCountSupplier.get();
        int totalPages = calculateTotalPages(totalCount, validatedSize);
        int validatedPage = validatePage(page, totalPages);

        List<T> items = itemsSupplier.get();
        HttpHeaders headers = createPaginationHeaders(totalCount, totalPages, validatedPage, validatedSize);

        return ResponseEntity.ok()
            .headers(headers)
            .body(items);
    }

    /**
     * Create pagination metadata without building a full response.
     *
     * @param totalCount Total number of items
     * @param page Current page (0-indexed)
     * @param size Page size
     * @return PaginationMetadata with calculated values
     */
    public static PaginationMetadata createMetadata(int totalCount, int page, int size) {
        int validatedSize = validateSize(size);
        int totalPages = calculateTotalPages(totalCount, validatedSize);
        int validatedPage = validatePage(page, totalPages);

        return new PaginationMetadata(
            totalCount,
            totalPages,
            validatedPage,
            validatedSize,
            validatedPage < totalPages - 1,
            validatedPage > 0
        );
    }

    /**
     * Paginate a list by extracting a sublist for the given page.
     *
     * @param items Full list of items
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param <T> Type of items
     * @return Sublist for the requested page
     */
    public static <T> List<T> paginateList(List<T> items, int page, int size) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        int validatedSize = validateSize(size);
        int fromIndex = page * validatedSize;

        if (fromIndex >= items.size()) {
            return new ArrayList<>();
        }

        int toIndex = Math.min(fromIndex + validatedSize, items.size());
        return new ArrayList<>(items.subList(fromIndex, toIndex));
    }

    /**
     * Create HTTP headers for pagination.
     *
     * @param totalCount Total number of items
     * @param totalPages Total number of pages
     * @param currentPage Current page (0-indexed)
     * @param pageSize Page size
     * @return HttpHeaders with pagination information
     */
    public static HttpHeaders createPaginationHeaders(int totalCount, int totalPages, int currentPage, int pageSize) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_TOTAL_COUNT, String.valueOf(totalCount));
        headers.add(HEADER_TOTAL_PAGES, String.valueOf(totalPages));
        headers.add(HEADER_CURRENT_PAGE, String.valueOf(currentPage));
        headers.add(HEADER_PAGE_SIZE, String.valueOf(pageSize));
        headers.add(HEADER_HAS_NEXT, String.valueOf(currentPage < totalPages - 1));
        headers.add(HEADER_HAS_PREVIOUS, String.valueOf(currentPage > 0));

        // Expose headers to CORS clients
        headers.setAccessControlExposeHeaders(List.of(
            HEADER_TOTAL_COUNT,
            HEADER_TOTAL_PAGES,
            HEADER_CURRENT_PAGE,
            HEADER_PAGE_SIZE,
            HEADER_HAS_NEXT,
            HEADER_HAS_PREVIOUS
        ));

        return headers;
    }

    /**
     * Calculate total number of pages.
     *
     * @param totalCount Total number of items
     * @param pageSize Page size
     * @return Total number of pages
     */
    public static int calculateTotalPages(int totalCount, int pageSize) {
        if (totalCount <= 0 || pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalCount / pageSize);
    }

    /**
     * Validate and constrain page size.
     *
     * @param size Requested page size
     * @return Validated page size (between 1 and MAX_SIZE)
     */
    public static int validateSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * Validate and constrain page number.
     *
     * @param page Requested page number
     * @param totalPages Total number of pages
     * @return Validated page number (between 0 and totalPages - 1)
     */
    public static int validatePage(int page, int totalPages) {
        if (page < 0) {
            return 0;
        }
        if (totalPages > 0 && page >= totalPages) {
            return totalPages - 1;
        }
        return page;
    }

    /**
     * Calculate the offset for database queries.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Offset value
     */
    public static int calculateOffset(int page, int size) {
        return page * validateSize(size);
    }

    /**
     * Record containing pagination metadata.
     */
    public record PaginationMetadata(
        int totalCount,
        int totalPages,
        int currentPage,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
    ) {
        /**
         * Check if this is the first page.
         */
        public boolean isFirst() {
            return currentPage == 0;
        }

        /**
         * Check if this is the last page.
         */
        public boolean isLast() {
            return !hasNext;
        }

        /**
         * Check if there are any items.
         */
        public boolean isEmpty() {
            return totalCount == 0;
        }
    }
}
