package com.kobe.warehouse.service.ordonnance.controle;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.CustomerTraitementChroniqueRepository;
import com.kobe.warehouse.repository.RefDciRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.service.dto.controle.MoleculeDTO;
import com.kobe.warehouse.service.dto.controle.ProduitLibelleDTO;
import com.kobe.warehouse.service.dto.controle.TraitementChroniqueRefDTO;
import com.kobe.warehouse.service.dto.controle.TraitementEnCoursDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * « Traitements en cours » d'un client (plan §6.3) : ses achats clôturés et non annulés dans une
 * fenêtre paramétrable, et ses traitements chroniques actifs. Une vente comptant anonyme n'y entre
 * pas : c'est la limite que l'écran affiche.
 */
@Service
@Transactional(readOnly = true)
public class TraitementsEnCoursService {

    private final SalesLineRepository salesLineRepository;
    private final CustomerTraitementChroniqueRepository traitementChroniqueRepository;
    private final RefDciRepository refDciRepository;
    private final MoleculesProduitService moleculesProduitService;
    private final int fenetreJours;

    public TraitementsEnCoursService(
        SalesLineRepository salesLineRepository,
        CustomerTraitementChroniqueRepository traitementChroniqueRepository,
        RefDciRepository refDciRepository,
        MoleculesProduitService moleculesProduitService,
        @Value("${pharma.controle.fenetre-jours:90}") int fenetreJours
    ) {
        this.salesLineRepository = salesLineRepository;
        this.traitementChroniqueRepository = traitementChroniqueRepository;
        this.refDciRepository = refDciRepository;
        this.moleculesProduitService = moleculesProduitService;
        this.fenetreJours = fenetreJours;
    }

    public List<TraitementEnCoursDTO> enCours(Integer customerId) {
        Map<Integer, String> produits = new LinkedHashMap<>();
        for (ProduitLibelleDTO achete : salesLineRepository.findProduitsAchetes(customerId, SalesStatut.CLOSED, LocalDate.now().minusDays(fenetreJours))) {
            produits.put(achete.id(), achete.libelle());
        }

        Set<Integer> dciSeules = new LinkedHashSet<>();
        for (TraitementChroniqueRefDTO chronique : traitementChroniqueRepository.findActifsNonEchus(customerId, LocalDate.now())) {
            if (chronique.produitId() != null) {
                produits.putIfAbsent(chronique.produitId(), chronique.produitLibelle());
            } else if (chronique.dciId() != null) {
                dciSeules.add(chronique.dciId());
            }
        }

        List<TraitementEnCoursDTO> resultat = new ArrayList<>();
        moleculesProduitService
            .molecules(produits.keySet())
            .forEach((produitId, molecules) -> molecules.forEach(m -> resultat.add(new TraitementEnCoursDTO(produitId, produits.get(produitId), m))));
        if (!dciSeules.isEmpty()) {
            for (MoleculeDTO molecule : refDciRepository.findByDciIds(dciSeules)) {
                resultat.add(new TraitementEnCoursDTO(null, null, molecule));
            }
        }
        return resultat;
    }
}
