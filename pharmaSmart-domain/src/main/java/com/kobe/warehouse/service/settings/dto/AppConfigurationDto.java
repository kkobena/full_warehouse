package com.kobe.warehouse.service.settings.dto;

import com.kobe.warehouse.domain.AppOption;
import com.kobe.warehouse.domain.enumeration.ParametreValueType;

import java.time.LocalDateTime;
import java.util.List;

public record AppConfigurationDto(String name, String description, String value, LocalDateTime created,
                                  LocalDateTime updated, ParametreValueType valueType, String otherValue,
                                  List<AppOption> options) {
}
