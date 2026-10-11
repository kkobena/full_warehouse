package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.AchatsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AchatsVentesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneAchatsDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneAchatsVentesDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneStockDTO;
import com.kobe.warehouse.service.dto.pilotage.PeremptionMoisDTO;
import com.kobe.warehouse.service.dto.pilotage.PointAchatsVentesDTO;
import com.kobe.warehouse.service.dto.pilotage.PointStockDTO;
import com.kobe.warehouse.service.dto.pilotage.ProduitDormantDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RupturesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.StockPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.AchatsStockRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO.CelluleRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO.StyleSerie;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TuileRapportDTO;
import com.kobe.warehouse.service.pilotage.AchatsPilotageService;
import com.kobe.warehouse.service.pilotage.GraphiquePilotageService;
import com.kobe.warehouse.service.pilotage.RapportAchatsStockService;
import com.kobe.warehouse.service.pilotage.RapportCourbesService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.service.pilotage.StockPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.FormatRapport;
import com.kobe.warehouse.service.pilotage.calcul.HorizonCourbe;
import com.kobe.warehouse.service.pilotage.calcul.TableauxRapport;
import com.kobe.warehouse.service.pilotage.calcul.TuilesRapport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RapportAchatsStockServiceImpl implements RapportAchatsStockService {

    private static final int LIGNES_MAXIMUM = 30;
    private static final DateTimeFormatter MOIS_COURT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter MOIS_LONG = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final List<String> COLONNES_ACHATS_FAMILLE = List.of("Achats TTC", "Part");
    private static final List<String> COLONNES_ACHATS_FOURNISSEUR = List.of("Achats TTC", "Part", "Bons", "Délai", "Conformité");
    private static final List<String> COLONNES_ACHATS_VENTES = List.of("Achats HT", "Coût des ventes HT", "Écart", "Ratio");
    private static final List<String> COLONNES_DORMANTS = List.of("Quantité", "Valeur", "Dernière vente");
    private static final List<String> COLONNES_STOCK = List.of(
        "Valeur",
        "Part",
        "Coût des ventes 12 mois",
        "Rotation",
        "Couverture (j)",
        "Dormant"
    );
    private static final List<String> COLONNES_RUPTURES = List.of("Ruptures", "Quantité");
    private static final List<String> COLONNES_VENTES_MANQUEES = List.of("Avoirs", "Quantité", "Valeur");
    private static final List<String> COLONNES_PEREMPTIONS = List.of("Quantité", "Valeur");

    private final AchatsPilotageService achatsPilotageService;
    private final StockPilotageService stockPilotageService;
    private final SeriesPilotageService seriesPilotageService;
    private final RapportCourbesService rapportCourbesService;
    private final GraphiquePilotageService graphiquePilotageService;

    public RapportAchatsStockServiceImpl(
        AchatsPilotageService achatsPilotageService,
        StockPilotageService stockPilotageService,
        SeriesPilotageService seriesPilotageService,
        RapportCourbesService rapportCourbesService,
        GraphiquePilotageService graphiquePilotageService
    ) {
        this.achatsPilotageService = achatsPilotageService;
        this.stockPilotageService = stockPilotageService;
        this.seriesPilotageService = seriesPilotageService;
        this.rapportCourbesService = rapportCourbesService;
        this.graphiquePilotageService = graphiquePilotageService;
    }

    @Override
    public AchatsStockRapportDTO construireAchatsStock(RequetePilotageDTO requete) {
        AchatsPilotageDTO achats = achatsPilotageService.analyserAchats(requete);
        AchatsVentesPilotageDTO achatsVentes = achatsPilotageService.analyserAchatsVentes(requete);
        StockPilotageDTO stock = stockPilotageService.analyserStock(requete);
        RupturesPilotageDTO ruptures = stockPilotageService.analyserRuptures(requete);
        String horizon = HorizonCourbe.libellerHorizon(requete.granularite());
        return new AchatsStockRapportDTO(
            lireTuilesAchats(achats),
            "Achats TTC — " + horizon,
            rapportCourbesService.dessinerHistorique(requete, IndicateurPilotage.ACHATS_TTC, false, FormatGraphe.MOITIE),
            TableauxRapport.limiter(construireAchats("Famille", achats.familles(), false), LIGNES_MAXIMUM),
            TableauxRapport.limiter(construireAchats("Fournisseur", achats.fournisseurs(), true), LIGNES_MAXIMUM),
            lireTuilesAchatsVentes(requete),
            "Achats et coût des ventes — " + horizon,
            dessinerAchatsVentes(requete),
            TableauxRapport.limiter(construireAchatsVentes(achatsVentes.familles()), LIGNES_MAXIMUM),
            lireTuilesStock(stock),
            "Fin " + stock.mois().format(MOIS_LONG) + " ; valeur au prix d'achat du dernier mouvement, comme la valorisation du stock.",
            dessinerStock(stock.courbe()),
            "Produits dormants (sans vente depuis " + stock.seuilDormant() + " jours)",
            TableauxRapport.limiter(construireDormants(stock.dormants()), LIGNES_MAXIMUM),
            TableauxRapport.limiter(construireStock(stock.familles()), LIGNES_MAXIMUM),
            lireTuilesRuptures(ruptures),
            construireComptages("Fournisseur", ruptures.rupturesParFournisseur(), false),
            construireComptages("Produit", ruptures.rupturesParProduit(), false),
            construireComptages("Produit", ruptures.ventesManqueesParProduit(), true),
            construirePeremptions(ruptures.peremptionsAVenir())
        );
    }

    private static List<TuileRapportDTO> lireTuilesAchats(AchatsPilotageDTO achats) {
        return List.of(
            TuilesRapport.lireTuile("Achats TTC", achats.achatsTtc(), UniteIndicateur.MONTANT, SensFavorable.NEUTRE),
            TuilesRapport.lireTuile("Achats HT", achats.achatsHt(), UniteIndicateur.MONTANT, SensFavorable.NEUTRE),
            TuilesRapport.lireTuile("Bons reçus", achats.nbBons(), UniteIndicateur.NOMBRE, SensFavorable.NEUTRE),
            TuilesRapport.lireTuile("Délai de livraison (j)", achats.delaiMoyen(), UniteIndicateur.JOURS, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Conformité", achats.conformite(), UniteIndicateur.POURCENTAGE, SensFavorable.HAUSSE)
        );
    }

    /** Délai et conformité ne valent que pour un fournisseur. */
    private static TableauRapportDTO construireAchats(String entete, List<LigneAchatsDTO> achats, boolean parFournisseur) {
        List<LigneRapportDTO> lignes = new ArrayList<>(achats.size());
        for (LigneAchatsDTO ligne : achats) {
            List<CelluleRapportDTO> cellules = new ArrayList<>(5);
            cellules.add(TableauxRapport.lireCellule(ligne.montant(), UniteIndicateur.MONTANT, SensFavorable.NEUTRE));
            cellules.add(TableauxRapport.lireValeur(ligne.part(), UniteIndicateur.POURCENTAGE));
            if (parFournisseur) {
                cellules.add(TableauxRapport.lireValeur(ligne.nbBons(), UniteIndicateur.NOMBRE));
                cellules.add(TableauxRapport.lireValeur(ligne.delaiMoyen(), UniteIndicateur.JOURS));
                cellules.add(TableauxRapport.lireValeur(ligne.conformite(), UniteIndicateur.POURCENTAGE));
            }
            lignes.add(new LigneRapportDTO(ligne.libelle(), cellules));
        }
        return new TableauRapportDTO(entete, parFournisseur ? COLONNES_ACHATS_FOURNISSEUR : COLONNES_ACHATS_FAMILLE, lignes);
    }

    /** Le ratio de la période face à sa référence, puis sur les 12 mois qui se terminent à la fin de la période, sans comparaison. */
    private List<TuileRapportDTO> lireTuilesAchatsVentes(RequetePilotageDTO requete) {
        List<IndicateurPilotage> ratio = List.of(IndicateurPilotage.RATIO_VENTES_ACHATS);
        List<TuileRapportDTO> tuiles = new ArrayList<>(2);
        List<SerieIndicateurDTO> periode = seriesPilotageService.calculerSeries(requete.lireAvecIndicateurs(ratio)).series();
        if (!periode.isEmpty()) {
            SerieIndicateurDTO serie = periode.getFirst();
            CelluleAnalyseDTO cellule = new CelluleAnalyseDTO(serie.valeur(), serie.valeurReference(), serie.ecart(), serie.ecartPct());
            tuiles.add(TuilesRapport.lireTuile("Ventes / achats", cellule, UniteIndicateur.RATIO, SensFavorable.NEUTRE));
        }
        RequetePilotageDTO douzeMois = new RequetePilotageDTO(
            requete.au().minusYears(1).plusDays(1),
            requete.au(),
            TypeComparaison.AUCUNE,
            null,
            null,
            null,
            false,
            requete.granularite(),
            ratio
        );
        List<SerieIndicateurDTO> glissant = seriesPilotageService.calculerSeries(douzeMois).series();
        if (!glissant.isEmpty()) {
            CelluleAnalyseDTO cellule = new CelluleAnalyseDTO(glissant.getFirst().valeur(), null, null, null);
            tuiles.add(TuilesRapport.lireTuile("Ventes / achats sur 12 mois", cellule, UniteIndicateur.RATIO, SensFavorable.NEUTRE));
        }
        return tuiles;
    }

    /** Un écart durable entre les deux courbes, c'est du stock qui gonfle (achats au-dessus) ou qui fond. */
    private String dessinerAchatsVentes(RequetePilotageDTO requete) {
        RequetePilotageDTO historique = HorizonCourbe.elargirAuxTreizeTranches(requete, IndicateurPilotage.ACHATS_TTC);
        List<PointAchatsVentesDTO> points = achatsPilotageService.analyserAchatsVentes(historique).points();
        if (points.isEmpty()) {
            return null;
        }
        List<String> libelles = new ArrayList<>(points.size());
        List<Double> achats = new ArrayList<>(points.size());
        List<Double> coutVentes = new ArrayList<>(points.size());
        for (PointAchatsVentesDTO point : points) {
            libelles.add(point.libelle());
            achats.add((double) point.achatsHt());
            coutVentes.add((double) point.coutVentesHt());
        }
        List<SerieGraphiqueDTO> series = List.of(
            new SerieGraphiqueDTO("Achats HT", achats, StyleSerie.PALETTE),
            new SerieGraphiqueDTO("Coût des ventes HT", coutVentes, StyleSerie.PALETTE)
        );
        return graphiquePilotageService.dessinerCourbes(libelles, series, UniteIndicateur.MONTANT, FormatGraphe.MOITIE);
    }

    private static TableauRapportDTO construireAchatsVentes(List<LigneAchatsVentesDTO> familles) {
        List<LigneRapportDTO> lignes = new ArrayList<>(familles.size());
        for (LigneAchatsVentesDTO ligne : familles) {
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireValeur(ligne.achatsHt(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(ligne.coutVentesHt(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(ligne.ecart(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(ligne.ratio(), UniteIndicateur.RATIO)
            );
            lignes.add(new LigneRapportDTO(ligne.libelle(), cellules));
        }
        return new TableauRapportDTO("Famille", COLONNES_ACHATS_VENTES, lignes);
    }

    private static List<TuileRapportDTO> lireTuilesStock(StockPilotageDTO stock) {
        return List.of(
            TuilesRapport.lireTuile("Valeur du stock", stock.valeur(), UniteIndicateur.MONTANT, SensFavorable.NEUTRE),
            TuilesRapport.lireTuile("Rotation", lireSeule(stock.rotation()), UniteIndicateur.RATIO, SensFavorable.HAUSSE),
            TuilesRapport.lireTuile("Couverture (j)", lireSeule(stock.couvertureJours()), UniteIndicateur.JOURS, SensFavorable.BAISSE),
            TuilesRapport.lireTuile(
                "Stock dormant (" + stock.nbDormants() + " produits)",
                lireSeule((double) stock.valeurDormante()),
                UniteIndicateur.MONTANT,
                SensFavorable.BAISSE
            )
        );
    }

    private String dessinerStock(List<PointStockDTO> courbe) {
        if (courbe.isEmpty()) {
            return null;
        }
        List<String> libelles = new ArrayList<>(courbe.size());
        List<Double> valeurs = new ArrayList<>(courbe.size());
        List<Double> valeursN1 = new ArrayList<>(courbe.size());
        for (PointStockDTO point : courbe) {
            libelles.add(point.mois().format(MOIS_COURT));
            valeurs.add(point.valeur() == null ? null : point.valeur().doubleValue());
            valeursN1.add(point.valeurN1() == null ? null : point.valeurN1().doubleValue());
        }
        List<SerieGraphiqueDTO> series = List.of(
            new SerieGraphiqueDTO("Stock", valeurs, StyleSerie.REALISE),
            new SerieGraphiqueDTO("Stock N-1", valeursN1, StyleSerie.REFERENCE)
        );
        return graphiquePilotageService.dessinerCourbes(libelles, series, UniteIndicateur.MONTANT, FormatGraphe.MOITIE);
    }

    private static TableauRapportDTO construireDormants(List<ProduitDormantDTO> dormants) {
        List<LigneRapportDTO> lignes = new ArrayList<>(dormants.size());
        for (ProduitDormantDTO produit : dormants) {
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireValeur(produit.quantite(), UniteIndicateur.NOMBRE),
                TableauxRapport.lireValeur(produit.valeur(), UniteIndicateur.MONTANT),
                CelluleRapportDTO.lireTexte(produit.derniereVente() == null ? "Jamais" : FormatRapport.formaterDate(produit.derniereVente()))
            );
            lignes.add(new LigneRapportDTO(produit.libelle(), cellules));
        }
        return new TableauRapportDTO("Produit", COLONNES_DORMANTS, lignes);
    }

    private static TableauRapportDTO construireStock(List<LigneStockDTO> familles) {
        List<LigneRapportDTO> lignes = new ArrayList<>(familles.size());
        for (LigneStockDTO ligne : familles) {
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireValeur(ligne.valeur(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(ligne.part(), UniteIndicateur.POURCENTAGE),
                TableauxRapport.lireValeur(ligne.coutVentes12Mois(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(ligne.rotation(), UniteIndicateur.RATIO),
                TableauxRapport.lireValeur(ligne.couvertureJours(), UniteIndicateur.JOURS),
                TableauxRapport.lireValeur(ligne.valeurDormante(), UniteIndicateur.MONTANT)
            );
            lignes.add(new LigneRapportDTO(ligne.libelle(), cellules));
        }
        return new TableauRapportDTO("Famille", COLONNES_STOCK, lignes);
    }

    /** Deux ruptures distinctes, jamais additionnées : chez le fournisseur et au comptoir (ventes manquées). */
    private static List<TuileRapportDTO> lireTuilesRuptures(RupturesPilotageDTO ruptures) {
        return List.of(
            TuilesRapport.lireTuile("Ruptures fournisseurs", ruptures.tauxRupture(), UniteIndicateur.POURCENTAGE, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Ventes manquées", ruptures.ventesManquees(), UniteIndicateur.MONTANT, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Taux de ventes manquées", ruptures.tauxVentesManquees(), UniteIndicateur.POURCENTAGE, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Périmé sur la période", ruptures.valeurPerimee(), UniteIndicateur.MONTANT, SensFavorable.BAISSE),
            TuilesRapport.lireTuile(
                "À périmer sous 3 mois",
                lireSeule((double) ruptures.valeurAPerimerTroisMois()),
                UniteIndicateur.MONTANT,
                SensFavorable.BAISSE
            )
        );
    }

    /** Vide, le tableau n'est pas imprimé, comme l'écran le masque. */
    private static TableauRapportDTO construireComptages(String entete, List<ComptageDTO> comptages, boolean avecValeur) {
        if (comptages.isEmpty()) {
            return null;
        }
        List<LigneRapportDTO> lignes = new ArrayList<>(comptages.size());
        for (ComptageDTO comptage : comptages) {
            List<CelluleRapportDTO> cellules = new ArrayList<>(3);
            cellules.add(TableauxRapport.lireValeur(comptage.nombre(), UniteIndicateur.NOMBRE));
            cellules.add(TableauxRapport.lireValeur(comptage.quantite(), UniteIndicateur.NOMBRE));
            if (avecValeur) {
                cellules.add(TableauxRapport.lireValeur(comptage.montant(), UniteIndicateur.MONTANT));
            }
            lignes.add(new LigneRapportDTO(comptage.libelle(), cellules));
        }
        TableauRapportDTO tableau = new TableauRapportDTO(entete, avecValeur ? COLONNES_VENTES_MANQUEES : COLONNES_RUPTURES, lignes);
        return TableauxRapport.limiter(tableau, LIGNES_MAXIMUM);
    }

    private static TableauRapportDTO construirePeremptions(List<PeremptionMoisDTO> peremptions) {
        if (peremptions.isEmpty()) {
            return null;
        }
        List<LigneRapportDTO> lignes = new ArrayList<>(peremptions.size());
        for (PeremptionMoisDTO peremption : peremptions) {
            String mois = LocalDate.of(peremption.annee(), peremption.mois(), 1).format(MOIS_LONG);
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireValeur(peremption.quantite(), UniteIndicateur.NOMBRE),
                TableauxRapport.lireValeur(peremption.valeur(), UniteIndicateur.MONTANT)
            );
            lignes.add(new LigneRapportDTO(mois, cellules));
        }
        return new TableauRapportDTO("Mois", COLONNES_PEREMPTIONS, lignes);
    }

    private static CelluleAnalyseDTO lireSeule(Double valeur) {
        return new CelluleAnalyseDTO(valeur, null, null, null);
    }
}
