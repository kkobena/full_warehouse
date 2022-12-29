package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.service.dto.CommandeDTO;

public interface StockEntryService {
    Commande saveSaisieEntreeStock(CommandeDTO commandeDTO);
}
