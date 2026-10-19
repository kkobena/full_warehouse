package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ExportFichier;
import com.kobe.warehouse.domain.ExportModele;
import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.SujetExport;
import com.kobe.warehouse.domain.enumeration.SujetsExport;
import com.kobe.warehouse.domain.enumeration.StatutExport;
import com.kobe.warehouse.repository.ExportFichierRepository;
import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.exports.ContenuExportDTO;
import com.kobe.warehouse.service.dto.exports.DemandeExportDTO;
import com.kobe.warehouse.service.dto.exports.ExportFichierDTO;
import com.kobe.warehouse.service.dto.exports.FichierExporteDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.exports.ExportsFichiersService;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional
public class ExportsFichiersServiceImpl implements ExportsFichiersService {

    private static final DateTimeFormatter DATE_FICHIER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ExportFichierRepository exportFichierRepository;
    private final EcritureExport ecritureExport;
    private final GenerateurExports generateurExports;
    private final RepertoireExports repertoireExports;
    private final NavAccessService navAccessService;
    private final UserService userService;

    public ExportsFichiersServiceImpl(
        ExportFichierRepository exportFichierRepository,
        EcritureExport ecritureExport,
        GenerateurExports generateurExports,
        RepertoireExports repertoireExports,
        NavAccessService navAccessService,
        UserService userService
    ) {
        this.exportFichierRepository = exportFichierRepository;
        this.ecritureExport = ecritureExport;
        this.generateurExports = generateurExports;
        this.repertoireExports = repertoireExports;
        this.navAccessService = navAccessService;
        this.userService = userService;
    }

    @Override
    public ContenuExportDTO produire(DemandeExportDTO demande) {
        SujetExport export = lireSujet(demande.export());
        verifierDroit(export);
        verifierFormat(export, demande.format());
        boolean periodique = export.isPeriodique();
        LocalDate du = periodique ? demande.du() : null;
        LocalDate au = periodique ? demande.au() : null;
        verifierPeriode(periodique, du, au);
        Map<FiltreExport, List<String>> filtres = FiltresDemandes.retenir(export, demande.filtres());

        // Le fichier tient en mémoire le temps de la réponse : c'est le prix de l'immédiateté, et
        // l'écrivain reste le même qu'en tâche de fond — rien n'est tenu en plus côté base.
        ByteArrayOutputStream contenu = new ByteArrayOutputStream();
        ecritureExport.ecrire(export, demande.format(), du, au, filtres, contenu);
        return new ContenuExportDTO(contenu.toByteArray(), nommer(export, demande.format(), du, au, LocalDate.now()), demande.format().getTypeMime());
    }

    @Override
    public ExportFichierDTO demander(DemandeExportDTO demande) {
        SujetExport export = lireSujet(demande.export());
        verifierDroit(export);
        verifierFormat(export, demande.format());
        return inscrire(export, demande.format(), demande.du(), demande.au(), demande.filtres(), userService.getUser(), null);
    }

