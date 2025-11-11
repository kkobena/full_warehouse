package com.kobe.warehouse.service.settings;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Poste;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.service.settings.dto.PosteRecord;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    @Override
    @Cacheable(
        value = EntityConstant.APP_POST_CONFIG,
        key = "#address + '-' + #name"
    )
    public Optional<PosteRecord> findFirstByAddressOrName(String address, String name) {
        return posteRepository.findFirstByAddressOrName(address, name)
            .map(this::buildRecordFromEntity);
    }

    @Override
    @Transactional
    public void create(PosteRecord posteRecord) {
        posteRepository.save(buildEntityFromRecord(posteRecord));
    }


    @Override
    @Transactional
    public void delete(Integer id) {
        posteRepository.deleteById(id);
    }

    private PosteRecord buildRecordFromEntity(Poste poste) {
        return new PosteRecord(
            poste.getId(),
            poste.getName(),
            poste.getPosteNumber(),
            poste.getAddress(),
            poste.isCustomerDisplay(),
            poste.getCustomerDisplayPort()
        );
    }

    private Poste buildEntityFromRecord(PosteRecord posteRecord) {

        Poste poste = new Poste()
            .setName(posteRecord.name())
            .setPosteNumber(posteRecord.posteNumber())
            .setAddress(posteRecord.address())
            .setCustomerDisplay(posteRecord.customerDisplay())
            .setCustomerDisplayPort(posteRecord.customerDisplayPort());
        if (posteRecord.id() != null) {
            poste.setId(posteRecord.id());
        }
        return poste;
    }
}
