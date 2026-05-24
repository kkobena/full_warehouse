package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.UninsuredCustomer;

public class UninsuredCustomerDTO extends CustomerDTO {

    public UninsuredCustomerDTO() {}

    public UninsuredCustomerDTO(UninsuredCustomer customer) {
        super(customer);
    }
}
