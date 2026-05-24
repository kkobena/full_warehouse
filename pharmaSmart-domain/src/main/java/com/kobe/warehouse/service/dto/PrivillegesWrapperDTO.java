package com.kobe.warehouse.service.dto;

import java.util.List;

public record PrivillegesWrapperDTO(List<PrivillegesDTO> associes, List<PrivillegesDTO> others) {}
