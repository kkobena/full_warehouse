package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.AchatsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AchatsVentesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.CaissierEncaissementDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ClientsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.DifferesTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.ElementAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.EncaissementsTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.EquipePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneAchatsDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneAchatsVentesDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneCroiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneStockDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneVendeurDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ModeEncaissementDTO;
import com.kobe.warehouse.service.dto.pilotage.OrganismeTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.PeremptionMoisDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.PointAchatsVentesDTO;
import com.kobe.warehouse.service.dto.pilotage.PointStockDTO;
import com.kobe.warehouse.service.dto.pilotage.ProduitDormantDTO;
import com.kobe.warehouse.service.dto.pilotage.RupturesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieModeDTO;
import com.kobe.warehouse.service.dto.pilotage.StockPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.TiersPayantTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.TrancheMontantDTO;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Données d'écran des onglets Achats & stock, Trésorerie et Clients & équipe, pour les tests du rapport PDF. */
final class JeuRapportPilotage {

    static final ComparaisonPeriodesDTO COMPARAISON = new ComparaisonPeriodesDTO(
        new PeriodeDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
        new PeriodeDTO(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30)),
        false
    );
    static final CelluleAnalyseDTO CELLULE = new CelluleAnalyseDTO(100_000d, 80_000d, 20_000d, 25.0);

    private JeuRapportPilotage() {}

    static AchatsPilotageDTO lireAchats() {
        return new AchatsPilotageDTO(
            COMPARAISON,
            CELLULE,
            CELLULE,
            CELLULE,
            CELLULE,
            CELLULE,
            List.of(new LigneAchatsDTO("1", "Laborex", CELLULE, 60.0, 12L, 2.5, 98.0)),
            List.of(new LigneAchatsDTO("2", "Antalgiques", CELLULE, 40.0, null, null, null))
        );
    }

    static AchatsVentesPilotageDTO lireAchatsVentes() {
        return new AchatsVentesPilotageDTO(
            COMPARAISON,
            List.of(
                new PointAchatsVentesDTO(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "Août 2026", 100_000, 90_000),
                new PointAchatsVentesDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "Sept. 2026", 120_000, 95_000)
            ),
            List.of(new LigneAchatsVentesDTO("2", "Antalgiques", 100_000, 90_000, 10_000, 0.9))
        );
    }

    static StockPilotageDTO lireStock() {
        return new StockPilotageDTO(
            LocalDate.of(2026, 9, 30),
            CELLULE,
            4.2,
            87.0,
            3,
            45_000,
            90,
            List.of(
                new PointStockDTO(LocalDate.of(2026, 8, 31), 1_000_000L, 900_000L),
                new PointStockDTO(LocalDate.of(2026, 9, 30), 1_100_000L, null)
            ),
            List.of(new LigneStockDTO("2", "Antalgiques", 500_000, 45.0, 2_000_000, 4.0, 91.0, 10_000)),
            List.of(new ProduitDormantDTO("p", "Sirop Toux Sèche", 3L, 4_500L, null))
        );
    }

    /** Aucune rupture par produit : ce tableau-là n'est pas imprimé. */
    static RupturesPilotageDTO lireRuptures() {
        return new RupturesPilotageDTO(
            COMPARAISON,
            CELLULE,
            CELLULE,
            CELLULE,
            CELLULE,
            25_000,
            List.of(new ComptageDTO("f", "Laborex", 4L, 40L, null)),
            List.of(),
            List.of(new ComptageDTO("p", "Amoxicilline 500", 2L, 6L, 9_000L)),
            List.of(new PeremptionMoisDTO(2026, 12, 10L, 15_000L))
        );
    }

    static EncaissementsTresorerieDTO lireEncaissements() {
        return new EncaissementsTresorerieDTO(
            COMPARAISON,
            CELLULE,
            List.of(new ModeEncaissementDTO("CASH", "Espèces", CELLULE, 70.0), new ModeEncaissementDTO("CB", "Carte", CELLULE, 30.0)),
            List.of("Août 2026", "Sept. 2026"),
            List.of(new SerieModeDTO("Espèces", List.of(100_000L, 120_000L)), new SerieModeDTO("Carte", List.of(40_000L, 50_000L))),
            List.of(new CaissierEncaissementDTO("1", "Awa", 170_000, 120, 20))
        );
    }

    static TiersPayantTresorerieDTO lireTiersPayant() {
        return new TiersPayantTresorerieDTO(
            COMPARAISON,
            CELLULE,
            CELLULE,
            2_500_000,
            45,
            List.of(new OrganismeTresorerieDTO("1", "MUGEF-CI", 1_000_000, 800_000, 80.0, 1_200_000, 48.0, 52, 300_000, 60, "OBSERVE")),
            List.of(new TrancheMontantDTO("0-30 j", 1_000_000), new TrancheMontantDTO("31-60 j", 800_000)),
            List.of(new TrancheMontantDTO("En retard", 300_000), new TrancheMontantDTO("Nov. 2026", 900_000)),
            72.0,
            85.0,
            3
        );
    }

    static DifferesTresorerieDTO lireDifferes() {
        return new DifferesTresorerieDTO(
            150_000,
            List.of(new TrancheMontantDTO("Moins de 30 j", 100_000)),
            List.of(new ComptageDTO("c", "Kouassi Yao", 3L, null, 90_000L)),
            CELLULE,
            CELLULE
        );
    }

    /** Mardi 10 h, le créneau le plus chargé. */
    static AnalysePilotageDTO lireAnalyseCroisee() {
        CroiseAnalyseDTO croise = new CroiseAnalyseDTO(
            List.of(new MembreAnalyseDTO("1", "Lundi"), new MembreAnalyseDTO("2", "Mardi")),
            List.of(
                new LigneCroiseeDTO("9", "9 h", List.of(new CelluleAnalyseDTO(12d, null, null, null), new CelluleAnalyseDTO(0d, null, null, null))),
                new LigneCroiseeDTO("10", "10 h", List.of(new CelluleAnalyseDTO(30d, null, null, null), new CelluleAnalyseDTO(42d, null, null, null)))
            )
        );
        IndicateurPilotageDTO indicateur = IndicateurPilotageDTO.fromIndicateur(IndicateurPilotage.NB_VENTES);
        return new AnalysePilotageDTO(
            COMPARAISON,
            SourceAnalyse.ENTETES,
            List.of(indicateur),
            List.of(),
            AxeAnalyseDTO.fromAxe(AxeAnalyse.HEURE),
            AxeAnalyseDTO.fromAxe(AxeAnalyse.JOUR_SEMAINE),
            List.of(),
            List.of(),
            null,
            0,
            croise
        );
    }

    /** Deux produits et « autres » sur CA TTC et taux de marge ; part et contribution sur le CA. */
    static AnalysePilotageDTO lireAnalyseVentilee() {
        CelluleAnalyseDTO taux = new CelluleAnalyseDTO(30.0, 29.0, 1.0, 3.4);
        CroiseAnalyseDTO dansLeTemps = new CroiseAnalyseDTO(
            List.of(new MembreAnalyseDTO("2026-08", "Août 2026"), new MembreAnalyseDTO("2026-09", "Sept. 2026")),
            List.of(new LigneCroiseeDTO("p1", "Doliprane 1000", List.of(CELLULE, CELLULE)))
        );
        return new AnalysePilotageDTO(
            COMPARAISON,
            SourceAnalyse.LIGNES,
            List.of(IndicateurPilotageDTO.fromIndicateur(IndicateurPilotage.CA_TTC), IndicateurPilotageDTO.fromIndicateur(IndicateurPilotage.TAUX_MARGE)),
            List.of(),
            AxeAnalyseDTO.fromAxe(AxeAnalyse.PRODUIT),
            null,
            List.of(CELLULE, taux),
            List.of(
                new ElementAnalyseDTO("p1", "Doliprane 1000", List.of(CELLULE, taux), 55.0, 60.0),
                new ElementAnalyseDTO("p2", "Efferalgan 500", List.of(CELLULE, taux), 30.0, 25.0)
            ),
            new ElementAnalyseDTO(null, "Autres", List.of(CELLULE, taux), 15.0, 15.0),
            3,
            dansLeTemps
        );
    }

    static ClientsPilotageDTO lireClients() {
        return new ClientsPilotageDTO(
            COMPARAISON,
            CELLULE,
            CELLULE,
            4L,
            2L,
            CELLULE,
            CELLULE,
            List.of(new ContributionDTO(null, "Client", "k", "Kouassi Yao", 10_000, 30_000, -20_000))
        );
    }

    static EquipePilotageDTO lireEquipe() {
        List<CelluleAnalyseDTO> mesures = Collections.nCopies(9, CELLULE);
        return new EquipePilotageDTO(COMPARAISON, List.of(lireVendeur("1", "Awa", mesures)), lireVendeur("equipe", "Équipe entière", mesures));
    }

    private static LigneVendeurDTO lireVendeur(String cle, String libelle, List<CelluleAnalyseDTO> m) {
        return new LigneVendeurDTO(cle, libelle, m.get(0), m.get(1), m.get(2), m.get(3), m.get(4), m.get(5), m.get(6), m.get(7), m.get(8));
    }
}
