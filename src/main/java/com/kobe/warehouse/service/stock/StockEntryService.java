package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.service.dto.CommandeDTO;

public interface StockEntryService {
  Commande saveSaisieEntreeStock(CommandeDTO commandeDTO);

  void finalizeSaisieEntreeStock(CommandeDTO commandeDTO);
}
