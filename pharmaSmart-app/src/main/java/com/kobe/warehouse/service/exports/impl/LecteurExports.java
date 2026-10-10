package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.ExportDonneesRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** Le flux de lignes de chaque export ; le CA de l'officine a la même définition que le pilotage. */
@Component
class LecteurExports {

    private static final List<OrderStatut> ACHATS_RECUS = List.of(OrderStatut.RECEIVED, OrderStatut.CLOSED);

    private final ExportDonneesRepository exportDonneesRepository;

    LecteurExports(ExportDonneesRepository exportDonneesRepository) {
        this.exportDonneesRepository = exportDonneesRepository;
    }

    /** À appeler dans une transaction en lecture seule ; le flux doit être fermé. */
    Stream<Object[]> lire(ExportDonnees export, LocalDate du, LocalDate au) {
        var officine = CategorieChiffreAffaire.officine();
        return switch (export) {
            case VENTES -> exportDonneesRepository.lireVentes(du, au, SalesStatut.CLOSED, officine);
            case LIGNES_VENTE -> exportDonneesRepository.lireLignesVente(du, au, SalesStatut.CLOSED, officine);
            case ENCAISSEMENTS -> exportDonneesRepository.lireEncaissements(du, au);
            case ACHATS -> exportDonneesRepository.lireAchats(du, au, ACHATS_RECUS);
            case MOUVEMENTS_STOCK -> exportDonneesRepository.lireMouvementsStock(du, au);
            case PRODUITS -> exportDonneesRepository.lireProduits();
            case CLIENTS -> exportDonneesRepository.lireClients();
            case VENTES_JOUR_PRODUIT -> exportDonneesRepository.lireVentesJourProduit(du, au, officine);
        };
    }
}
