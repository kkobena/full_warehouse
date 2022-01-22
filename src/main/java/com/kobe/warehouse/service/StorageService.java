package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.AppConfigurationRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.StorageRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class StorageService {
    private final StorageRepository storageRepository;
    private final AppConfigurationService appConfigurationService;
    private final UserRepository userRepository;

    public StorageService(StorageRepository storageRepository, AppConfigurationService appConfigurationService, UserRepository userRepository) {
        this.storageRepository = storageRepository;
        this.appConfigurationService = appConfigurationService;
        this.userRepository = userRepository;
    }

    public Storage getDefaultMagasinPointOfSale() {
        if (appConfigurationService.isMono()) return this.getDefaultMagasinMainStorage();
        return storageRepository.getOne(EntityConstant.POINT_of_STORAGE);
    }

    public Storage getStorageByMagasinIdAndType(Long magasinId, StorageType storageType) {
        return storageRepository.findFirstByMagasinIdAndStorageType(magasinId, storageType);

    }

    public Storage getOne(Long id) {
        return storageRepository.getOne(id);
    }

    public Storage getDefaultMagasinMainStorage() {
        return storageRepository.getOne(EntityConstant.DEFAULT_STORAGE);
    }

    public Storage getDefaultMagasinReserveStorage() {
        return storageRepository.getOne(EntityConstant.RESERVE_STORAGE);
    }

    private User getUser() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> userRepository.findOneByLogin(login));
        return user.orElseGet(null);
    }

    public Storage getDefaultConnectedUserMainStorage() {
        return this.getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.PRINCIPAL);
    }

    public Storage getDefaultConnectedUserPointOfSaleStorage() {
        if (appConfigurationService.isMono())
            return this.getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.PRINCIPAL);
        return this.getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.POINT_DE_VENTE);
    }

    public Storage getDefaultConnectedUserReserveStorage() {
        return this.getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.SAFETY_STOCK);
    }

    public Magasin getConnectedUserMagasin() {
        return getUser().getMagasin();
    }
}
