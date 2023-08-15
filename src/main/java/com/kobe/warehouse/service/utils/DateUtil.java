package com.kobe.warehouse.service.utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import org.apache.commons.net.time.TimeTCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateUtil {

  private static final Logger log = LoggerFactory.getLogger(DateUtil.class);

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

  public static String getMonthFromMonth(Month month) {
    return switch (month) {
      case JANUARY -> "JANVIER";
      case FEBRUARY -> "FEVRIER";
      case MARCH -> "MARS";
      case APRIL -> "AVRIL";
      case MAY -> "MAI";
      case JUNE -> "JUIN";
      case JULY -> "JUILLET";
      case AUGUST -> "AOÛT";
      case SEPTEMBER -> "SEPTEMBRE";
      case OCTOBER -> "OCTOBRE";
      case NOVEMBER -> "NOVEMBRE";
      case DECEMBER -> "DECEMBRE";
    };
  }
}
