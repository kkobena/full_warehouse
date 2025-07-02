package com.kobe.warehouse.service.historique_inventaire;

import com.kobe.warehouse.domain.HistoriqueInventaire;
import com.kobe.warehouse.repository.HistoriqueInventaireRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HistoriqueInventaireServiceImpl implements HistoriqueInventaireService {
    private final HistoriqueInventaireRepository historiqueInventaireRepository;

    public HistoriqueInventaireServiceImpl(HistoriqueInventaireRepository historiqueInventaireRepository) {
        this.historiqueInventaireRepository = historiqueInventaireRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistoriqueInventaire> fetchAllHistoriqueInventaires(Pageable pageable) {
        return this.historiqueInventaireRepository.findAllByOrderByCreatedDesc(pageable);
    }

    @Override
    public void save(HistoriqueInventaire historiqueInventaire) {
        this.historiqueInventaireRepository.save(historiqueInventaire);
    }
}
