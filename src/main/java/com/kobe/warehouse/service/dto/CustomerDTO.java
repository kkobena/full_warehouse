package com.kobe.warehouse.service.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Payment;
import com.kobe.warehouse.domain.UninsuredCustomer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = AssuredCustomerDTO.class, name = "ASSURE"),
    @JsonSubTypes.Type(value = UninsuredCustomerDTO.class, name = "STANDARD")
})
public class CustomerDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String fullName;
    private int encours;
    private String categorie;
    private List<SaleDTO> sales = new ArrayList<>();
    private Set<Payment> payments = new HashSet<>();
    private String code;

    public String getCategorie() {
        return categorie;
    }

    public CustomerDTO setCategorie(String categorie) {
        this.categorie = categorie;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getCode() {
        return code;
    }

    public CustomerDTO setCode(String code) {
        this.code = code;
        return this;
    }

    public CustomerDTO setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public CustomerDTO setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public CustomerDTO setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public CustomerDTO setEmail(String email) {
        this.email = email;
        return this;
    }

    public int getEncours() {
        return encours;
    }

    public CustomerDTO setEncours(int encours) {
        this.encours = encours;
        return this;
    }

    public List<SaleDTO> getSales() {
        return sales;
    }

    public CustomerDTO setSales(List<SaleDTO> sales) {
        this.sales = sales;
        return this;
    }

    public Set<Payment> getPayments() {
        return payments;
    }

    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }

    public CustomerDTO() {
        super();
    }

    public CustomerDTO(Customer customer) {
        super();
        this.firstName = customer.getFirstName();
        this.lastName = customer.getLastName();
        this.phone = customer.getPhone();
        this.email = customer.getEmail();
        this.encours = 0;
        this.id = customer.getId();
        this.fullName = customer.getFirstName() + " " + customer.getLastName();
        this.code = customer.getCode();
        if (customer instanceof AssuredCustomer) {
            this.categorie = "ASSURE";


        } else if (customer instanceof UninsuredCustomer) {
            this.categorie = "STANDARD";
        }

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }


}
