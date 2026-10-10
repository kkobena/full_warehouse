package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ExportModele;
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
 * chaque exécution manuelle ; le fichier rejoint l'historique du propriétaire.
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
        verifierDroit(dto);
        if (dto.export().isPeriodique() && dto.periode() == null) {
            throw new GenericError("Choisissez la période du modèle");
        }
        ExportModele modele = dto.id() == null ? new ExportModele().setProprietaire(utilisateur) : lireModifiable(dto.id(), utilisateur);
        modele
            .setLibelle(dto.libelle().trim())
            .setExport(dto.export())
            .setFormat(dto.format())
            .setPeriode(dto.export().isPeriodique() ? dto.periode() : null)
            .setPartage(dto.partage())
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
        if (!navAccessService.isAllowed(List.of(modele.getExport().getRubrique().getDroit()), NavAction.EXPORT)) {
            throw new ForbiddenOperationException("Export non autorisé : " + modele.getExport().getRubrique().getLibelle(), "exportInterdit");
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

    private void verifierDroit(ExportModeleDTO dto) {
        if (!navAccessService.isAllowed(List.of(dto.export().getRubrique().getDroit()), NavAction.EXPORT)) {
            throw new ForbiddenOperationException("Export non autorisé : " + dto.export().getRubrique().getLibelle(), "exportInterdit");
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
            modele.getExport(),
            modele.getFormat(),
            modele.getPeriode(),
            modele.isPartage(),
            modele.getFrequence(),
            modele.getHeure(),
            modele.getJour(),
            modele.getProchaineExecution(),
            proprietaire.getFirstName() + " " + proprietaire.getLastName(),
            Objects.equals(proprietaire.getId(), utilisateur.getId())
        );
    }
}
