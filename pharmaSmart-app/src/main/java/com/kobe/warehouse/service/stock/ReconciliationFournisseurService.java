package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.service.dto.ReconciliationFactureDTO;

import java.time.LocalDate;

public interface ReconciliationFournisseurService {

    ReconciliationFactureDTO save(CommandeId commandeId, ReconciliationCommand command);

    ReconciliationFactureDTO findByCommandeId(CommandeId commandeId);

    record ReconciliationCommand(
        String factureReference,
        LocalDate factureDate,
        Integer factureMontantHT,
        Integer factureTVA
    ) {}
}
