package com.kobe.warehouse.service.dto.builder;

public final class QueryBuilderConstant {

    public static final String CA = " AND s.ca='CA' ";
    public static final String CA_DEPOT = " AND s.ca='CA_DEPOT' ";
    public static final String CALLEBASE = " AND s.ca='CALLEBASE' ";
    public static final String TO_IGNORE = " AND s.ca='TO_IGNORE' ";
    public static final String DIFFERE = " AND s.differe=1 ";
    public static final String LIMIT_STATEMENT = "{limit_statement}";
    public static final String LIKE_STATEMENT = "{like_statement}";
    public static final String ORDER_BY_STATEMENT = "{order_by_statement}";
    public static final String QUANTITY_QUERY_STATEMENT = "{quantity_query}";
    public static final String AMOUNT_QUERY_STATEMENT = "{amount_query}";
}
