package com.kobe.warehouse.domain.enumeration;

/**
 * Frequency of scheduled report execution
 */
public enum ScheduledReportFrequency {
    DAILY,      // Every day at specified time
    WEEKLY,     // Once a week on specified day
    MONTHLY,    // Once a month on specified day
    CUSTOM      // Custom cron expression
}
