package com.kobe.warehouse.service.settings;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Poste;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.service.settings.dto.PosteRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PosteServiceImpl implements PosteService {

    private final PosteRepository posteRepository;

    public PosteServiceImpl(PosteRepository posteRepository) {
        this.posteRepository = posteRepository;
    }

    @Override
    public List<PosteRecord> findAll() {
        return posteRepository.findAll().stream()
                .map(this::buildRecordFromEntity)
                .toList();
    }

    /**
     * Lu à chaque démarrage de poste client et à chaque vente. La réponse « aucun poste » n'est PAS
     * mise en cache (pour un {@code Optional}, {@code #result} désigne la valeur qu'il contient) :
     * un poste enregistré après une première recherche ratée restait sinon introuvable 24 h.
     */
    @Override
    @Cacheable(value = EntityConstant.APP_POST_CONFIG, key = "#address + '-' + #name", unless = "#result == null")
    public Optional<PosteRecord> findFirstByAddressOrName(String address, String name) {
        return posteRepository.findFirstByAddressOrName(address, name).map(this::buildRecordFromEntity);
    }

    /**
     * Crée ou modifie (identifiant fourni) : l'adresse ou le nom d'un poste en cache peut changer,
     * et la clé du cache ne permet pas de retrouver l'entrée concernée — tout est vidé.
     */
    @Override
    @Transactional
    @CacheEvict(value = EntityConstant.APP_POST_CONFIG, allEntries = true)
    public PosteRecord create(PosteRecord posteRecord) {
        return buildRecordFromEntity(posteRepository.save(buildEntityFromRecord(posteRecord)));
    }

    /** Un poste supprimé restait sinon reconnu jusqu'à expiration du cache. */
    @Override
    @Transactional
    @CacheEvict(value = EntityConstant.APP_POST_CONFIG, allEntries = true)
    public void delete(Integer id) {
        posteRepository.deleteById(id);
    }

    private PosteRecord buildRecordFromEntity(Poste poste) {
        return new PosteRecord(poste.getId(), poste.getName(), poste.getPosteNumber(), poste.getAddress());
    }

    private Poste buildEntityFromRecord(PosteRecord posteRecord) {
        Poste poste = new Poste()
                .setName(posteRecord.name())
                .setPosteNumber(posteRecord.posteNumber())
                .setAddress(posteRecord.address());
        if (posteRecord.id() != null) {
            poste.setId(posteRecord.id());
        }
        return poste;
    }
}
