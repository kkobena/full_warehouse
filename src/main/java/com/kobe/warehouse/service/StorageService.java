package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.StorageRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.StorageDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StorageService {

    private final StorageRepository storageRepository;
    private final AppConfigurationService appConfigurationService;
    private final UserRepository userRepository;
    private final UserService userService;

    public StorageService(
        StorageRepository storageRepository,
        AppConfigurationService appConfigurationService,
        UserRepository userRepository, UserService userService
    ) {
        this.storageRepository = storageRepository;
        this.appConfigurationService = appConfigurationService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public Storage getStorageByMagasinIdAndType(Long magasinId, StorageType storageType) {
        return storageRepository.findFirstByMagasinIdAndStorageType(magasinId, storageType);
    }

    public Storage getOne(Long id) {
        return storageRepository.getReferenceById(id);
    }

    @Cacheable(EntityConstant.DEFAULT_MAIN_STORAGE)
    public Storage getDefaultMagasinMainStorage() {
        return storageRepository.getReferenceById(EntityConstant.DEFAULT_STORAGE);
    }


    public AppUser getUser() {
        return userService.getUser();
    }

    @Cacheable(value = EntityConstant.USER_MAIN_STORAGE_CACHE,key = "#root.target.getUser().getMagasin().getId()")
    public Storage getDefaultConnectedUserMainStorage() {
        return getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.PRINCIPAL);
    }

    @Cacheable(value =EntityConstant.POINT_DE_VENTE_CACHE,key = "#root.target.getUser().getMagasin().getId()")
    public Storage getDefaultConnectedUserPointOfSaleStorage() {
        if (appConfigurationService.isMono()) {
            return getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.PRINCIPAL);
        }

        return getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.POINT_DE_VENTE);
    }

    @Cacheable( value =EntityConstant.USER_RESERVE_STORAGE_CACHE,key = "#root.target.getUser().getMagasin().getId()")
    public Storage getDefaultConnectedUserReserveStorage() {
        return getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.SAFETY_STOCK);
    }


    public Magasin getConnectedUserMagasin() {
        return getUser().getMagasin();
    }

    public AppUser getUserFormImport() {
        Optional<AppUser> user = SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin);
        return user.orElse(userRepository.findOneByLogin(Constants.SYSTEM).get());
    }

    public Magasin getImportationMagasin() {
        return getUserFormImport().getMagasin();
    }

    public AppUser getSystemeUser() {
        return userRepository.findOneByLogin(Constants.SYSTEM).orElse(getUser());
    }

    public List<StorageDTO> fetchAllByMagasin(Long magasinId) {
        return this.storageRepository.findAllByMagasinId(magasinId).stream().map(StorageDTO::new).toList();
    }

    @Cacheable(EntityConstant.USER_STORAGE_CACHE)
    public List<StorageDTO> fetchAllByConnectedUser() {
        return this.fetchAllByMagasin(this.getUser().getMagasin().getId());
    }
}
