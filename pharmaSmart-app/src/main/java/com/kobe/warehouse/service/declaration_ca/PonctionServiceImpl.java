package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CaPonction;
import com.kobe.warehouse.repository.CaPeriodeRepository;
import com.kobe.warehouse.repository.CaPonctionDetailRepository;
import com.kobe.warehouse.repository.CaPonctionRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.declaration_ca.dto.ModeCalculPonction;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionLigneDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionParamDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionParametresDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionSimulationDTO;
import com.kobe.warehouse.service.declaration_ca.dto.StatutPonction;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.id_generator.CaPonctionIdGeneratorService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PonctionServiceImpl implements PonctionService {

    /** Aperçu montré à l'écran : assez pour juger, pas assez pour noyer. */
    private static final int TAILLE_APERCU = 20;

    private static final String[] MODES_REGLEMENT = { "CASH" };

    /** Seule stratégie implémentée : les plus grosses ventes d'abord. */
    private static final String STRATEGIE_PAR_DEFAUT = "DECROISSANT";

    /** Seul le taux exonéré est ponctionnable (cf. D7). */
    private static final String TAUX_TVA_ELIGIBLES = "0";

    private final PonctionCalculator calculator;
    private final StorageService storageService;
    private final CaPonctionIdGeneratorService idGeneratorService;
    private final PonctionReportService reportService;
    private final CaPonctionRepository ponctionRepository;
    private final AppConfigurationService appConfigurationService;
    private final CaPonctionDetailRepository detailRepository;
    private final CaPeriodeRepository caPeriodeRepository;

    public PonctionServiceImpl(
        PonctionCalculator calculator,
        StorageService storageService,
        CaPonctionIdGeneratorService idGeneratorService,
        PonctionReportService reportService,
        CaPonctionRepository ponctionRepository,
        AppConfigurationService appConfigurationService,
        CaPonctionDetailRepository detailRepository,
        CaPeriodeRepository caPeriodeRepository
    ) {
        this.calculator = calculator;
        this.storageService = storageService;
        this.idGeneratorService = idGeneratorService;
        this.reportService = reportService;
        this.ponctionRepository = ponctionRepository;
        this.appConfigurationService = appConfigurationService;
        this.detailRepository = detailRepository;
        this.caPeriodeRepository = caPeriodeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PonctionSimulationDTO simuler(PonctionParamDTO param) {
        controlerPeriode(param);
        int magasinId = magasinId();
        BigDecimal plafond = plafondEffectif(param);

        PonctionCalculator.Assiette assiette = calculator.calculerAssiette(
            param.dateDebut(),
            param.dateFin(),
            magasinId,
            plafond,
            MODES_REGLEMENT
        );

        long caReel = caPeriodeRepository.caReel(param.dateDebut(), param.dateFin(), magasinId);
        long caApresExclusions = caPeriodeRepository.caApresExclusions(param.dateDebut(), param.dateFin(), magasinId);
        long objectif = objectif(param, caApresExclusions);

        List<String> avertissements = new ArrayList<>();
        boolean atteignable = objectif <= assiette.montantPonctionnable();
        if (!atteignable) {
            avertissements.add(messageObjectifInatteignable(objectif, assiette));
        }
        if (assiette.nombreVentes() == 0) {
            avertissements.add(
                "Aucune vente éligible sur cette période : ventes comptant, closes, non différées, " +
                "réglées en espèces, sans exclusion déjà appliquée et comportant des produits exonérés."
            );
        }
        chevauchement(param).ifPresent(periode ->
            avertissements.add("Cette période chevauche une ponction déjà validée : " + periode)
        );

        List<PonctionLigneDTO> lignes = atteignable && objectif > 0
            ? calculator.repartir(param.dateDebut(), param.dateFin(), magasinId, plafond, MODES_REGLEMENT, objectif)
            : List.of();

        long ponctionne = lignes.stream().mapToLong(PonctionLigneDTO::montantPonctionne).sum();

        return new PonctionSimulationDTO(
            param.dateDebut(),
            param.dateFin(),
            caReel,
            caApresExclusions,
            assiette.assietteTva0(),
            objectif,
            assiette.montantPonctionnable(),
            ponctionne,
            caApresExclusions - ponctionne,
            assiette.nombreVentes(),
            lignes.size(),
            tauxMoyen(ponctionne, caApresExclusions),
            tauxMax(lignes),
            atteignable,
            appConfigurationService.getPonctionAnnulationMaxDays(),
            lignes.stream().limit(TAILLE_APERCU).toList(),
            avertissements
        );
    }

    /**
     * Applique la ponction.
     *
     * <p>L'ordre des écritures n'est pas indifférent : le détail est écrit d'abord, puis les totaux de
     * l'en-tête en sont <strong>déduits</strong>. Les faire calculer séparément — ce qui était le cas —
     * revenait à exécuter deux fois la même requête et à espérer qu'elle rende deux fois le même
     * résultat ; une vente close entre les deux suffisait à les faire diverger, et l'invariant « le
     * détail totalise le montant ponctionné » tombait silencieusement. Ici il est vrai par
     * construction.
     */
    @Override
    public PonctionDTO valider(PonctionParamDTO param) {
        PonctionSimulationDTO simulation = simuler(param);
        if (!simulation.objectifAtteignable()) {
            throw new GenericError(messageObjectifInatteignable(simulation.montantObjectif(), simulation.montantPonctionnable()));
        }
        if (simulation.montantPonctionne() <= 0) {
            throw new GenericError("Aucune vente ne peut être ponctionnée sur cette période.");
        }

        Integer id = insererPonction(param, simulation);
        calculator.enregistrerDetail(
            id,
            param.dateDebut(),
            param.dateFin(),
            magasinId(),
            plafondEffectif(param),
            MODES_REGLEMENT,
            simulation.montantObjectif()
        );
        ponctionRepository.alignerEnTeteSurLeDetail(id);
        calculator.appliquer(id);
        return lire(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PonctionParametresDTO getParametres() {
        return new PonctionParametresDTO(
            appConfigurationService.getPonctionPlafondDefaut(),
            appConfigurationService.getPonctionAnnulationMaxDays()
        );
    }

    @Override
    public void annulerPourVente(Long saleId, LocalDate saleDate, Integer ponctionId) {
        if (ponctionId == null) {
            return;
        }
        ponctionRepository
            .findById(ponctionId)
            .filter(ponction -> ponction.getStatut() == StatutPonction.VALIDEE)
            .ifPresent(ponction -> {
                calculator.annuler(ponctionId);
                ponction
                    .setStatut(StatutPonction.ANNULEE)
                    .setCanceledAt(LocalDateTime.now())
                    .setCommentaire(motifAnnulationAutomatique(ponction.getCommentaire(), saleId, saleDate));
                ponctionRepository.save(ponction);
            });
    }

    /** Le motif est conservé dans le commentaire : sans lui, l'historique montre une annulation sans auteur ni cause. */
    private String motifAnnulationAutomatique(String commentaire, Long saleId, LocalDate saleDate) {
        String motif = "Annulée automatiquement : la vente %d du %s a été annulée.".formatted(saleId, saleDate);
        return commentaire == null || commentaire.isBlank() ? motif : commentaire + " — " + motif;
    }

    @Override
    public PonctionDTO annuler(Integer id) {
        CaPonction ponction = charger(id);
        if (ponction.getStatut() != StatutPonction.VALIDEE) {
            throw new GenericError("Seule une ponction validée peut être annulée.");
        }
        int delai = appConfigurationService.getPonctionAnnulationMaxDays();
        long joursEcoules = ChronoUnit.DAYS.between(
            Objects.requireNonNullElse(ponction.getValidatedAt(), ponction.getCreatedAt()),
            LocalDateTime.now()
        );
        if (joursEcoules > delai) {
            throw new GenericError(
                String.format(
                    "Cette ponction date de %d jour(s) : elle n'est plus annulable. Le délai est de %d jour(s).",
                    joursEcoules,
                    delai
                )
            );
        }
        calculator.annuler(id);
        ponction.setStatut(StatutPonction.ANNULEE).setCanceledAt(LocalDateTime.now()).setCanceledBy(storageService.getUser());
        return versDto(ponctionRepository.save(ponction));
    }


    @Override
    @Transactional(readOnly = true)
    public List<PonctionDTO> getHistorique() {
        return ponctionRepository.findByMagasinIdOrderByDateDebutDesc(magasinId()).stream().map(this::versDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PonctionLigneDTO> getDetail(Integer id) {
        return detailRepository
            .findByPonctionIdOrderByRang(id)
            .stream()
            .map(d ->
                new PonctionLigneDTO(
                    d.getSaleId(),
                    d.getSaleDate(),
                    d.getNumeroTransaction(),
                    d.getMontantVente(),
                    d.getMontantBase(),
                    d.getMontantPonctionne(),
                    d.getRang()
                )
            )
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToPdf(Integer id) {
        return reportService.exportToPdfBytes(lire(id), getDetail(id));
    }

    // ===== Écritures =====

    /**
     * Crée l'en-tête de la ponction.
     *
     * <p>{@code saveAndFlush} et non {@code save} : c'est la contrainte d'exclusion GiST qui rejette
     * une période chevauchante, et elle ne parle qu'au moment où l'insertion atteint la base. Sans
     * vidage explicite, l'erreur ne surviendrait qu'à la validation de la transaction — trop tard
     * pour la traduire en message compréhensible.
     */
    private Integer insererPonction(PonctionParamDTO param, PonctionSimulationDTO simulation) {
        AppUser appUser=storageService.getUser();
        CaPonction entite = new CaPonction()
            .setId(idGeneratorService.getNextIdAsInt())
            .setMagasin(storageService.getDefaultConnectedUserMainStorage().getMagasin())
            .setDateDebut(param.dateDebut())
            .setDateFin(param.dateFin())
            .setModeCalcul(param.modeCalcul())
            .setValeurSaisie(param.valeur())
            .setPlafondParVente(plafondEffectif(param))
            .setStrategie(STRATEGIE_PAR_DEFAUT)
            .setModesReglement(String.join(",", MODES_REGLEMENT))
            .setTauxTvaEligibles(TAUX_TVA_ELIGIBLES)
            .setCaReel(simulation.caReel())
            .setCaApresExclusions(simulation.caApresExclusions())
            .setCaAssietteTva0(simulation.caAssietteTva0())
            .setMontantPonctionnable(simulation.montantPonctionnable())
            .setMontantObjectif(simulation.montantObjectif())
            // Les totaux définitifs sont déduits du détail juste après son écriture.
            .setMontantPonctionne(0)
            .setCaDeclare(simulation.caApresExclusions())
            .setNombreVentes(0)
            .setStatut(StatutPonction.VALIDEE)
            .setCommentaire(param.commentaire())
            .setCreatedBy(appUser)
            .setCreatedAt(LocalDateTime.now())
            .setValidatedBy(appUser)
            .setValidatedAt(LocalDateTime.now());
        try {
            return ponctionRepository.saveAndFlush(entite).getId();
        } catch (DataIntegrityViolationException e) {
            // C'est la contrainte d'exclusion qui parle : la période est déjà prise.
            throw new GenericError(
                "Cette période chevauche une ponction déjà validée. Les périodes ne peuvent pas se recouvrir."
            );
        }
    }


    // ===== Lectures et utilitaires =====

    private PonctionDTO lire(Integer id) {
        return versDto(charger(id));
    }

    private CaPonction charger(Integer id) {
        return ponctionRepository
            .findByIdAndMagasinId(id, magasinId())
            .orElseThrow(() -> new GenericError("Ponction introuvable"));
    }

    /**
     * Projection de l'entité vers le DTO.
     *
     * <p>Remplace un mapping manuel depuis {@code Object[]}, où chaque champ se lisait par son
     * indice : ajouter une colonne au milieu de la requête décalait silencieusement tout le reste.
     */
    private PonctionDTO versDto(CaPonction p) {
        return new PonctionDTO(
            p.getId(),
            p.getDateDebut(),
            p.getDateFin(),
            p.getModeCalcul(),
            p.getValeurSaisie(),
            p.getPlafondParVente(),
            p.getCaReel(),
            p.getCaApresExclusions(),
            p.getCaDeclare(),
            p.getMontantObjectif(),
            p.getMontantPonctionne(),
            p.getNombreVentes(),
            p.getStatut(),
            p.getCommentaire(),
            nomComplet(p.getCreatedBy()),
            p.getCreatedAt(),
            p.getValidatedAt(),
            p.getCanceledAt()
        );
    }

    private String nomComplet(AppUser utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        String prenom = utilisateur.getFirstName();
        String nom = utilisateur.getLastName();
        return prenom == null || nom == null ? utilisateur.getLogin() : prenom + " " + nom;
    }

    /**
     * Le pourcentage porte sur le chiffre d'affaires après exclusions — celui que le pharmacien lit
     * sur ses états — et non sur l'assiette exonérée, qui n'est qu'une contrainte de faisabilité.
     */
    private long objectif(PonctionParamDTO param, long caApresExclusions) {
        if (param.modeCalcul() == ModeCalculPonction.MONTANT_FIXE) {
            return param.valeur().setScale(0, RoundingMode.HALF_UP).longValue();
        }
        return BigDecimal
            .valueOf(caApresExclusions)
            .multiply(param.valeur())
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .longValue();
    }

    private void controlerPeriode(PonctionParamDTO param) {
        if (param.dateFin().isBefore(param.dateDebut())) {
            throw new GenericError("La date de fin précède la date de début.");
        }
        BigDecimal plafond = plafondEffectif(param);
        if (plafond.signum() <= 0 || plafond.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new GenericError("Le plafond par vente doit être compris entre 0 et 100 %.");
        }
    }

    /** Le contrôle applicatif double la contrainte GiST : il sert à prévenir avant, pas à garantir. */
    private Optional<String> chevauchement(PonctionParamDTO param) {
        return ponctionRepository
            .findChevauchantes(
                magasinId(),
                param.dateDebut(),
                param.dateFin(),
                List.of(StatutPonction.VALIDEE)
            )
            .stream()
            .findFirst()
            .map(p -> "du " + p.getDateDebut() + " au " + p.getDateFin());
    }

    private String messageObjectifInatteignable(long objectif, PonctionCalculator.Assiette assiette) {
        return messageObjectifInatteignable(objectif, assiette.montantPonctionnable());
    }

    private String messageObjectifInatteignable(long objectif, long ponctionnable) {
        return String.format(
            "Objectif inatteignable : %d F demandés pour un maximum de %d F. " +
            "Le plafond par vente et la part de produits exonérés bornent ce qui peut être prélevé.",
            objectif,
            ponctionnable
        );
    }

    private BigDecimal tauxMoyen(long ponctionne, long caApresExclusions) {
        if (caApresExclusions <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal
            .valueOf(ponctionne)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(caApresExclusions), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal tauxMax(List<PonctionLigneDTO> lignes) {
        return lignes
            .stream()
            .filter(ligne -> ligne.montantVente() > 0)
            .map(ligne ->
                BigDecimal
                    .valueOf(ligne.montantPonctionne())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(ligne.montantVente()), 2, RoundingMode.HALF_UP)
            )
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }

    /** Le plafond saisi, ou celui de l'officine. */
    private BigDecimal plafondEffectif(PonctionParamDTO param) {
        return param.plafondEffectif(appConfigurationService.getPonctionPlafondDefaut());
    }

    private int magasinId() {
        return storageService.getDefaultConnectedUserMainStorage().getMagasin().getId();
    }

}
