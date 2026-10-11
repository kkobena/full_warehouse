package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ClientsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.EquipePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneCroiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneVendeurDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.ClientsEquipeRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO.CelluleRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TuileRapportDTO;
import com.kobe.warehouse.service.pilotage.AnalysePilotageService;
import com.kobe.warehouse.service.pilotage.ClientsEquipePilotageService;
import com.kobe.warehouse.service.pilotage.RapportClientsEquipeService;
import com.kobe.warehouse.service.pilotage.RapportCourbesService;
import com.kobe.warehouse.service.pilotage.calcul.CartesChaleur;
import com.kobe.warehouse.service.pilotage.calcul.ColonneVendeur;
import com.kobe.warehouse.service.pilotage.calcul.Creneau;
import com.kobe.warehouse.service.pilotage.calcul.FormatRapport;
import com.kobe.warehouse.service.pilotage.calcul.TableauxRapport;
import com.kobe.warehouse.service.pilotage.calcul.TuilesRapport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RapportClientsEquipeServiceImpl implements RapportClientsEquipeService {

    private static final int CRENEAUX = 3;
    private static final int LIGNES_MAXIMUM = 30;
    private static final RequeteAnalyseDTO HEURES_PAR_JOUR = new RequeteAnalyseDTO(AxeAnalyse.HEURE, AxeAnalyse.JOUR_SEMAINE, 0, null, null);
    private static final RequeteAnalyseDTO VENDEURS_PAR_HEURE = new RequeteAnalyseDTO(AxeAnalyse.VENDEUR, AxeAnalyse.HEURE, 0, null, null);
    private static final List<String> COLONNES_EN_BAISSE = List.of("CA", "CA de référence", "Écart");
    /** Mêmes colonnes que l'écran : libellé, unité, sens favorable, lecture de la mesure du vendeur. */
    private static final List<ColonneVendeur> COLONNES_VENDEURS = List.of(
        new ColonneVendeur("Ventes", UniteIndicateur.NOMBRE, SensFavorable.HAUSSE, LigneVendeurDTO::nbVentes),
        new ColonneVendeur("CA TTC", UniteIndicateur.MONTANT, SensFavorable.HAUSSE, LigneVendeurDTO::caTtc),
        new ColonneVendeur("Panier moyen", UniteIndicateur.MONTANT, SensFavorable.HAUSSE, LigneVendeurDTO::panierMoyen),
        new ColonneVendeur("Articles par vente", UniteIndicateur.RATIO, SensFavorable.HAUSSE, LigneVendeurDTO::articlesParVente),
        new ColonneVendeur("Taux de marge", UniteIndicateur.POURCENTAGE, SensFavorable.HAUSSE, LigneVendeurDTO::tauxMarge),
        new ColonneVendeur("Taux de remise", UniteIndicateur.POURCENTAGE, SensFavorable.BAISSE, LigneVendeurDTO::tauxRemise),
        new ColonneVendeur("Annulations", UniteIndicateur.NOMBRE, SensFavorable.BAISSE, LigneVendeurDTO::annulations),
        new ColonneVendeur("Avoirs", UniteIndicateur.NOMBRE, SensFavorable.BAISSE, LigneVendeurDTO::avoirs),
        new ColonneVendeur("Part ordonnance", UniteIndicateur.POURCENTAGE, SensFavorable.NEUTRE, LigneVendeurDTO::partOrdonnance)
    );

    private final ClientsEquipePilotageService clientsEquipePilotageService;
    private final AnalysePilotageService analysePilotageService;
    private final RapportCourbesService rapportCourbesService;

    public RapportClientsEquipeServiceImpl(
        ClientsEquipePilotageService clientsEquipePilotageService,
        AnalysePilotageService analysePilotageService,
        RapportCourbesService rapportCourbesService
    ) {
        this.clientsEquipePilotageService = clientsEquipePilotageService;
        this.analysePilotageService = analysePilotageService;
        this.rapportCourbesService = rapportCourbesService;
    }

    @Override
    public ClientsEquipeRapportDTO construireClientsEquipe(RequetePilotageDTO requete) {
        CroiseAnalyseDTO frequentation = analysePilotageService
            .analyser(requete.lireAvecIndicateurs(List.of(IndicateurPilotage.NB_VENTES)), HEURES_PAR_JOUR)
            .croise();
        AnalysePilotageDTO parHeure = analysePilotageService.analyser(requete.lireAvecIndicateurs(List.of(IndicateurPilotage.CA_TTC)), VENDEURS_PAR_HEURE);
        ClientsPilotageDTO clients = clientsEquipePilotageService.analyserClients(requete);
        EquipePilotageDTO equipe = clientsEquipePilotageService.analyserEquipe(requete);
        RequetePilotageDTO parJour = new RequetePilotageDTO(
            requete.du(),
            requete.au(),
            requete.comparaison(),
            requete.anneesEnArriere(),
            requete.referenceDu(),
            requete.referenceAu(),
            requete.aDate(),
            Granularite.JOUR,
            requete.indicateurs()
        );
        return new ClientsEquipeRapportDTO(
            lireCreneaux(frequentation),
            estVide(frequentation) ? null : CartesChaleur.lireCarte("Heure", frequentation, UniteIndicateur.NOMBRE),
            rapportCourbesService.dessinerPeriode(parJour, IndicateurPilotage.NB_VENTES, FormatGraphe.MOITIE),
            lireTuilesClients(clients),
            clients.enBaisse().isEmpty() ? null : TableauxRapport.limiter(construireEnBaisse(clients.enBaisse()), LIGNES_MAXIMUM),
            construireVendeurs(equipe),
            estVide(parHeure.croise()) ? null : TableauxRapport.lireCroise("Vendeur / Heure", parHeure.croise(), parHeure.indicateurs().getFirst(), false)
        );
    }

    private static boolean estVide(CroiseAnalyseDTO croise) {
        return croise == null || croise.lignes().isEmpty();
    }

    /** « Créneaux les plus chargés : mardi 10 h (42 ventes), … » ; {@code null} sans vente. */
    private static String lireCreneaux(CroiseAnalyseDTO croise) {
        if (estVide(croise)) {
            return null;
        }
        List<Creneau> creneaux = new ArrayList<>();
        for (LigneCroiseeDTO ligne : croise.lignes()) {
            for (int rang = 0; rang < ligne.cellules().size(); rang++) {
                CelluleAnalyseDTO cellule = ligne.cellules().get(rang);
                if (cellule != null && cellule.valeur() != null && cellule.valeur() > 0) {
                    creneaux.add(new Creneau(croise.colonnes().get(rang).libelle().toLowerCase() + " " + ligne.libelle(), cellule.valeur()));
                }
            }
        }
        List<String> plusCharges = creneaux
            .stream()
            .sorted(Comparator.comparingDouble(Creneau::valeur).reversed())
            .limit(CRENEAUX)
            .map(creneau -> creneau.libelle() + " (" + FormatRapport.formaterValeur(creneau.valeur(), UniteIndicateur.NOMBRE) + " ventes)")
            .toList();
        return plusCharges.isEmpty() ? null : "Créneaux les plus chargés : " + String.join(", ", plusCharges) + ".";
    }

    /** Revenus et perdus n'existent qu'avec une référence. */
    private static List<TuileRapportDTO> lireTuilesClients(ClientsPilotageDTO clients) {
        List<TuileRapportDTO> tuiles = new ArrayList<>(6);
        tuiles.add(TuilesRapport.lireTuile("Clients actifs", clients.actifs(), UniteIndicateur.NOMBRE, SensFavorable.HAUSSE));
        tuiles.add(TuilesRapport.lireTuile("Nouveaux", clients.nouveaux(), UniteIndicateur.NOMBRE, SensFavorable.HAUSSE));
        if (clients.revenus() != null && clients.perdus() != null) {
            tuiles.add(TuilesRapport.lireTuile("Revenus", lireSeule(clients.revenus()), UniteIndicateur.NOMBRE, SensFavorable.HAUSSE));
            tuiles.add(TuilesRapport.lireTuile("Perdus", lireSeule(clients.perdus()), UniteIndicateur.NOMBRE, SensFavorable.BAISSE));
        }
        tuiles.add(TuilesRapport.lireTuile("CA avec un client identifié", clients.partCaIdentifie(), UniteIndicateur.POURCENTAGE, SensFavorable.HAUSSE));
        tuiles.add(TuilesRapport.lireTuile("CA moyen par client", clients.caMoyenParClient(), UniteIndicateur.MONTANT, SensFavorable.HAUSSE));
        return tuiles;
    }

    private static TableauRapportDTO construireEnBaisse(List<ContributionDTO> enBaisse) {
        List<LigneRapportDTO> lignes = new ArrayList<>(enBaisse.size());
        for (ContributionDTO client : enBaisse) {
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireValeur(client.valeur(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(client.valeurReference(), UniteIndicateur.MONTANT),
                TableauxRapport.lireValeur(client.ecart(), UniteIndicateur.MONTANT)
            );
            lignes.add(new LigneRapportDTO(client.libelle(), cellules));
        }
        return new TableauRapportDTO("Client", COLONNES_EN_BAISSE, lignes);
    }

    /** Chaque vendeur face à la référence ; la dernière ligne situe l'équipe entière, sans variation. */
    private static TableauRapportDTO construireVendeurs(EquipePilotageDTO equipe) {
        List<LigneRapportDTO> lignes = new ArrayList<>(equipe.vendeurs().size() + 1);
        for (LigneVendeurDTO vendeur : equipe.vendeurs()) {
            List<CelluleRapportDTO> cellules = new ArrayList<>(COLONNES_VENDEURS.size());
            for (ColonneVendeur colonne : COLONNES_VENDEURS) {
                cellules.add(TableauxRapport.lireCellule(colonne.lecture().apply(vendeur), colonne.unite(), colonne.sens()));
            }
            lignes.add(new LigneRapportDTO(vendeur.libelle(), cellules));
        }
        if (!equipe.vendeurs().isEmpty() && equipe.equipe() != null) {
            List<CelluleRapportDTO> totaux = new ArrayList<>(COLONNES_VENDEURS.size());
            for (ColonneVendeur colonne : COLONNES_VENDEURS) {
                CelluleAnalyseDTO cellule = colonne.lecture().apply(equipe.equipe());
                totaux.add(TableauxRapport.lireValeur(cellule == null ? null : cellule.valeur(), colonne.unite()));
            }
            lignes.add(new LigneRapportDTO(equipe.equipe().libelle(), totaux, true));
        }
        return new TableauRapportDTO("Vendeur", COLONNES_VENDEURS.stream().map(ColonneVendeur::libelle).toList(), lignes);
    }

    private static CelluleAnalyseDTO lireSeule(Long valeur) {
        return new CelluleAnalyseDTO(valeur == null ? null : valeur.doubleValue(), null, null, null);
    }
}
