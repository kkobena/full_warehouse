package com.kobe.warehouse.domain;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "ticket")
public class
Ticket implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotNull
    @Size(max = 50)
    @Id
    @Column(length = 50)
    private String code;
    @NotNull
    @Column(name = "montant_attendu", nullable = false)
    private Integer montantAttendu = 0;
    @NotNull
    @Column(name = "montant_paye", nullable = false)
    private Integer montantPaye = 0;
    @NotNull
    @Column(name = "montant_rendu", nullable = false)
    private Integer montantRendu = 0;
    @NotNull
    @Column(name = "montant_verse", nullable = false)
    private Integer montantVerse = 0;
    @NotNull
    @Column(name = "rest_to_pay", nullable = false)
    private Integer restToPay = 0;
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant created;
    @NotNull
    @ManyToOne(optional = false)
    private User user;
    @ManyToOne
    private Sales sale;
    @OneToMany(mappedBy = "ticket", fetch = FetchType.EAGER)
    private Set<Payment> payments = new HashSet<>();
    @ManyToOne
    private Customer customer;
    @Column(length = 100)
    private String tva;

    public String getTva() {
        return tva;
    }

    public Ticket setTva(String tva) {
        this.tva = tva;
        return this;
    }

    public Integer getRestToPay() {
        return restToPay;
    }

    public Ticket setRestToPay(Integer restToPay) {
        this.restToPay = restToPay;
        return this;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Ticket setCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Ticket setCode(String code) {
        this.code = code;
        return this;
    }

    public Integer getMontantAttendu() {
        return montantAttendu;
    }

    public Ticket setMontantAttendu(Integer montantAttendu) {
        this.montantAttendu = montantAttendu;
        return this;
    }

    public Integer getMontantPaye() {
        return montantPaye;
    }

    public Ticket setMontantPaye(Integer montantPaye) {
        this.montantPaye = montantPaye;
        return this;
    }

    public Integer getMontantRendu() {
        return montantRendu;
    }

    public Ticket setMontantRendu(Integer montantRendu) {
        this.montantRendu = montantRendu;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public Ticket setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Ticket setUser(User user) {
        this.user = user;
        return this;
    }

    public Sales getSale() {
        return sale;
    }

    public Ticket setSale(Sales sale) {
        this.sale = sale;
        return this;
    }

    public Set<Payment> getPayments() {
        return payments;
    }

    public Ticket setPayments(Set<Payment> payments) {
        this.payments = payments;
        return this;
    }

    public Integer getMontantVerse() {
        return montantVerse;
    }

    public Ticket setMontantVerse(Integer montantVerse) {
        this.montantVerse = montantVerse;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return code.equals(ticket.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
