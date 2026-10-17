package com.kobe.warehouse.service.etiquette.impl;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.domain.enumeration.TypeEtiquette;
import com.kobe.warehouse.domain.ordonnance.Ordonnance;
import com.kobe.warehouse.domain.ordonnance.OrdonnanceLigne;
import com.kobe.warehouse.domain.ordonnance.Prescripteur;
import com.kobe.warehouse.repository.OrdonnanceRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.dto.etiquette.EtiquettePreparationDTO;
import com.kobe.warehouse.service.etiquette.EtiquettePdf;
import com.kobe.warehouse.service.etiquette.EtiquetteUsageService;
import com.kobe.warehouse.service.etiquette.zpl.ZplEtiquetteService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Les trois usages du lot 3. Ils partagent le gabarit commun des usages, qui choisit son contenu
 * sur le {@link TypeEtiquette}, et passent donc par le même catalogue de formats et les mêmes
 * deux chemins d'impression que les étiquettes d'article.
 */
@Service
public class EtiquetteUsageServiceImpl implements EtiquetteUsageService {

    private static final DateTimeFormatter DATE_LISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final OrdonnanceRepository ordonnanceRepository;
    private final ZplEtiquetteService zplEtiquetteService;

    public EtiquetteUsageServiceImpl(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        OrdonnanceRepository ordonnanceRepository,
        ZplEtiquetteService zplEtiquetteService
    ) {
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.ordonnanceRepository = ordonnanceRepository;
        this.zplEtiquetteService = zplEtiquetteService;
    }

    @Override
    public byte[] imprimerReglette(Produit produit, int qty, int startAt, FormatEtiquette format) {
        return imprimer(construireReglettes(produit, qty), TypeEtiquette.REGLETTE, startAt, format);
    }

    @Override
    public String genererZplReglette(Produit produit, int qty, FormatEtiquette format, ResolutionEtiquette resolution) {
        return zplEtiquetteService.rendreEtiquettes(construireReglettes(produit, qty), TypeEtiquette.REGLETTE, format, resolution);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] imprimerPosologie(Integer ordonnanceId, Integer ligneId, int qty, int startAt, FormatEtiquette format) {
        return imprimer(construirePosologies(ordonnanceId, ligneId, qty), TypeEtiquette.POSOLOGIE, startAt, format);
    }

    @Override
    @Transactional(readOnly = true)
    public String genererZplPosologie(
        Integer ordonnanceId,
        Integer ligneId,
        int qty,
        FormatEtiquette format,
        ResolutionEtiquette resolution
    ) {
        return zplEtiquetteService.rendreEtiquettes(
            construirePosologies(ordonnanceId, ligneId, qty),
            TypeEtiquette.POSOLOGIE,
            format,
            resolution
        );
    }

    @Override
    public byte[] imprimerPreparation(EtiquettePreparationDTO saisie, int qty, int startAt, FormatEtiquette format) {
        return imprimer(construirePreparations(saisie, qty), TypeEtiquette.PREPARATION, startAt, format);
    }

    @Override
    public String genererZplPreparation(
        EtiquettePreparationDTO saisie,
        int qty,
        FormatEtiquette format,
        ResolutionEtiquette resolution
    ) {
        return zplEtiquetteService.rendreEtiquettes(construirePreparations(saisie, qty), TypeEtiquette.PREPARATION, format, resolution);
    }

    private byte[] imprimer(List<EtiquetteDTO> etiquettes, TypeEtiquette type, int startAt, FormatEtiquette format) {
        int depart = format.normaliserCaseDeDepart(startAt);
        List<EtiquetteDTO> planche = new ArrayList<>();
        for (int i = 1; i < depart; i++) {
            planche.add(new EtiquetteDTO().setPrint(false));
        }
        planche.addAll(etiquettes);

        Context contexte = new Context(Locale.FRANCE);
        contexte.setVariable(Constant.ITEMS, ListUtils.partition(planche, format.getColonnes()));
        contexte.setVariable(Constant.ETIQUETTES_FORMAT, format);
        contexte.setVariable(Constant.ETIQUETTES_TYPE, type);
        contexte.setVariable(Constant.ETIQUETES_BEGIN, depart);
        // Un rouleau n'a pas de planche : son gabarit fixe la page à la taille de l'étiquette.
        String gabarit = format.isRouleau() ? Constant.ETIQUETES_ROULEAU_USAGE_TEMPLATE_FILE : Constant.ETIQUETES_USAGE_TEMPLATE_FILE;
        return EtiquettePdf.rendre(templateEngine.process(gabarit, contexte), listerCodesBarres(planche));
    }

