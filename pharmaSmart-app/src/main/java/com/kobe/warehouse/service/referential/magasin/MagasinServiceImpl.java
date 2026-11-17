package com.kobe.warehouse.service.referential.magasin;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.service.portee.PorteeMagasinService;
import com.kobe.warehouse.service.RayonService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.MagasinDTO;
import com.kobe.warehouse.service.errors.GenericError;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class MagasinServiceImpl implements MagasinService {

    /** Officine puis dépôt : deux niveaux suffisent, une officine ne dépendant jamais d'une autre. */
    private static final int PROFONDEUR_MAX = 2;

    private final MagasinRepository magasinRepository;
    private final PorteeMagasinService porteeMagasinService;
    private final UserService userService;
    private final StorageService storageService;
    private final RayonService rayonService;

    public MagasinServiceImpl(
        MagasinRepository magasinRepository,
        UserService userService,
        StorageService storageService,
        RayonService rayonService,
        PorteeMagasinService porteeMagasinService
    ) {
        this.magasinRepository = magasinRepository;
        this.userService = userService;
        this.storageService = storageService;
        this.rayonService = rayonService;
        this.porteeMagasinService = porteeMagasinService;
    }

    @Override
    @CacheEvict(value = {EntityConstant.USER_MAGASIN, EntityConstant.CURRENT_USER_CACHE}, allEntries = true)
    public MagasinDTO save(MagasinDTO magasinDto) {
        Magasin aEnregistrer = toEntity(magasinDto);
        appliquerLaHierarchie(aEnregistrer, magasinDto);
        appliquerLaCapaciteDeCommander(aEnregistrer, magasinDto);
        interdireIdentiteFneHorsRacine(aEnregistrer);
        Magasin magasin = magasinRepository.save(aEnregistrer);
        if (magasinDto.getId() != null) {
            return new MagasinDTO(magasin);
        }
        Set<Storage> storages = storageService.createStorageForNewMagasin(magasin);
        this.rayonService.initDefaultRayon(storages);

        return new MagasinDTO(magasin);
    }

    @Override
    public MagasinDTO currentUserMagasin() {
        return new MagasinDTO(this.userService.getUser().getMagasin());
    }

    @Override
    public List<MagasinDTO> listerLesMagasinsDAffectation() {
        Magasin courant = userService.getUser().getMagasin();
        List<MagasinDTO> magasins = new ArrayList<>();
        magasins.add(new MagasinDTO(courant));
        // Un dépôt n'est jamais proposé : personne ne s'y affecte, il n'a ni caisse ni saisie.
        for (Magasin rattache : magasinRepository.findAllByParentId(courant.getId())) {
            if (rattache.getTypeMagasin() != TypeMagasin.DEPOT) {
                magasins.add(new MagasinDTO(rattache));
            }
        }
        return magasins;
    }

    @Override
    public List<MagasinDTO> listerLaPorteeDeSupervision() {
        List<MagasinDTO> magasins = new ArrayList<>();
        for (Integer id : porteeMagasinService.resoudreLaPorteeDeSupervision()) {
            magasinRepository.findById(id).map(MagasinDTO::new).ifPresent(magasins::add);
        }
        return magasins;
    }

    @Override
    public MagasinDTO findById(Integer id) {
        return magasinRepository.findById(id).map(MagasinDTO::new).orElse(null);
    }

    @Override
    public void delete(Integer id) {
        Magasin magasin = magasinRepository.findById(id).orElseThrow();
        List<Storage> storages = storageService.findAllByMagasin(magasin);
        storages.forEach(rayonService::deleteByStorage);
        storageService.deleteAll(storages);
        magasinRepository.delete(magasin);
    }

    @Override
    public List<MagasinDTO> findAll(Set<TypeMagasin> types) {
        if (CollectionUtils.isEmpty(types)) {
            return magasinRepository.findAll().stream().map(MagasinDTO::new).toList();
        }
        return magasinRepository.findAll(magasinRepository.hasTypes(EnumSet.copyOf(types))).stream().map(MagasinDTO::new).toList();
    }

    @Override
    public boolean hasDepot() {
        return magasinRepository.existsByTypeMagasin(TypeMagasin.DEPOT);
    }

    @Override
    public MagasinDTO resoudreOfficineRacine(Integer magasinId) {
        Magasin magasin = magasinRepository.findById(magasinId).orElseThrow();
        // L'arbre est profond d'un niveau, mais la boucle est bornée plutôt que supposée : un
        // parent mal posé à la main ne doit pas faire tourner le serveur indéfiniment.
        int gardeFou = 0;
        while (magasin.getParent() != null && gardeFou < PROFONDEUR_MAX) {
            magasin = magasin.getParent();
            gardeFou++;
        }
        return new MagasinDTO(magasin);
    }

    /**
     * Pose et contrôle le rattachement avant l'enregistrement.
     *
     * <p>Le parent absent n'est pas une erreur : s'il n'existe qu'une officine, le cas est certain
     * et on la pose. C'est la même règle que la migration qui a rattaché l'existant, et elle évite
     * d'imposer un champ de plus aux écrans qui créent un dépôt.
     */
    private void appliquerLaHierarchie(Magasin magasin, MagasinDTO dto) {
        if (magasin.getTypeMagasin() == TypeMagasin.OFFICINE) {
            if (dto.getParentId() != null) {
                throw new GenericError("Une officine est racine : elle ne dépend d'aucun autre magasin.",
                    "magasin.officineSansParent");
            }
            magasin.setParent(null);
            return;
        }

        Integer parentId = dto.getParentId();
        if (parentId == null) {
            List<Magasin> officines = magasinRepository.findAllByTypeMagasin(TypeMagasin.OFFICINE);
            if (officines.size() != 1) {
                throw new GenericError("Préciser l'officine dont dépend ce magasin.", "magasin.parentRequis");
            }
            magasin.setParent(officines.getFirst());
            return;
        }
        if (parentId.equals(magasin.getId())) {
            throw new GenericError("Un magasin ne peut pas dépendre de lui-même.", "magasin.parentCirculaire");
        }
        Magasin parent = magasinRepository.findById(parentId)
            .orElseThrow(() -> new GenericError("L'officine de rattachement est introuvable.", "magasin.parentIntrouvable"));
        if (parent.getTypeMagasin() != TypeMagasin.OFFICINE) {
            throw new GenericError("Un magasin ne peut dépendre que d'une officine.", "magasin.parentNonOfficine");
        }
        magasin.setParent(parent);
    }

    /**
     * Un écran qui ignore {@code commandeEnPropre} ne doit pas écraser la valeur enregistrée : à
     * la modification on reprend l'existant, à la création on retombe sur la nature du magasin.
     * Un dépôt ne commande pas — il s'approvisionne auprès de son officine.
     */
    private void appliquerLaCapaciteDeCommander(Magasin magasin, MagasinDTO dto) {
        if (dto.getCommandeEnPropre() != null) {
            magasin.setCommandeEnPropre(dto.getCommandeEnPropre());
            return;
        }
        if (dto.getId() != null) {
            magasinRepository.findById(dto.getId()).ifPresent(existant -> magasin.setCommandeEnPropre(existant.isCommandeEnPropre()));
            return;
        }
        magasin.setCommandeEnPropre(magasin.getTypeMagasin() != TypeMagasin.DEPOT);
    }

    /**
     * Seule la racine certifie. Un dépôt ne vend pas au comptoir et une unité de gestion facture
     * sous l'identité de son officine : laisser renseigner un point de vente FNE ailleurs qu'à la
     * racine produirait des factures normalisées au mauvais nom, sans rien signaler.
     */
    private void interdireIdentiteFneHorsRacine(Magasin magasin) {
        if (magasin.getParent() == null) {
            return;
        }
        if (StringUtils.hasText(magasin.getFnePointOfSale()) || StringUtils.hasText(magasin.getFneSecretKey())) {
            throw new GenericError("Seule l'officine porte une identité FNE : les factures de ce magasin partent sous la sienne.",
                "magasin.fneReserveeALOfficine");
        }
    }

    private Magasin toEntity(MagasinDTO dto) {
        Magasin magasin = new Magasin();
        if (Objects.nonNull(dto.getId())) {
            magasin.setId(dto.getId());
        }
        magasin.setEmail(dto.getEmail());
        magasin.setWelcomeMessage(dto.getWelcomeMessage());
        magasin.setName(dto.getName());
        magasin.setFullName(dto.getFullName());
        magasin.setPhone(dto.getPhone());
        magasin.setAddress(dto.getAddress());
        magasin.setNote(dto.getNote());
        magasin.setRegistre(dto.getRegistre());
        magasin.setCompteContribuable(dto.getCompteContribuable());
        magasin.setCompteBancaire(dto.getCompteBancaire());
        magasin.setNumComptable(dto.getNumComptable());
        magasin.setRegistreImposition(dto.getRegistreImposition());
        magasin.setTypeMagasin(dto.getTypeMagasin());
        magasin.setManagerLastName(dto.getManagerLastName());
        magasin.setManagerFirstName(dto.getManagerFirstName());
        magasin.setFneSecretKey(dto.getFneSecretKey());
        magasin.setFnePointOfSale(dto.getFnePointOfSale());
        return magasin;
    }
}
