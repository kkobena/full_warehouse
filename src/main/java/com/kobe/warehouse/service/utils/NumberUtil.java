package com.kobe.warehouse.service.utils;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.RuleBasedNumberFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