    @Override
    public ExportFichierDTO lancerModele(ExportModele modele, AppUser demandeur, LocalDate aujourdhui) {
        LocalDate[] periode = modele.getPeriode() == null ? new LocalDate[] { null, null } : modele.getPeriode().calculer(aujourdhui);
        return inscrire(modele.getExport(), modele.getFormat(), periode[0], periode[1], modele.getFiltres(), demandeur, modele);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExportFichierDTO> listerHistorique(Pageable page) {
        Page<ExportFichier> fichiers = estAdministrateur()
            ? exportFichierRepository.findAllByOrderByDemandeLeDesc(page)
            : exportFichierRepository.findByDemandeParIdOrderByDemandeLeDesc(userService.getUser().getId(), page);
        return fichiers.map(ExportsFichiersServiceImpl::versDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public FichierExporteDTO lireFichier(Long id) {
        ExportFichier fichier = exportFichierRepository.findById(id).orElseThrow(() -> new GenericError("Export introuvable"));
        if (!estAdministrateur() && !Objects.equals(fichier.getDemandePar().getId(), userService.getUser().getId())) {
            throw new ForbiddenOperationException("Ce fichier a été exporté par un autre utilisateur", "exportAutreUtilisateur");
        }
        verifierDroit(fichier.getExport());
        Path chemin = fichier.getFichier() == null ? null : repertoireExports.resoudre(fichier.getFichier());
        if (fichier.getStatut() != StatutExport.TERMINE || chemin == null || !Files.exists(chemin)) {
            throw new GenericError("Le fichier n'est pas ou plus disponible");
        }
        return new FichierExporteDTO(chemin, nommer(fichier), fichier.getFormat().getTypeMime());
    }

    @Override
    public void purgerExpires(LocalDateTime maintenant) {
        for (ExportFichier fichier : exportFichierRepository.listerExpires(StatutExport.TERMINE, maintenant)) {
            repertoireExports.supprimer(fichier.getFichier());
            fichier.setStatut(StatutExport.EXPIRE);
        }
    }

    /** Inscrit l'export puis, une fois la transaction validée, en lance la génération : le générateur relit une ligne existante. */
    private ExportFichierDTO inscrire(
        SujetExport export,
        FormatExport format,
        LocalDate du,
        LocalDate au,
        Map<FiltreExport, List<String>> filtres,
        AppUser demandeur,
        ExportModele modele
    ) {
        boolean periodique = export.isPeriodique();
        verifierPeriode(periodique, du, au);
        ExportFichier fichier = exportFichierRepository.save(
            new ExportFichier()
                .setExport(export)
                .setFormat(format)
                .setDu(periodique ? du : null)
                .setAu(periodique ? au : null)
                .setFiltres(FiltresDemandes.retenir(export, filtres))
                .setStatut(StatutExport.EN_ATTENTE)
                .setDemandePar(demandeur)
                .setDemandeLe(LocalDateTime.now())
                .setModele(modele)
        );
        fichier.setFichier(export.name().toLowerCase() + "-" + fichier.getId() + "." + format.getExtension());
        Long id = fichier.getId();
        // Une édition se bâtit avec les droits de qui la demande ; un export programmé n'a personne derrière lui.
        SecurityContext contexte = SecurityContextHolder.getContext();
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    generateurExports.generer(id, contexte);
                }
            }
        );
        return versDTO(fichier);
    }

    static SujetExport lireSujet(String code) {
        try {
            return SujetsExport.lire(code);
        } catch (IllegalArgumentException e) {
            throw new GenericError("Export inconnu : " + code);
        }
    }

    /** Le droit de l'entrée : celui de sa rubrique pour un export de données, celui de son écran pour une édition. */
    private void verifierDroit(SujetExport export) {
        if (!navAccessService.isAllowed(export.getDroits(), NavAction.EXPORT)) {
            throw new ForbiddenOperationException("Export non autorisé : " + export.getLibelle(), "exportInterdit");
        }
    }

    private static void verifierPeriode(boolean periodique, LocalDate du, LocalDate au) {
        if (periodique && (du == null || au == null || au.isBefore(du))) {
            throw new GenericError("Choisissez une période valide pour cet export");
        }
    }

    private static void verifierFormat(SujetExport export, FormatExport format) {
        if (!export.getFormats().contains(format)) {
            throw new GenericError(export.getLibelle() + " ne se produit pas en " + format.name());
        }
    }

    private static boolean estAdministrateur() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);
    }

    private static String nommer(ExportFichier fichier) {
        return nommer(fichier.getExport(), fichier.getFormat(), fichier.getDu(), fichier.getAu(), fichier.getDemandeLe().toLocalDate());
    }

    /** « lignes-de-vente_2026-10-01_2026-10-31.xlsx » ; sans période, le jour de la demande. */
    private static String nommer(SujetExport export, FormatExport format, LocalDate du, LocalDate au, LocalDate demandeLe) {
        String base = export.name().toLowerCase().replace('_', '-');
        String periode = du == null
            ? "_" + demandeLe.format(DATE_FICHIER)
            : "_" + du.format(DATE_FICHIER) + "_" + au.format(DATE_FICHIER);
        return base + periode + "." + format.getExtension();
    }

    static ExportFichierDTO versDTO(ExportFichier fichier) {
        AppUser demandePar = fichier.getDemandePar();
        return new ExportFichierDTO(
            fichier.getId(),
            fichier.getExport().name(),
            fichier.getExport().getLibelle(),
            fichier.getFormat(),
            fichier.getDu(),
            fichier.getAu(),
            fichier.getStatut(),
            fichier.getNombreLignes(),
            fichier.getTaille(),
            fichier.getErreur(),
            demandePar.getFirstName() + " " + demandePar.getLastName(),
            fichier.getDemandeLe(),
            fichier.getTermineLe(),
            fichier.getExpireLe(),
            fichier.getModele() == null ? null : fichier.getModele().getLibelle(),
            RedacteurFiltresExport.rediger(fichier.getFiltres())
        );
    }
}
