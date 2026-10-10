package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.repository.PilotageClientsRepository;
import com.kobe.warehouse.service.dto.pilotage.ClienteleDTO;
import com.kobe.warehouse.service.dto.pilotage.ClientsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.EquipePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneVendeurDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.pilotage.ClientsEquipePilotageService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.MesuresVendeur;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clients (identifiés à la vente) et équipe (vendeur de la vente). Les mesures de l'équipe viennent des agrégats : en-têtes
 * pour les ventes, le CA, les remises et l'ordonnance ; lignes pour la marge et les articles.
 */
@Service
@Transactional(readOnly = true)
public class ClientsEquipePilotageServiceImpl implements ClientsEquipePilotageService {

    private static final int CLIENTS_EN_BAISSE = 20;
    private static final double CENT = 100.0;

    private final PeriodeComparaisonService periodeComparaisonService;
    private final PilotageClientsRepository pilotageClientsRepository;
    private final VentilateurPilotage ventilateurPilotage;

    public ClientsEquipePilotageServiceImpl(
        PeriodeComparaisonService periodeComparaisonService,
        PilotageClientsRepository pilotageClientsRepository,
        VentilateurPilotage ventilateurPilotage
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.pilotageClientsRepository = pilotageClientsRepository;
        this.ventilateurPilotage = ventilateurPilotage;
    }

    @Override
    public ClientsPilotageDTO analyserClients(RequetePilotageDTO requete) {
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        PeriodeDTO periode = comparaison.periode();
        PeriodeDTO reference = comparaison.reference();
        var officine = CategorieChiffreAffaire.officine();
        ClienteleDTO clientele = pilotageClientsRepository.lireClientele(periode.du(), periode.au(), SalesStatut.CLOSED, officine);
        ClienteleDTO clienteleReference = reference == null ? null : pilotageClientsRepository.lireClientele(reference.du(), reference.au(), SalesStatut.CLOSED, officine);
        Long nouveauxReference = reference == null ? null : pilotageClientsRepository.compterNouveaux(reference.du(), reference.au(), SalesStatut.CLOSED, officine);
        return new ClientsPilotageDTO(
            comparaison,
            Variations.cellule(UniteIndicateur.NOMBRE, (double) clientele.actifs(), clienteleReference == null ? null : (double) clienteleReference.actifs()),
            Variations.cellule(
                UniteIndicateur.NOMBRE,
                (double) pilotageClientsRepository.compterNouveaux(periode.du(), periode.au(), SalesStatut.CLOSED, officine),
                nouveauxReference == null ? null : (double) nouveauxReference
            ),
            reference == null ? null : pilotageClientsRepository.compterRevenus(reference.du(), reference.au(), periode.du(), periode.au(), SalesStatut.CLOSED, officine),
            reference == null ? null : pilotageClientsRepository.compterPerdus(reference.du(), reference.au(), periode.du(), periode.au(), SalesStatut.CLOSED, officine),
            Variations.cellule(UniteIndicateur.POURCENTAGE, partIdentifiee(clientele), clienteleReference == null ? null : partIdentifiee(clienteleReference)),
            Variations.cellule(UniteIndicateur.MONTANT, caMoyen(clientele), clienteleReference == null ? null : caMoyen(clienteleReference)),
            reference == null ? List.of() : listerEnBaisse(periode, reference)
        );
    }

