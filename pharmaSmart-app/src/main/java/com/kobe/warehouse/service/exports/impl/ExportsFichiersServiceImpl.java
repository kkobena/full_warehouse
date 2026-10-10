package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ExportFichier;
import com.kobe.warehouse.domain.ExportModele;
import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.StatutExport;
import com.kobe.warehouse.repository.ExportFichierRepository;
import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.exports.DemandeExportDTO;
import com.kobe.warehouse.service.dto.exports.ExportFichierDTO;
import com.kobe.warehouse.service.dto.exports.FichierExporteDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.exports.ExportsFichiersService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
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
    private final GenerateurExports generateurExports;
    private final RepertoireExports repertoireExports;
    private final NavAccessService navAccessService;
    private final UserService userService;

    public ExportsFichiersServiceImpl(
        ExportFichierRepository exportFichierRepository,
        GenerateurExports generateurExports,
        RepertoireExports repertoireExports,
        NavAccessService navAccessService,
        UserService userService
    ) {
        this.exportFichierRepository = exportFichierRepository;
        this.generateurExports = generateurExports;
        this.repertoireExports = repertoireExports;
        this.navAccessService = navAccessService;
        this.userService = userService;
    }

    @Override
    public ExportFichierDTO demander(DemandeExportDTO demande) {
        verifierDroit(demande.export());
        return inscrire(demande.export(), demande.format(), demande.du(), demande.au(), userService.getUser(), null);
    }

    @Override
    public ExportFichierDTO lancerModele(ExportModele modele, AppUser demandeur, LocalDate aujourdhui) {
        LocalDate[] periode = modele.getPeriode() == null ? new LocalDate[] { null, null } : modele.getPeriode().calculer(aujourdhui);
        return inscrire(modele.getExport(), modele.getFormat(), periode[0], periode[1], demandeur, modele);
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
    private ExportFichierDTO inscrire(ExportDonnees export, FormatExport format, LocalDate du, LocalDate au, AppUser demandeur, ExportModele modele) {
        boolean periodique = export.isPeriodique();
        if (periodique && (du == null || au == null || au.isBefore(du))) {
            throw new GenericError("Choisissez une période valide pour cet export");
        }
        ExportFichier fichier = exportFichierRepository.save(
            new ExportFichier()
                .setExport(export)
                .setFormat(format)
                .setDu(periodique ? du : null)
                .setAu(periodique ? au : null)
                .setStatut(StatutExport.EN_ATTENTE)
                .setDemandePar(demandeur)
                .setDemandeLe(LocalDateTime.now())
                .setModele(modele)
        );
        fichier.setFichier(export.name().toLowerCase() + "-" + fichier.getId() + "." + format.getExtension());
        Long id = fichier.getId();
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    generateurExports.generer(id);
                }
            }
        );
        return versDTO(fichier);
    }

    private void verifierDroit(ExportDonnees export) {
        if (!navAccessService.isAllowed(List.of(export.getRubrique().getDroit()), NavAction.EXPORT)) {
            throw new ForbiddenOperationException("Export non autorisé : " + export.getRubrique().getLibelle(), "exportInterdit");
        }
    }

    private static boolean estAdministrateur() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);
    }

    /** « lignes-de-vente_2026-10-01_2026-10-31.xlsx » */
    private static String nommer(ExportFichier fichier) {
        String base = fichier.getExport().name().toLowerCase().replace('_', '-');
        String periode = fichier.getDu() == null ? "_" + fichier.getDemandeLe().format(DATE_FICHIER) : "_" + fichier.getDu().format(DATE_FICHIER) + "_" + fichier.getAu().format(DATE_FICHIER);
        return base + periode + "." + fichier.getFormat().getExtension();
    }

    static ExportFichierDTO versDTO(ExportFichier fichier) {
        AppUser demandePar = fichier.getDemandePar();
        return new ExportFichierDTO(
            fichier.getId(),
            fichier.getExport(),
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
            fichier.getModele() == null ? null : fichier.getModele().getLibelle()
        );
    }
}
