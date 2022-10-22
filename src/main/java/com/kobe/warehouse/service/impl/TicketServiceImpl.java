package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.Util;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.Ticket;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.repository.TicketRepository;
import com.kobe.warehouse.service.TicketService;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.TvaEmbeded;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;

    public TicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket cloneTicket(Ticket ticket, Sales copy) {
        Ticket ticketCopy = (Ticket) ticket.clone();
        ticketCopy.setCode(RandomStringUtils.randomNumeric(8));
        ticketCopy.setSale(copy);
        ticketCopy.setCreated(copy.getEffectiveUpdateDate());
        ticketCopy.setMontantVerse(ticketCopy.getMontantVerse() * (-1));
        ticketCopy.setRestToPay(ticketCopy.getRestToPay() * (-1));
        ticketCopy.setMontantAttendu(ticketCopy.getMontantAttendu() * (-1));
        ticketCopy.setMontantPaye(ticketCopy.getMontantPaye() * (-1));
        List<TvaEmbeded> tvaEmbededs = Util.transformTvaEmbeded(ticket.getTva());
        if (tvaEmbededs.size() > 0) {
            for (TvaEmbeded tva : tvaEmbededs) {
                tva.setAmount(tva.getAmount() * (-1));
            }
        }
        ticketCopy.setTva(Util.transformTvaEmbededToString(tvaEmbededs));
        ticketCopy.setCanceled(true);
        this.ticketRepository.save(ticketCopy);
        ticket.setCanceled(true);
        this.ticketRepository.save(ticket);
        return ticketCopy;
    }
  public   List<Ticket> findAllBySaleId(Long id){
        return ticketRepository.findAllBySaleId(id);
    }

  public   Ticket buildTicket(ThirdPartySales thirdPartySales, ThirdPartySaleDTO thirdPartySaleDTO, User user,String tvaDatas) {
        Ticket ticket = new Ticket();
        ticket.setCode(RandomStringUtils.randomNumeric(8));
        ticket.setCreated(Instant.now());
        ticket.setUser(user);
        ticket.setSale(thirdPartySales);
        ticket.setMontantAttendu(thirdPartySales.getAmountToBePaid());
        ticket.setMontantPaye(thirdPartySales.getPayrollAmount());
        ticket.setMontantRendu(thirdPartySaleDTO.getMontantRendue());
        ticket.setRestToPay(thirdPartySaleDTO.getRestToPay());
        ticket.setMontantVerse(thirdPartySaleDTO.getMontantVerse());
        ticket.setPartAssure(thirdPartySales.getPartAssure());
        ticket.setPartTiersPayant(thirdPartySales.getPartTiersPayant());
        ticket.setCustomer(thirdPartySales.getCustomer());
        ticket.setTva(tvaDatas);
        ticket = ticketRepository.save(ticket);
        return ticket;
    }
    public Ticket buildTicket(SaleDTO saleDTO, Sales sales,User user,String tvaDatas) {
        Ticket ticket = new Ticket();
        ticket.setCode(RandomStringUtils.randomNumeric(8));
        ticket.setCreated(Instant.now());
        ticket.setUser(user);
        ticket.setSale(sales);
        ticket.setMontantAttendu(sales.getAmountToBePaid());
        ticket.setMontantPaye(saleDTO.getPayrollAmount());
        ticket.setMontantRendu(saleDTO.getMontantRendue());
        ticket.setRestToPay(saleDTO.getRestToPay());
        ticket.setMontantVerse(saleDTO.getMontantVerse());
        ticket.setTva(tvaDatas);
        CashSale cashSale = (CashSale) sales;
        ticket.setCustomer(cashSale.getCustomer());
        ticket = ticketRepository.save(ticket);
        return ticket;

    }

    @Override
    public void delete(Ticket ticket) {
        this.ticketRepository.delete(ticket);
    }
}
