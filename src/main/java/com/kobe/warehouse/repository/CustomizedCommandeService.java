package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.service.dto.CommandeFilterDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomizedCommandeService {
  List<Commande> fetchCommandes(CommandeFilterDTO commandeFilterDTO, Pageable pageable);

  long countfetchCommandes(CommandeFilterDTO commandeFilterDTO);
}
