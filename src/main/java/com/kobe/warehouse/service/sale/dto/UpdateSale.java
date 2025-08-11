package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleLineDTO;
import java.util.Map;
import java.util.Set;

public record UpdateSale(
    long id,
    AssuredCustomerDTO customer,
    AssuredCustomerDTO ayantDroit,
    Set<ThirdPartySaleLineDTO> thirdPartySaleLines,
    Map<Object, Object> initialValue,
    Map<Object, Object> finalValue
) {}
