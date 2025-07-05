package com.kobe.warehouse.service.utils;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.RuleBasedNumberFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public final class NumberUtil {

    private static final Logger log = LoggerFactory.getLogger(NumberUtil.class);

    private NumberUtil() {}

    public static String formatToString(Number value) {
        String result = null;
        try {
            DecimalFormatSymbols amountSymbols = new DecimalFormatSymbols();

            amountSymbols.setGroupingSeparator(' ');

            DecimalFormat amountFormat = new DecimalFormat("###,###", amountSymbols);
            result = amountFormat.format(value);
        } catch (NumberFormatException ex) {
            log.debug("", ex);
        }
        return result;
    }

    public static String formatToStringIfNotNull(Number value) {
        if (value == null) {
            return "";
        }

        try {
            DecimalFormatSymbols amountSymbols = new DecimalFormatSymbols();

            amountSymbols.setGroupingSeparator(' ');

            DecimalFormat amountFormat = new DecimalFormat("###,###", amountSymbols);
            return amountFormat.format(value);
        } catch (NumberFormatException ex) {
            log.debug("", ex);
            return "";
        }
    }

    public static String getNumberToWords(long num) {
        RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(Locale.FRANCE, RuleBasedNumberFormat.SPELLOUT);
        return formatter.format(num);
    }

    public static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Integer doubleFromString(String doubleStringValue) {
        if (!StringUtils.hasLength(doubleStringValue)) {
            return null;
        }

        return (int) Double.parseDouble(doubleStringValue);
    }

    public static Integer intFromString(String intStringValue) {
        if (!StringUtils.hasLength(intStringValue)) {
            return null;
        }
        try {
            return Integer.valueOf(intStringValue);
        } catch (Exception e) {
            return doubleFromString(intStringValue);
        }
    }

    public double round(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public double round(long value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public double round(int value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
