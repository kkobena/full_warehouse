package com.kobe.warehouse.service.historique_inventaire;

import com.kobe.warehouse.domain.HistoriqueInventaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HistoriqueInventaireService {
    Page<HistoriqueInventaire> fetchAllHistoriqueInventaires(Pageable pageable);

    void save(HistoriqueInventaire historiqueInventaire);
}