    /** Seule la réglette porte un code-barres ; une posologie et une préparation n'en ont pas. */
    private List<String> listerCodesBarres(List<EtiquetteDTO> etiquettes) {
        List<String> codes = new ArrayList<>(etiquettes.size());
        for (EtiquetteDTO etiquette : etiquettes) {
            if (etiquette.isPrint() && StringUtils.hasText(etiquette.getCode())) {
                codes.add(etiquette.getCode());
            }
        }
        return codes;
    }

    private List<EtiquetteDTO> construireReglettes(Produit produit, int qty) {
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        EtiquetteDTO etiquette = new EtiquetteDTO()
            .setCode(fournisseurProduit.getCodeCip())
            .setLibelle(produit.getLibelle())
            .setPrix(String.format("%s CFA", NumberUtil.formatToString(fournisseurProduit.getPrixUni())))
            .setMagasin(nommerMagasin())
            .setDate(LocalDate.now().format(DATE_LISIBLE))
            .setPrint(true);
        return repeter(etiquette, qty);
    }

    private List<EtiquetteDTO> construirePosologies(Integer ordonnanceId, Integer ligneId, int qty) {
        Ordonnance ordonnance = ordonnanceRepository
            .findById(ordonnanceId)
            .orElseThrow(() -> new IllegalArgumentException("Ordonnance introuvable : " + ordonnanceId));
        String patient = nommerPatient(ordonnance.getCustomer());
        String prescripteur = nommerPrescripteur(ordonnance.getPrescripteur());
        String magasin = nommerMagasin();
        String date = ordonnance.getDatePrescription().format(DATE_LISIBLE);
        List<EtiquetteDTO> etiquettes = new ArrayList<>();
        for (OrdonnanceLigne ligne : ordonnance.getLignes()) {
            // Une ligne précise, ou toute l'ordonnance : le comptoir imprime parfois une seule boîte.
            if (ligneId == null || ligneId.equals(ligne.getId())) {
                etiquettes.addAll(repeter(construirePosologie(ligne, patient, prescripteur, magasin, date), qty));
            }
        }
        return etiquettes;
    }

    private EtiquetteDTO construirePosologie(OrdonnanceLigne ligne, String patient, String prescripteur, String magasin, String date) {
        return new EtiquetteDTO()
            .setLibelle(nommerLigne(ligne))
            .setPatient(patient)
            .setPrescripteur(prescripteur)
            .setPosologie(ligne.getPosologie())
            .setDuree(ligne.getDureeJours() == null ? null : "Pendant " + ligne.getDureeJours() + " jours")
            .setMagasin(magasin)
            .setDate(date)
            .setPrint(true);
    }

    private List<EtiquetteDTO> construirePreparations(EtiquettePreparationDTO saisie, int qty) {
        LocalDate preparationLe = saisie.datePreparation() == null ? LocalDate.now() : saisie.datePreparation();
        EtiquetteDTO etiquette = new EtiquetteDTO()
            .setLibelle(saisie.libelle())
            .setComposition(saisie.composition())
            .setPatient(saisie.patient())
            .setPrescripteur(saisie.prescripteur())
            .setPosologie(saisie.posologie())
            .setMention(saisie.mention())
            .setMagasin(nommerMagasin())
            .setDate(preparationLe.format(DATE_LISIBLE))
            .setExpiryDate(saisie.peremption() == null ? null : saisie.peremption().format(DATE_LISIBLE))
            .setPrint(true);
        return repeter(etiquette, qty);
    }

    private List<EtiquetteDTO> repeter(EtiquetteDTO etiquette, int qty) {
        List<EtiquetteDTO> etiquettes = new ArrayList<>(Math.max(qty, 0));
        for (int i = 0; i < qty; i++) {
            etiquettes.add(etiquette);
        }
        return etiquettes;
    }

    /** Le produit du catalogue quand la ligne y est rattachée, sinon le texte lu sur l'ordonnance. */
    private String nommerLigne(OrdonnanceLigne ligne) {
        if (ligne.getProduit() != null) {
            return ligne.getProduit().getLibelle();
        }
        return ligne.getTexteLu();
    }

    private String nommerPatient(Customer customer) {
        if (customer == null) {
            return null;
        }
        return (StringUtils.hasText(customer.getFirstName()) ? customer.getFirstName() + " " : "") + customer.getLastName();
    }

    private String nommerPrescripteur(Prescripteur prescripteur) {
        if (prescripteur == null) {
            return null;
        }
        String nom = "Dr " + (StringUtils.hasText(prescripteur.getPrenom()) ? prescripteur.getPrenom() + " " : "") + prescripteur.getNom();
        return StringUtils.hasText(prescripteur.getSpecialite()) ? nom + " — " + prescripteur.getSpecialite() : nom;
    }

    private String nommerMagasin() {
        return storageService.getConnectedUserMagasin().getName().toUpperCase();
    }
}
