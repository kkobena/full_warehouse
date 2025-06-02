package com.kobe.warehouse.service.tiketz.dto;

import java.util.List;

public record TicketZ(List<TicketZData> summaries, List<TicketZRecap> datas) {}
