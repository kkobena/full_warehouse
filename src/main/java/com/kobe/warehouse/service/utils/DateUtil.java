package com.kobe.warehouse.service.utils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateUtil {

  private static final Logger log = LoggerFactory.getLogger(DateUtil.class);
  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private static final String TIME_FORMAT = "HH:mm";

  public static LocalDate getLastMonthFromNow() {
    LocalDate lastMonth = LocalDate.now().minusMonths(1);
    return LocalDate.of(lastMonth.getYear(), lastMonth.getMonth(), lastMonth.lengthOfMonth());
  }

  public static LocalDateTime fromString(String dateValue) {
    Objects.requireNonNull(dateValue);
    return LocalDateTime.parse(dateValue, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
  }

  public static String format(LocalDateTime date) {
    if (date == null) {
      return "";
    }
    return date.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
  }

  public static String format(Timestamp date) {
    if (date == null) {
      return "";
    }
    return date.toLocalDateTime().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
  }

  public static LocalDate getNthLastMonthFromNow(int nthMoth) {
    LocalDate lastMonth = LocalDate.now().minusMonths(nthMoth);
    return LocalDate.of(lastMonth.getYear(), lastMonth.getMonth(), 1);
  }

  public static Date getNistTime() {
    //  TimeTCPClient timeTCPClient = new TimeTCPClient();

    // "time.nist.gov"
    //  timeTCPClient.connect("time.nist.gov");
    Date date = new Date(); // timeTCPClient.getDate();

    return date;
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
