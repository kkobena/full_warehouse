package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.StorageRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.StorageDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StorageService {
  private final StorageRepository storageRepository;
  private final AppConfigurationService appConfigurationService;
  private final UserRepository userRepository;

  public StorageService(
      StorageRepository storageRepository,
      AppConfigurationService appConfigurationService,
      UserRepository userRepository) {
    this.storageRepository = storageRepository;
    this.appConfigurationService = appConfigurationService;
    this.userRepository = userRepository;
  }

  public Storage getDefaultMagasinPointOfSale() {
    if (appConfigurationService.isMono()) return getDefaultMagasinMainStorage();
    return storageRepository.getReferenceById(EntityConstant.POINT_of_STORAGE);
  }

  public Storage getStorageByMagasinIdAndType(Long magasinId, StorageType storageType) {
    return storageRepository.findFirstByMagasinIdAndStorageType(magasinId, storageType);
  }

  public Storage getOne(Long id) {
    return storageRepository.getReferenceById(id);
  }

  public Storage getDefaultMagasinMainStorage() {
    return storageRepository.getReferenceById(EntityConstant.DEFAULT_STORAGE);
  }

  public Storage getDefaultMagasinReserveStorage() {
    return storageRepository.getReferenceById(EntityConstant.RESERVE_STORAGE);
  }

  public User getUser() {
    Optional<User> user =
        SecurityUtils.getCurrentUserLogin().flatMap(login -> userRepository.findOneByLogin(login));
    return user.orElseGet(null);
  }

  @Cacheable(EntityConstant.PRINCIPAL_CACHE)
  public Storage getDefaultConnectedUserMainStorage() {
    return getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.PRINCIPAL);
  }

  @Cacheable(EntityConstant.POINT_DE_VENTE_CACHE)
  public Storage getDefaultConnectedUserPointOfSaleStorage() {
    if (appConfigurationService.isMono()) {
      return getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.PRINCIPAL);
    }

    return getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.POINT_DE_VENTE);
  }

  public Storage getDefaultConnectedUserReserveStorage() {
    return getStorageByMagasinIdAndType(getUser().getMagasin().getId(), StorageType.SAFETY_STOCK);
  }

  public Magasin getConnectedUserMagasin() {
    return getUser().getMagasin();
  }

  public User getUserFormImport() {
    Optional<User> user =
        SecurityUtils.getCurrentUserLogin().flatMap(login -> userRepository.findOneByLogin(login));
    return user.orElseGet(() -> userRepository.findOneByLogin(Constants.SYSTEM).get());
  }

  public Magasin getImportationMagasin() {
    return getUserFormImport().getMagasin();
  }

  public User getSystemeUser() {
    return userRepository.findOneByLogin(Constants.SYSTEM).orElse(getUser());
  }

  public List<StorageDTO> fetchAllByMagasin(Long magasinId) {

    return this.storageRepository.findAllByMagasinId(magasinId).stream()
        .map(StorageDTO::new)
        .toList();
  }

  public List<StorageDTO> fetchAllByConnectedUser() {
    return this.fetchAllByMagasin(this.getUser().getMagasin().getId());
  }
}
