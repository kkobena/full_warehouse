package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.service.dto.filter.CommandeFilterDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CustomizedCommandeService {
  List<Commande> fetchCommandes(CommandeFilterDTO commandeFilterDTO, Pageable pageable);

  long countfetchCommandes(CommandeFilterDTO commandeFilterDTO);
}
