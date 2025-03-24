package com.kobe.warehouse.service.utils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import org.springframework.util.StringUtils;

public final class DateUtil {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DD_MM_YYYY = "dd-MM-yyyy";
    private static final String DD_MM_YYYY_FR = "dd/MM/yyyy";
    private static final String TIME_FORMAT = "HH:mm";

    private DateUtil() {}

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

    public static String displayMonthName(Month month) {
        Objects.requireNonNull(month);
        return month.getDisplayName(TextStyle.FULL, Locale.FRANCE);
    }

    public static String format(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern(DD_MM_YYYY));
    }

    public static LocalDate formaFromYearMonth(String date) {
        if (StringUtils.hasLength(date)) {
            String[] parts = date.split("-");
            Month month = Month.of(Integer.parseInt(parts[1]));
            return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), month.maxLength());
        }
        return null;
    }
    public static String formatFr(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern(DD_MM_YYYY_FR));
    }

}
