package com.kobe.warehouse.service.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Payment;

public class CustomerDTO {
	private Long id;
	private String firstName;
	private String lastName;
	private String phone;
	private String email;
	private String fullName;
	private int encours;
	private List<SaleDTO> sales = new ArrayList<>();
	private Set<Payment> payments = new HashSet<>();

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public int getEncours() {
		return encours;
	}

	public void setEncours(int encours) {
		this.encours = encours;
	}

	public List<SaleDTO> getSales() {
		return sales;
	}

	public void setSales(List<SaleDTO> sales) {
		this.sales = sales;
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
		this.id=customer.getId();
		this.fullName= customer.getFirstName()+" "+ customer.getLastName();

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
