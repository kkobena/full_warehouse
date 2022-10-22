package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.Ticket;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;

import java.util.List;

public interface TicketService {
  Ticket cloneTicket(Ticket ticket, Sales sales);

  List<Ticket> findAllBySaleId(Long id);

  Ticket buildTicket(
      ThirdPartySales thirdPartySales,
      ThirdPartySaleDTO thirdPartySaleDTO,
      User user,
      String tvaDatas);

  Ticket buildTicket(SaleDTO saleDTO, Sales sales, User user, String tvaDatas);

  void delete(Ticket ticket);
}