    @Override
    public EquipePilotageDTO analyserEquipe(RequetePilotageDTO requete) {
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        boolean avecReference = comparaison.reference() != null;
        Map<String, MesuresVendeur> vendeurs = new LinkedHashMap<>();
        var entetes = ventilateurPilotage.comparer(SourceAnalyse.ENTETES, comparaison, AxeAnalyse.VENDEUR, null, List.of(), Granularite.MOIS);
        for (MesuresComparees element : entetes.elements()) {
            vendeurs.put(element.membre().cle(), new MesuresVendeur(element.membre().libelle(), element));
        }
        var lignes = ventilateurPilotage.comparer(SourceAnalyse.LIGNES, comparaison, AxeAnalyse.VENDEUR, null, List.of(), Granularite.MOIS);
        for (MesuresComparees element : lignes.elements()) {
            vendeurs.computeIfAbsent(element.membre().cle(), cle -> new MesuresVendeur(element.membre().libelle(), MesuresComparees.AUCUNES)).definirLignes(element);
        }
        MesuresVendeur equipe = new MesuresVendeur("Équipe", entetes.total());
        equipe.definirLignes(lignes.total());

        var ordonnances = ventilateurPilotage.comparer(SourceAnalyse.ENTETES, comparaison, AxeAnalyse.VENDEUR, AxeAnalyse.TYPE_PRESCRIPTION, List.of(), Granularite.MOIS);
        for (MesuresComparees element : ordonnances.elements()) {
            if (TypePrescription.PRESCRIPTION.name().equals(element.membre2().cle())) {
                MesuresVendeur vendeur = vendeurs.get(element.membre().cle());
                if (vendeur != null) {
                    vendeur.ajouterOrdonnance(element);
                }
                equipe.ajouterOrdonnance(element);
            }
        }
        compter(vendeurs, equipe, comparaison, true);
        compter(vendeurs, equipe, comparaison, false);

        List<LigneVendeurDTO> lignesVendeurs = new ArrayList<>(vendeurs.size());
        for (Map.Entry<String, MesuresVendeur> vendeur : vendeurs.entrySet()) {
            lignesVendeurs.add(vendeur.getValue().versLigne(vendeur.getKey(), avecReference));
        }
        lignesVendeurs.sort(Comparator.comparing((LigneVendeurDTO ligne) -> ligne.caTtc().valeur(), Comparator.nullsLast(Comparator.reverseOrder())));
        return new EquipePilotageDTO(comparaison, lignesVendeurs, equipe.versLigne("", avecReference));
    }

    /** Annulations ({@code annulations}) ou avoirs, sur la période puis sur la référence. */
    private void compter(Map<String, MesuresVendeur> vendeurs, MesuresVendeur equipe, ComparaisonPeriodesDTO comparaison, boolean annulations) {
        List<PeriodeDTO> periodes = comparaison.reference() == null ? List.of(comparaison.periode()) : List.of(comparaison.periode(), comparaison.reference());
        for (int rang = 0; rang < periodes.size(); rang++) {
            PeriodeDTO periode = periodes.get(rang);
            List<ComptageDTO> comptages = annulations
                ? pilotageClientsRepository.compterAnnulationsParVendeur(periode.du(), periode.au(), CategorieChiffreAffaire.officine())
                : pilotageClientsRepository.compterAvoirsParVendeur(periode.du().atStartOfDay(), periode.au().plusDays(1).atStartOfDay());
            for (ComptageDTO comptage : comptages) {
                MesuresVendeur vendeur = vendeurs.get(comptage.cle());
                if (vendeur != null) {
                    vendeur.ajouter(annulations, rang, comptage.nombre());
                }
                equipe.ajouter(annulations, rang, comptage.nombre());
            }
        }
    }

    /** Clients dont le CA recule le plus : le plus fort recul d'abord. */
    private List<ContributionDTO> listerEnBaisse(PeriodeDTO periode, PeriodeDTO reference) {
        var officine = CategorieChiffreAffaire.officine();
        Map<String, Long> caPeriode = new HashMap<>();
        for (ComptageDTO client : pilotageClientsRepository.sommerCaParClient(periode.du(), periode.au(), SalesStatut.CLOSED, officine)) {
            caPeriode.put(client.cle(), client.montant());
        }
        List<ContributionDTO> enBaisse = new ArrayList<>();
        for (ComptageDTO client : pilotageClientsRepository.sommerCaParClient(reference.du(), reference.au(), SalesStatut.CLOSED, officine)) {
            long ca = caPeriode.getOrDefault(client.cle(), 0L);
            if (ca < client.montant()) {
                enBaisse.add(new ContributionDTO(null, "Client", client.cle(), client.libelle(), ca, client.montant(), ca - client.montant()));
            }
        }
        return enBaisse.stream().sorted(Comparator.comparingLong(ContributionDTO::ecart)).limit(CLIENTS_EN_BAISSE).toList();
    }

    private static Double partIdentifiee(ClienteleDTO clientele) {
        return clientele.caTotal() == 0 ? null : clientele.caIdentifie() * CENT / clientele.caTotal();
    }

    private static Double caMoyen(ClienteleDTO clientele) {
        return clientele.actifs() == 0 ? null : (double) clientele.caIdentifie() / clientele.actifs();
    }

}
