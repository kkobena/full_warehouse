package com.kobe.warehouse.service.utils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import org.apache.commons.net.time.TimeTCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateUtil {

    private final static Logger log = LoggerFactory.getLogger(DateUtil.class);

    public static LocalDate getLastMonthFromNow() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        return LocalDate.of(lastMonth.getYear(), lastMonth.getMonth(), lastMonth.lengthOfMonth());
    }

    public static LocalDate getNthLastMonthFromNow(int nthMoth) {
        LocalDate lastMonth = LocalDate.now().minusMonths(nthMoth);
        return LocalDate.of(lastMonth.getYear(), lastMonth.getMonth(), 1);
    }

    public static Date getNistTime() {
        TimeTCPClient timeTCPClient = new TimeTCPClient();
        try {

            // "time.nist.gov"
            timeTCPClient.connect("time.nist.gov");
            Date date = timeTCPClient.getDate();

            return date;

        } catch (IOException e) {
            log.debug("", e);
            return null;
        }
    }
}
