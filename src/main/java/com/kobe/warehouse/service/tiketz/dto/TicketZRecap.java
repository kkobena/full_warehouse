package com.kobe.warehouse.service.tiketz.dto;

import java.util.List;

public record TicketZRecap(long userId, String userName, List<TicketZData> datas, List<TicketZData> summary) {}
