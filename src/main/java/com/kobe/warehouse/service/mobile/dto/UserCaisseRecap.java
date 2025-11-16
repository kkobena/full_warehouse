package com.kobe.warehouse.service.mobile.dto;

import java.util.List;

public record UserCaisseRecap(String title, List<ListItem> items, List<ListItem> resume) {}
