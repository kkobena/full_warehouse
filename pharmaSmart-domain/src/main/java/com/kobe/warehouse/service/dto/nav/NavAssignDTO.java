package com.kobe.warehouse.service.dto.nav;

import java.util.List;

public record NavAssignDTO(
    String roleName,
    List<NavItemAssignmentDTO> assignments
) {}

