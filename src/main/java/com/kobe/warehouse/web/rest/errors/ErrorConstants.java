package com.kobe.warehouse.web.rest.errors;

import java.net.URI;

public final class ErrorConstants {

    public static final String ERR_CONCURRENCY_FAILURE = "error.concurrencyFailure";
    public static final String ERR_VALIDATION = "error.validation";
    public static final String PROBLEM_BASE_URL = "";
    public static final URI DEFAULT_TYPE = URI.create(PROBLEM_BASE_URL + "/problem-with-message");
    public static final URI CONSTRAINT_VIOLATION_TYPE = URI.create(PROBLEM_BASE_URL + "/constraint-violation");
    public static final URI INVALID_PASSWORD_TYPE = URI.create(PROBLEM_BASE_URL + "/invalid-password");
    public static final URI EMAIL_ALREADY_USED_TYPE = URI.create(PROBLEM_BASE_URL + "/email-already-used");
    public static final URI LOGIN_ALREADY_USED_TYPE = URI.create(PROBLEM_BASE_URL + "/login-already-used");
    public static final URI ERR_STOCK_INSUFFISANT = URI.create(PROBLEM_BASE_URL + "/error.stockInsuffisant");
    public static final URI ERR_WRONG_ENTRY_AMOUNT = URI.create(PROBLEM_BASE_URL + "/error.wrongEntryAmount");
    public static final URI ERR_CUSTOMER_NOT_FOUND = URI.create(PROBLEM_BASE_URL + "/error.customerNotFound");
    public static final URI ERR_CUSTOMER_EXIST = URI.create(PROBLEM_BASE_URL + "/error.customerExist");
    public static final URI ERR_CUSTOMER_ACCOUNT = URI.create(PROBLEM_BASE_URL + "/error.accountAmountError");
    public static final URI ERR_NO_TIERS_PAYANT = URI.create(PROBLEM_BASE_URL + "/error.noTiersPayant");
    public static final URI NUM_BON_ALREADY_USE = URI.create(PROBLEM_BASE_URL + "/error.numBonAlreadyUse");
    public static final URI CASH_REGISTER_NOT_FOUND = URI.create(PROBLEM_BASE_URL + "/error.cashRegisterNotFound");

    private ErrorConstants() {}
}
