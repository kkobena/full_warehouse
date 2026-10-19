package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ExportModele;
import com.kobe.warehouse.domain.enumeration.SujetExport;
import com.kobe.warehouse.repository.ExportModeleRepository;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.exports.ExportFichierDTO;
import com.kobe.warehouse.service.dto.exports.ExportModeleDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.exports.ExportsFichiersService;
import com.kobe.warehouse.service.exports.ModelesExportService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Un modèle programmé s'exécute au nom de son propriétaire, sans session : le droit d'export est vérifié à l'enregistrement et à
 * chaque exécution manuelle ; le fichier rejoint l'historique du propriétaire. Une édition n'étant bâtie qu'avec les droits de
 * qui la demande, elle s'enregistre pour être rejouée d'un clic mais ne se programme pas.
 */
@Service
@Transactional
public class ModelesExportServiceImpl implements ModelesExportService {

    private static final Logger LOG = LoggerFactory.getLogger(ModelesExportServiceImpl.class);

    private final ExportModeleRepository exportModeleRepository;
    private final ExportsFichiersService exportsFichiersService;
    private final NavAccessService navAccessService;
    private final UserService userService;

    public ModelesExportServiceImpl(
        ExportModeleRepository exportModeleRepository,
        ExportsFichiersService exportsFichiersService,
        NavAccessService navAccessService,
        UserService userService
    ) {
        this.exportModeleRepository = exportModeleRepository;
        this.exportsFichiersService = exportsFichiersService;
        this.navAccessService = navAccessService;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportModeleDTO> listerModeles() {
        AppUser utilisateur = userService.getUser();
        return exportModeleRepository.listerVisibles(utilisateur.getId()).stream().map(modele -> versDTO(modele, utilisateur)).toList();
    }

    @Override
    public ExportModeleDTO enregistrerModele(ExportModeleDTO dto) {
        AppUser utilisateur = userService.getUser();
        SujetExport export = ExportsFichiersServiceImpl.lireSujet(dto.export());
        verifierDroit(export);
        if (export.isPeriodique() && dto.periode() == null) {
            throw new GenericError("Choisissez la période du modèle");
        }
        if (!export.getFormats().contains(dto.format())) {
            throw new GenericError(export.getLibelle() + " ne se produit pas en " + dto.format().name());
        }
        if (dto.frequence() != null && !export.estProgrammable()) {
            throw new GenericError(
                export.getLibelle() + " ne se programme pas : cette édition est bâtie avec les droits de qui la demande"
            );
        }
        ExportModele modele = dto.id() == null ? new ExportModele().setProprietaire(utilisateur) : lireModifiable(dto.id(), utilisateur);
        modele
            .setLibelle(dto.libelle().trim())
            .setExport(export)
            .setFormat(dto.format())
            .setPeriode(export.isPeriodique() ? dto.periode() : null)
            .setPartage(dto.partage())
            .setFiltres(FiltresDemandes.retenir(export, dto.filtres()))
            .setFrequence(dto.frequence())
            .setHeure(dto.frequence() == null ? null : Objects.requireNonNullElse(dto.heure(), ProgrammationExport.HEURE_PAR_DEFAUT))
            .setJour(dto.frequence() == null ? null : dto.jour())
            .setProchaineExecution(
                dto.frequence() == null ? null : ProgrammationExport.calculerProchaine(dto.frequence(), dto.heure(), dto.jour(), LocalDateTime.now())
            );
        return versDTO(exportModeleRepository.save(modele), utilisateur);
    }

    @Override
    public void supprimerModele(Long id) {
        exportModeleRepository.delete(lireModifiable(id, userService.getUser()));
    }

    @Override
    public ExportFichierDTO executerModele(Long id) {
        AppUser utilisateur = userService.getUser();
        ExportModele modele = exportModeleRepository.findById(id).orElseThrow(() -> new GenericError("Modèle introuvable"));
        if (!modele.isPartage() && !Objects.equals(modele.getProprietaire().getId(), utilisateur.getId())) {
            throw new ForbiddenOperationException("Ce modèle n'est pas partagé", "exportModelePrive");
        }
        if (!navAccessService.isAllowed(modele.getExport().getDroits(), NavAction.EXPORT)) {
            throw new ForbiddenOperationException("Export non autorisé : " + modele.getExport().getLibelle(), "exportInterdit");
        }
        return exportsFichiersService.lancerModele(modele, utilisateur, LocalDateTime.now().toLocalDate());
    }

    @Override
    public void executerProgrammes(LocalDateTime maintenant) {
        for (ExportModele modele : exportModeleRepository.listerProgrammesDus(maintenant)) {
            LOG.info("Export programmé « {} » lancé pour {}", modele.getLibelle(), modele.getProprietaire().getLogin());
            exportsFichiersService.lancerModele(modele, modele.getProprietaire(), maintenant.toLocalDate());
            modele.setProchaineExecution(ProgrammationExport.calculerProchaine(modele.getFrequence(), modele.getHeure(), modele.getJour(), maintenant));
        }
    }

    private void verifierDroit(SujetExport export) {
        if (!navAccessService.isAllowed(export.getDroits(), NavAction.EXPORT)) {
            throw new ForbiddenOperationException("Export non autorisé : " + export.getLibelle(), "exportInterdit");
        }
    }

    private ExportModele lireModifiable(Long id, AppUser utilisateur) {
        ExportModele modele = exportModeleRepository.findById(id).orElseThrow(() -> new GenericError("Modèle introuvable"));
        if (!Objects.equals(modele.getProprietaire().getId(), utilisateur.getId())) {
            throw new ForbiddenOperationException("Seul l'auteur d'un modèle peut le modifier ou le supprimer", "exportModeleNonModifiable");
        }
        return modele;
    }

    private static ExportModeleDTO versDTO(ExportModele modele, AppUser utilisateur) {
        AppUser proprietaire = modele.getProprietaire();
        return new ExportModeleDTO(
            modele.getId(),
            modele.getLibelle(),
            modele.getExport().name(),
            modele.getFormat(),
            modele.getPeriode(),
            modele.isPartage(),
            modele.getFrequence(),
            modele.getHeure(),
            modele.getJour(),
            modele.getProchaineExecution(),
            proprietaire.getFirstName() + " " + proprietaire.getLastName(),
            Objects.equals(proprietaire.getId(), utilisateur.getId()),
            modele.getFiltres()
        );
    }
}
