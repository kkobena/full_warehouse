package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Ticket;
import com.kobe.warehouse.domain.User;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class TicketDTO {
    private String code;

    private Integer montantAttendu;

    private Integer montantPaye;

    private Integer montantRendu;

    private LocalDateTime created;

    private String user;

    private SaleDTO sale;

    private Set<PaymentDTO> payments = new HashSet<>();

    private CustomerDTO customer;

    public TicketDTO(Ticket ticket) {
        this.code = ticket.getCode();
        this.montantAttendu = ticket.getMontantAttendu();
        this.montantPaye = ticket.getMontantPaye();
        this.montantRendu = ticket.getMontantRendu();
        this.created = ticket.getCreated();
        User user = ticket.getUser();
        this.user = user.getFirstName() + " " + user.getLastName();
        this.sale = new SaleDTO(ticket.getSale());
        if (ticket.getCustomer() != null) {
            this.customer = new CustomerDTO(ticket.getCustomer());
        }
    }

    public TicketDTO() {
    }

    public static TicketDTO build(Ticket ticket) {
        TicketDTO dto = new TicketDTO();
        dto.setCode(ticket.getCode());
        dto.setMontantAttendu(ticket.getMontantAttendu());
        dto.setMontantPaye(ticket.getMontantPaye());
        dto.setMontantRendu(ticket.getMontantRendu());
        dto.setCreated(ticket.getCreated());
        User user = ticket.getUser();
        dto.setUser(user.getFirstName() + " " + user.getLastName());
        return dto;
    }

  public TicketDTO setCode(String code) {
        this.code = code;
        return this;
    }

  public TicketDTO setMontantAttendu(Integer montantAttendu) {
        this.montantAttendu = montantAttendu;
        return this;
    }

  public TicketDTO setMontantPaye(Integer montantPaye) {
        this.montantPaye = montantPaye;
        return this;
    }

  public TicketDTO setMontantRendu(Integer montantRendu) {
        this.montantRendu = montantRendu;
        return this;
    }

  public TicketDTO setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

  public TicketDTO setUser(String user) {
        this.user = user;
        return this;
    }

  public TicketDTO setSale(SaleDTO sale) {
        this.sale = sale;
        return this;
    }

  public TicketDTO setPayments(Set<PaymentDTO> payments) {
        this.payments = payments;
        return this;
    }

  public TicketDTO setCustomer(CustomerDTO customer) {
        this.customer = customer;
        return this;
    }
}
