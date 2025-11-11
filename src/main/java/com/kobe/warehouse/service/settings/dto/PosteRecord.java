package com.kobe.warehouse.service.settings.dto;

public record PosteRecord(Integer id, String name, String posteNumber, String address, boolean customerDisplay,
                          String customerDisplayPort) {
}
