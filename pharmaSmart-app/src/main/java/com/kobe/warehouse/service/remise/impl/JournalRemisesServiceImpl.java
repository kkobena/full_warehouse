package com.kobe.warehouse.service.remise.impl;

import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.repository.PromotionRepository;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.portee.PorteeMagasinService;
import com.kobe.warehouse.service.remise.JournalRemisesService;
import com.kobe.warehouse.service.remise.dto.JournalRemisesFiltre;
import com.kobe.warehouse.service.remise.dto.LigneJournalDTO;
import com.kobe.warehouse.service.remise.dto.NatureJournal;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO.ParGrille;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO.ParPromotion;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO.Totaux;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO.TotauxUg;
import jakarta.persistence.Tuple;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JournalRemisesServiceImpl implements JournalRemisesService {

    /** Au-delà, un journal se lit par l'export de données programmable, pas à l'écran ni en PDF. */
    static final long JOURS_MAX = 366;

    private static final DateTimeFormatter FICHIER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JournalRemisesRequetes requetes;
    private final PorteeMagasinService porteeMagasinService;
    private final PromotionRepository promotionRepository;
    private final JournalRemisesExports exports;

    public JournalRemisesServiceImpl(
            JournalRemisesRequetes requetes,
            PorteeMagasinService porteeMagasinService,
            PromotionRepository promotionRepository,
            JournalRemisesExports exports) {
        this.requetes = requetes;
        this.porteeMagasinService = porteeMagasinService;
        this.promotionRepository = promotionRepository;
        this.exports = exports;
    }

    @Override
    public SyntheseJournalDTO calculerSynthese(JournalRemisesFiltre filtre) {
        verifier(filtre);
        Accumulateur acc = new Accumulateur(filtre);
        try (Stream<Tuple> lignes = requetes.lire(null, filtre, resoudreMagasin())) {
            lignes.map(LignesJournal::lireLigne).forEach(acc::ajouter);
        }
        return acc.construireSynthese(chargerPromotions(acc.listerPromotionIds()));
    }

    @Override
    public Page<LigneJournalDTO> listerLignes(NatureJournal nature, JournalRemisesFiltre filtre, Pageable pageable) {
        verifier(filtre);
        Objects.requireNonNull(nature, "nature");
        Integer magasin = resoudreMagasin();
        long total = requetes.compter(nature, filtre, magasin);
        List<LigneJournalDTO> contenu =
                requetes.lirePage(nature, filtre, magasin, (int) pageable.getOffset(), pageable.getPageSize()).stream()
                        .map(LignesJournal::lireLigne)
                        .toList();
        return new PageImpl<>(contenu, pageable, total);
    }

    @Override
    public byte[] exporter(FormatExport format, NatureJournal nature, JournalRemisesFiltre filtre) {
        SyntheseJournalDTO synthese = nature == null ? calculerSynthese(filtre) : null;
        verifier(filtre);
        Integer magasin = resoudreMagasin();
        Map<NatureJournal, List<LigneJournalDTO>> journaux = new LinkedHashMap<>();
        for (NatureJournal n : nature == null ? List.of(NatureJournal.values()) : List.of(nature)) {
            try (Stream<Tuple> lignes = requetes.lire(n, filtre, magasin)) {
                journaux.put(n, lignes.map(LignesJournal::lireLigne).toList());
            }
        }
        return exports.exporter(format, filtre, synthese, journaux);
    }

    @Override
    public String nommerFichier(FormatExport format, NatureJournal nature, JournalRemisesFiltre filtre) {
        String objet =
                nature == null ? "journal-remises" : "journal-" + nature.name().toLowerCase();
        return objet + "-" + filtre.du().format(FICHIER) + "-" + filtre.au().format(FICHIER) + "."
                + format.getExtension();
    }

    private Integer resoudreMagasin() {
        return porteeMagasinService.resoudreLeMagasinDExploitation().getId();
    }

    private Map<Long, Promotion> chargerPromotions(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return promotionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Promotion::getId, Function.identity()));
    }

    static void verifier(JournalRemisesFiltre filtre) {
        if (filtre == null || filtre.du() == null || filtre.au() == null) {
            throw new BadRequestAlertException("La période du journal est obligatoire.", "journalRemises", "periode");
        }
        if (filtre.au().isBefore(filtre.du())) {
            throw new BadRequestAlertException("La fin de la période précède son début.", "journalRemises", "periode");
        }
        if (ChronoUnit.DAYS.between(filtre.du(), filtre.au()) >= JOURS_MAX) {
            throw new BadRequestAlertException(
                    "Le journal se lit sur un an au plus ; au-delà, utilisez l'export de données « Lignes remisées ».",
                    "journalRemises",
                    "periode");
        }
    }

    /** Les totaux de la synthèse, ligne après ligne. Le filtre d'origine ne concerne que les remises. */
    private static final class Accumulateur {

        private final JournalRemisesFiltre filtre;
        private final Somme promotions = new Somme();
        private final Somme remisesProduit = new Somme();
        private final SommeUg ug = new SommeUg();
        private final Map<Long, Somme> parPromotion = new LinkedHashMap<>();
        private final Map<Long, String> libelles = new HashMap<>();
        private final Map<String, Somme> parGrille = new LinkedHashMap<>();

        Accumulateur(JournalRemisesFiltre filtre) {
            this.filtre = filtre;
        }

        void ajouter(LigneJournalDTO l) {
            boolean origineRetenue = filtre.origines().isEmpty()
                    || (l.origine() != null && filtre.origines().contains(l.origine()));
            if (origineRetenue && l.promotionId() != null) {
                promotions.ajouter(l, l.partPromotion(), l.unitesPromo());
                parPromotion
                        .computeIfAbsent(l.promotionId(), id -> new Somme())
                        .ajouter(l, l.partPromotion(), l.unitesPromo());
                libelles.putIfAbsent(l.promotionId(), l.promotionLibelle());
            }
            if (origineRetenue && l.partRemiseProduit() != 0) {
                remisesProduit.ajouter(l, l.partRemiseProduit(), l.quantite());
                parGrille
                        .computeIfAbsent(Objects.requireNonNullElse(l.grille(), "Sans grille"), g -> new Somme())
                        .ajouter(l, l.partRemiseProduit(), l.quantite());
            }
            if (l.quantiteUg() > 0) {
                ug.ajouter(l);
            }
        }

        Set<Long> listerPromotionIds() {
            return parPromotion.keySet();
        }

        SyntheseJournalDTO construireSynthese(Map<Long, Promotion> promotionsConnues) {
            List<ParPromotion> lignesPromotion = parPromotion.entrySet().stream()
                    .map(e -> {
                        Promotion p = promotionsConnues.get(e.getKey());
                        Somme s = e.getValue();
                        return new ParPromotion(
                                e.getKey(),
                                p != null ? p.getLibelle() : libelles.get(e.getKey()),
                                p != null ? p.getType() : null,
                                s.lignes,
                                s.tickets.size(),
                                s.unites,
                                compterUnitesOffertes(p, s.unites),
                                s.remise,
                                s.net);
                    })
                    .sorted(Comparator.comparingLong(ParPromotion::remise).reversed())
                    .toList();
            List<ParGrille> lignesGrille = parGrille.entrySet().stream()
                    .map(e -> new ParGrille(
                            e.getKey(),
                            e.getValue().lignes,
                            e.getValue().tickets.size(),
                            e.getValue().remise,
                            e.getValue().net))
                    .sorted(Comparator.comparingLong(ParGrille::remise).reversed())
                    .toList();
            return new SyntheseJournalDTO(
                    filtre.du(),
                    filtre.au(),
                    promotions.remise + remisesProduit.remise,
                    promotions.calculerTotaux(),
                    remisesProduit.calculerTotaux(),
                    ug.calculerTotaux(),
                    lignesPromotion,
                    lignesGrille);
        }

        /**
         * Les unités gratuites d'une offre N+M : M par groupe complet de N+M. Une vente ne pose que des
         * groupes complets, la division tombe juste.
         */
        static Long compterUnitesOffertes(Promotion p, long unitesRemisees) {
            if (p == null || p.getType() != TypePromotion.PROMO_OFFRE_NM) {
                return null;
            }
            int achetees = p.getQuantiteMin();
            int offertes = Objects.requireNonNullElse(p.getValeur(), 0);
            if (achetees + offertes <= 0) {
                return 0L;
            }
            return unitesRemisees / (achetees + offertes) * offertes;
        }
    }

    /** Le numéro d'un ticket repart chaque jour (table {@code reference}) : seul le couple jour, numéro l'identifie. */
    private static String identifierTicket(LigneJournalDTO l) {
        return l.date() + "|" + l.ticket();
    }

    private static final class Somme {
        long lignes;
        final Set<String> tickets = new HashSet<>();
        long unites;
        long brut;
        long remise;
        long net;

        void ajouter(LigneJournalDTO l, int partRemise, int unitesLigne) {
            lignes++;
            tickets.add(identifierTicket(l));
            unites += unitesLigne;
            brut += l.montantBrut();
            remise += partRemise;
            net += l.montantNet();
        }

        Totaux calculerTotaux() {
            return new Totaux(lignes, tickets.size(), unites, brut, remise, net);
        }
    }

    private static final class SommeUg {
        long lignes;
        final Set<String> tickets = new HashSet<>();
        long unites;
        long valeur;
        long remise;
        long htNet;
        long coutEvite;
        long marge;
        long margeComptabilisee;
        long margeReelle;

        void ajouter(LigneJournalDTO l) {
            lignes++;
            tickets.add(identifierTicket(l));
            unites += l.quantiteUg();
            valeur += l.valeurUg();
            remise += l.remiseUg();
            htNet += l.htNetUg();
            coutEvite += l.coutEviteUg();
            marge += l.margeUg();
            margeComptabilisee += l.margeComptabilisee();
            margeReelle += l.margeReelle();
        }

        TotauxUg calculerTotaux() {
            return new TotauxUg(
                    lignes,
                    tickets.size(),
                    unites,
                    valeur,
                    remise,
                    htNet,
                    coutEvite,
                    marge,
                    margeComptabilisee,
                    margeReelle);
        }
    }
}
