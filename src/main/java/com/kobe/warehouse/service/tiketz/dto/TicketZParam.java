package com.kobe.warehouse.service.tiketz.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record TicketZParam(
    Set<Long> usersId,
    boolean onlyVente,
    LocalDate fromDate,
    LocalDate toDate,
    LocalTime fromTime,
    LocalTime toTime
) {
    public TicketZParam {

        if (fromTime == null) {
            fromTime = LocalTime.MIN;
        }
        if (toTime == null) {
            toTime = LocalTime.MAX;
        }
    }
}
