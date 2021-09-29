package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
public class ThirdPartySales extends  Sales implements Serializable {
    private static final long serialVersionUID = 1L;
    @ManyToOne
    @NotNull
    @JsonIgnoreProperties(value = "sales", allowSetters = true)
    private AssuredCustomer assuredCustomer;


    public AssuredCustomer getAssuredCustomer() {
        return assuredCustomer;
    }

    public void setAssuredCustomer(AssuredCustomer assuredCustomer) {
        this.assuredCustomer = assuredCustomer;
    }
}
