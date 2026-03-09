package com.kobe.warehouse.service.settings;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.ModelReapprovisionnement;
import com.kobe.warehouse.repository.AppConfigurationRepository;
import com.kobe.warehouse.service.UserService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppConfigurationService {

    private final UserService userService;
    private final AppConfigurationRepository appConfigurationRepository;

    public AppConfigurationService(UserService userService, AppConfigurationRepository appConfigurationRepository) {
        this.userService = userService;
        this.appConfigurationRepository = appConfigurationRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_MONO_STOCK)
    public boolean isMono() {
        Optional<AppConfiguration> appConfiguration = appConfigurationRepository.findById(EntityConstant.APP_GESTION_STOCK);
        return appConfiguration.map(configuration -> Integer.parseInt(configuration.getValue().trim()) == 0).orElse(true);
    }

    @Transactional(readOnly = true)
    public Optional<AppConfiguration> findOneById(String id) {
        return appConfigurationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_GESTION_STOCK)
    public Optional<AppConfiguration> findStockParam() {
        return appConfigurationRepository.findById(EntityConstant.APP_GESTION_STOCK);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_GESTION_LOT)
    public Optional<Boolean> useLot() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_GESTION_LOT)
            .map(configuration -> {
                try {
                    return Integer.parseInt(configuration.getValue().trim()) == 1;
                } catch (NumberFormatException _) {
                    return false;
                }
            });
    }

    @Transactional
    public void update(AppConfiguration appConfiguration) {
        appConfigurationRepository
            .findById(appConfiguration.getName())
            .map(configuration -> {
                configuration.setValue(appConfiguration.getValue());
                configuration.setDescription(appConfiguration.getDescription());
                configuration.setUpdated(LocalDateTime.now());
                configuration.setValidatedBy(userService.getUser());
                return appConfigurationRepository.save(configuration);
            });
    }

    @Transactional(readOnly = true)
    public List<AppConfiguration> fetchAll(String search) {
        return appConfigurationRepository.findAllByNameOrDescriptionContainingAllIgnoreCase(
            search,
            search,
            Sort.by(Sort.Direction.ASC, "description")
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_RESET_INVOICE_NUMBER)
    public Optional<AppConfiguration> findParamResetInvoiceNumberEveryYear() {
        return appConfigurationRepository.findById(EntityConstant.APP_RESET_INVOICE_NUMBER);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_SUGGESTION_RETENTION)
    public int findSuggestionRetention() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_SUGGESTION_RETENTION)
            .map(appConfiguration -> {
                try {
                    return Integer.parseInt(appConfiguration.getValue());
                } catch (NumberFormatException _) {
                    return 90;
                }
            })
            .orElse(90);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_POS_PRINTER_ITEM_COUNT_PER_PAGE)
    public int getPrinterItemCount() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_POS_PRINTER_ITEM_COUNT_PER_PAGE)
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .orElse(40);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.USER_MAGASIN)
    public Magasin getMagasin() {
        return userService.getUser().getMagasin();
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_EXPIRY_ALERT_DAYS_BEFORE)
    public int getNombreJourAlertPeremption() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_EXPIRY_ALERT_DAYS_BEFORE)
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .orElse(30);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_NOMBRE_JOUR_AVANT_PEREMPTION)
    public int getNombreJourPeremption() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_NOMBRE_JOUR_AVANT_PEREMPTION)
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .orElse(7);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.EXCLUDE_FREE_UNIT)
    public boolean excludeFreeUnit() {
        return appConfigurationRepository
            .findById(EntityConstant.EXCLUDE_FREE_UNIT)
            .map(AppConfiguration::getValue)
            .map(Boolean::parseBoolean)
            .orElse(false);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_NBRE_JOUR_RETENTION_COMMANDE)
    public int getNombreJourRetentionCommande() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_NBRE_JOUR_RETENTION_COMMANDE)
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .orElse(30);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_CUSTOMER_DISPLAY)
    public boolean isCustomerDisplayActif() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_CUSTOMER_DISPLAY)
            .map(configuration -> Integer.parseInt(configuration.getValue().trim()) == 0)
            .orElse(false);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_NTH_MOIS_CONSOMMATION_CACHE)
    public int getNthMoisConsommation() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_NTH_MOIS_CONSOMMATION)
            .map(AppConfiguration::getValue)
            .map(value -> {
                try {
                    return Integer.parseInt(value.trim());
                } catch (NumberFormatException _) {
                    return 3;
                }
            })
            .orElse(3);
    }

    /**
     * Récupère la configuration du modèle de réapprovisionnement et les options disponibles.
     *
     * @return Map contenant le modèle actuel et les options disponibles
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getModelReapproConfiguration() {
        Map<String, Object> response = new HashMap<>();

        // Récupère le modèle configuré
        String currentModel = appConfigurationRepository
            .findById(EntityConstant.APP_MODEL_REAPPRO)
            .map(AppConfiguration::getValue)
            .orElse(ModelReapprovisionnement.CLASSIQUE.name());

        // Liste des modèles disponibles avec leurs descriptions
        List<Map<String, String>> availableModels = Arrays.asList(
            Map.of(
                "value", ModelReapprovisionnement.CLASSIQUE.name(),
                "label", "Modèle Classique",
                "description", "Calcul basé sur la moyenne des ventes des 3 derniers mois"
            ),
            Map.of(
                "value", ModelReapprovisionnement.SEMOIS.name(),
                "label", "Modèle SEMOIS",
                "description", "Stock Économique Mensuel avec VMM pondéré, marge de sécurité et classe de criticité"
            )
        );

        response.put("currentModel", currentModel);
        response.put("availableModels", availableModels);

        return response;
    }

    /**
     * Met à jour le modèle de réapprovisionnement.
     *
     * @param model Le nouveau modèle (CLASSIQUE ou SEMOIS)
     * @throws IllegalArgumentException Si le modèle est invalide
     */
    @Transactional
    public void updateModelReappro(String model) {
        // Valide que le modèle est valide
        ModelReapprovisionnement.valueOf(model);

        // Met à jour ou crée la configuration
        Optional<AppConfiguration> configOpt = appConfigurationRepository.findById(EntityConstant.APP_MODEL_REAPPRO);

        if (configOpt.isPresent()) {
            AppConfiguration config = configOpt.get();
            config.setValue(model);
            config.setUpdated(LocalDateTime.now());
            config.setValidatedBy(userService.getUser());
            appConfigurationRepository.save(config);
        } else {
            // Crée la configuration si elle n'existe pas
            AppConfiguration newConfig = new AppConfiguration();
            newConfig.setName(EntityConstant.APP_MODEL_REAPPRO);
            newConfig.setDescription("Modèle de calcul du réapprovisionnement");
            newConfig.setValue(model);
            newConfig.setCreated(LocalDateTime.now());
            newConfig.setUpdated(LocalDateTime.now());
            newConfig.setValidatedBy(userService.getUser());
            appConfigurationRepository.save(newConfig);
        }
    }
}
