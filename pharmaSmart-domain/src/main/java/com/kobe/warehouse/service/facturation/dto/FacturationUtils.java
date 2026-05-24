package com.kobe.warehouse.service.facturation.dto;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;

public class FacturationUtils {

    public Object getValueByField(String fieldName, Object object) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            return null;
        }
    }

    public Long getLongValueByField(String fieldName, Object object) {
        Object value = getValueByField(fieldName, object);
        if (value instanceof Long) {
            return (Long) value;
        }
        return null;
    }

    public Integer getIntValueByField(String fieldName, Object object) {
        Object value = getValueByField(fieldName, object);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return null;
    }

    public BigDecimal getBigDecimalValueByField(String fieldName, Object object) {
        Object value = getValueByField(fieldName, object);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return null;
    }

    public BigInteger getBigIntegerValueByField(String fieldName, Object object) {
        Object value = getValueByField(fieldName, object);
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }
        return null;
    }

    public String getStringValueByField(String fieldName, Object object) {
        Object value = getValueByField(fieldName, object);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }
}
