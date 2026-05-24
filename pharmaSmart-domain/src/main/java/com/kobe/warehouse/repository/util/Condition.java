package com.kobe.warehouse.repository.util;

public final class Condition {

    public enum LogicalOperatorType {
        AND,
        OR,
        END,
    }

    public enum PathType {
        SINGLE,
        MULTIPLE,
    }

    public enum OperationType {
        EQUAL,
        NOT_EQUAL,
        GREATER_THAN,
        LESS_THAN,
        LIKE,
        LESS_OR_EQUAL_THAN,
        GREATER_OR_EQUAL_THAN,
    }

    private final String[] leftHand;
    private final String rightHand;
    private final OperationType operation;
    private final LogicalOperatorType operator;

    public Condition(String[] leftHand, String rightHand, OperationType operation, LogicalOperatorType operator) {
        this.leftHand = leftHand;
        this.rightHand = rightHand;
        this.operation = operation;
        this.operator = operator;
    }

    public String[] getLeftHand() {
        return leftHand;
    }

    public String getRightHand() {
        return rightHand;
    }

    public OperationType getOperation() {
        return operation;
    }

    public LogicalOperatorType getOperator() {
        return operator;
    }
}
