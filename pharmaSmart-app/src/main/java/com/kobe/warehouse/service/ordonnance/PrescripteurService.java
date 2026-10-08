package com.kobe.warehouse.service.ordonnance;

import com.kobe.warehouse.domain.ordonnance.Prescripteur;
import com.kobe.warehouse.repository.OrdonnanceRepository;
import com.kobe.warehouse.repository.PrescripteurRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.ordonnance.PrescripteurDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Référentiel local des prescripteurs : constitué par l'usage, retrouvé par
 * recherche approchée (pg_trgm sur le nom normalisé), jamais supprimé — désactivé ou fusionné.
 */
@Service
@Transactional
public class PrescripteurService {

    private final PrescripteurRepository prescripteurRepository;
    private final OrdonnanceRepository ordonnanceRepository;
    private final UserService userService;

    public PrescripteurService(PrescripteurRepository prescripteurRepository, OrdonnanceRepository ordonnanceRepository, UserService userService) {
        this.prescripteurRepository = prescripteurRepository;
        this.ordonnanceRepository = ordonnanceRepository;
        this.userService = userService;
    }

    /** Fiches actives dont le nom contient la saisie ou lui ressemble ; sans saisie, les plus récentes. */
    @Transactional(readOnly = true)
    public List<PrescripteurDTO> rechercher(String q, int limite) {
        PageRequest page = PageRequest.of(0, Math.clamp(limite, 1, 50));
        List<Prescripteur> trouves = StringUtils.hasText(q)
            ? prescripteurRepository.rechercher(q.trim(), page)
            : prescripteurRepository.findByActifTrueOrderByCreatedAtDescIdDesc(page);
        return trouves.stream().map(PrescripteurService::toDto).toList();
    }

    public PrescripteurDTO creer(PrescripteurDTO dto) {
        if (StringUtils.hasText(dto.numeroOrdre()) && prescripteurRepository.existsByNumeroOrdre(dto.numeroOrdre().trim())) {
            throw numeroOrdreExistant();
        }
        Prescripteur prescripteur = renseigner(new Prescripteur(), dto).setCreatedBy(userService.getUser());
        return toDto(prescripteurRepository.save(prescripteur));
    }

    public PrescripteurDTO modifier(Integer id, PrescripteurDTO dto) {
        Prescripteur prescripteur = charger(id);
        if (StringUtils.hasText(dto.numeroOrdre()) && prescripteurRepository.existsByNumeroOrdreAndIdNot(dto.numeroOrdre().trim(), id)) {
            throw numeroOrdreExistant();
        }
        return toDto(prescripteurRepository.save(renseigner(prescripteur, dto)));
    }

    /** {@code actif = false} plutôt que suppression : les ordonnances déjà saisies gardent leur prescripteur. */
    public void definirActif(Integer id, boolean actif) {
        prescripteurRepository.save(charger(id).setActif(actif));
    }

    /** Les ordonnances de la fiche source passent sur la fiche conservée ; la source est désactivée, jamais supprimée. */
    public PrescripteurDTO fusionner(Integer sourceId, Integer cibleId) {
        if (sourceId.equals(cibleId)) {
            throw new GenericError("Une fiche ne peut pas être fusionnée avec elle-même.", "prescripteurFusionIdentique");
        }
        Prescripteur source = charger(sourceId);
        Prescripteur cible = charger(cibleId);
        ordonnanceRepository.reaffecterPrescripteur(source, cible);
        // La mise à jour en masse vide le contexte de persistance : on recharge avant de désactiver.
        prescripteurRepository.save(charger(sourceId).setActif(false));
        return toDto(charger(cibleId));
    }

    @Transactional(readOnly = true)
    public PrescripteurDTO lire(Integer id) {
        return toDto(charger(id));
    }

    private Prescripteur charger(Integer id) {
        return prescripteurRepository.findById(id).orElseThrow(() -> new GenericError("Prescripteur introuvable", "prescripteurIntrouvable"));
    }

    private static GenericError numeroOrdreExistant() {
        return new GenericError("Ce numéro d'ordre est déjà celui d'une autre fiche.", "prescripteurNumeroOrdreExiste");
    }

    private static Prescripteur renseigner(Prescripteur prescripteur, PrescripteurDTO dto) {
        return prescripteur
            .setNom(dto.nom().trim())
            .setPrenom(propre(dto.prenom()))
            .setSpecialite(propre(dto.specialite()))
            .setNumeroOrdre(propre(dto.numeroOrdre()))
            .setStructure(propre(dto.structure()))
            .setTelephone(propre(dto.telephone()));
    }

    private static String propre(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    private static PrescripteurDTO toDto(Prescripteur p) {
        return new PrescripteurDTO(p.getId(), p.getNom(), p.getPrenom(), p.getSpecialite(), p.getNumeroOrdre(), p.getStructure(), p.getTelephone(), p.isActif());
    }
}
