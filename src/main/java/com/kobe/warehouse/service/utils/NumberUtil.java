package com.kobe.warehouse.service.utils;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NumberUtil {

    private final static Logger log = LoggerFactory.getLogger(NumberUtil.class);

    public static String formatLong(Number value) {
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

}
