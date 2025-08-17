package com.kobe.warehouse.web.util;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

public class PaginationUtil {

    private static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";
    private static final LinkHeaderUtil linkHeaderUtil = new LinkHeaderUtil();

    private PaginationUtil() {}

    public static <T> HttpHeaders generatePaginationHttpHeaders(UriComponentsBuilder uriBuilder, Page<T> page) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        headers.add("Link", linkHeaderUtil.prepareLinkHeaders(uriBuilder, page));
        return headers;
    }
}
