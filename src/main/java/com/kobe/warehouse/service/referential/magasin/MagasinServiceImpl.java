package com.kobe.warehouse.service.referential.magasin;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.service.RayonService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.MagasinDTO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class MagasinServiceImpl implements MagasinService {

    private final MagasinRepository magasinRepository;
    private final UserService userService;
    private final StorageService storageService;
    private final RayonService rayonService;

    public MagasinServiceImpl(MagasinRepository magasinRepository, UserService userService, StorageService storageService, RayonService rayonService) {
        this.magasinRepository = magasinRepository;
        this.userService = userService;
        this.storageService = storageService;
        this.rayonService = rayonService;
    }

    @Override
    public MagasinDTO save(MagasinDTO magasinDto) {
        Magasin magasin = magasinRepository.save(toEntity(magasinDto));
        if(magasinDto.getId() != null){
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
    public MagasinDTO findById(Long id) {
        return magasinRepository.findById(id).map(MagasinDTO::new).orElse(null);
    }

    @Override
    public void delete(Long id) {
        magasinRepository.deleteById(id);
    }

    @Override
    public List<MagasinDTO> findAll(Set<TypeMagasin> types) {
        if (CollectionUtils.isEmpty(types)) {
            return magasinRepository.findAll().stream().map(MagasinDTO::new).toList();
        }
        return magasinRepository.findAll(magasinRepository.hasTypes(EnumSet.copyOf(types))).stream().map(MagasinDTO::new).toList();
    }

    private Magasin toEntity(MagasinDTO dto) {
        Magasin magasin = new Magasin();
        magasin.setId(dto.getId());
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
        return magasin;
    }
}
