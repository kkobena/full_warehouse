package com.kobe.warehouse.service.settings;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.enumeration.ParametreValueType;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.AcceptationSubstitutionMode;
import com.kobe.warehouse.domain.enumeration.ModelReapprovisionnement;
import com.kobe.warehouse.domain.enumeration.PutawayMode;
import com.kobe.warehouse.repository.AppConfigurationRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.settings.dto.AppConfigurationDto;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Transactional
public class AppConfigurationService {

    private static final BigDecimal PLAFOND_PONCTION_DEFAUT = new BigDecimal("35");
    private static final BigDecimal CENT = new BigDecimal("100");

    private static final Logger LOG = LoggerFactory.getLogger(AppConfigurationService.class);

    private final UserService userService;
    private final AppConfigurationRepository appConfigurationRepository;
    /**
     * Optionnel : {@code pharmaSmart-batch} embarque ce service sans activer le cache. L'exiger
     * empêcherait le batch de démarrer, et il n'aurait de toute façon rien à vider.
     */
    private final ObjectProvider<CacheManager> cacheManager;

    public AppConfigurationService(
        UserService userService,
        AppConfigurationRepository appConfigurationRepository,
        ObjectProvider<CacheManager> cacheManager
    ) {
        this.userService = userService;
        this.appConfigurationRepository = appConfigurationRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_MONO_STOCK)
    public boolean isMono() {
        Optional<AppConfiguration> appConfiguration = appConfigurationRepository.findById(EntityConstant.APP_MONO_STOCK);
        return appConfiguration.map(configuration -> Integer.parseInt(configuration.getValue().trim()) == 0).orElse(true);
    }

    @Transactional(readOnly = true)
    public Optional<AppConfigurationDto> findOne(String id) {
        return findOneById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<AppConfiguration> findOneById(String id) {
        return appConfigurationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_GESTION_STOCK)
    public Optional<AppConfigurationDto> findStockParam() {
        return appConfigurationRepository.findById(EntityConstant.APP_GESTION_STOCK).map(this::toDto);
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

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_GESTION_LOT_INVENTAIRE_CACHE)
    public boolean useGestionLotInventaire() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_GESTION_LOT_INVENTAIRE)
            .map(configuration -> {
                try {
                    return Integer.parseInt(configuration.getValue().trim()) == 1;
                } catch (NumberFormatException _) {
                    return false;
                }
            })
            .orElse(false);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_MODE_SAISIE_LOT_INVENTAIRE_CACHE)
    public String getModeSaisieLotInventaire() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_MODE_SAISIE_LOT_INVENTAIRE)
            .map(AppConfiguration::getValue)
            .orElse("LOT_PLAT");
    }

    @Transactional
    public void update(AppConfigurationDto appConfiguration) {
        appConfigurationRepository
            .findById(appConfiguration.name())
            .map(configuration -> {
                configuration.setValue(appConfiguration.value());
                configuration.setDescription(appConfiguration.description());
                configuration.setUpdated(LocalDateTime.now());
                configuration.setValidatedBy(userService.getUser());
                return appConfigurationRepository.save(configuration);
            })
            .ifPresent(configuration -> eviderLeCache(configuration.getName()));
    }


    @Transactional(readOnly = true)
    public Page<AppConfigurationDto> fetchAll(String search, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
            ? pageable
            : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "description"));
        return appConfigurationRepository.findAll(AppConfigurationRepository.buildSpec(search), sorted)
            .map(this::toDto);
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
            .orElse(90);
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

    /**
     * Active ou non l'exclusion des unités gratuites du chiffre d'affaires à déclarer.
     *
     * <p>L'éviction est indispensable : {@link #excludeFreeUnit()} est {@code @Cacheable}, et sans
     * elle le basculement resterait sans effet jusqu'au redémarrage.
     */
    @Transactional
    public void setExcludeFreeUnit(boolean exclure) {
        AppConfiguration configuration = appConfigurationRepository
            .findById(EntityConstant.EXCLUDE_FREE_UNIT)
            .orElseGet(() -> {
                AppConfiguration nouvelle = new AppConfiguration();
                nouvelle.setName(EntityConstant.EXCLUDE_FREE_UNIT);
                nouvelle.setDescription("Exclure les unités gratuites du chiffre d'affaires à déclarer");
                nouvelle.setCreated(LocalDateTime.now());
                nouvelle.setValueType(ParametreValueType.BOOLEAN);
                return nouvelle;
            });
        configuration.setValue(Boolean.toString(exclure));
        configuration.setUpdated(LocalDateTime.now());
        configuration.setValidatedBy(userService.getUser());
        appConfigurationRepository.save(configuration);
        eviderLeCache(EntityConstant.EXCLUDE_FREE_UNIT);
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
        try {
            return appConfigurationRepository
                .findById(EntityConstant.APP_CUSTOMER_DISPLAY)
                .map(configuration -> Integer.parseInt(configuration.getValue().trim()) == 0)
                .orElse(false);
        } catch (Exception e) {
            return false;
        }

    }

    /**
     * Délai, en jours, pendant lequel une ponction validée reste annulable.
     *
     * <p>Un jour par défaut : au-delà, les chiffres ont eu le temps d'être lus, imprimés ou
     * transmis, et les défaire rendrait un état déjà sorti impossible à reproduire.
     */
    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_PONCTION_ANNULATION_MAX_DAYS_CACHE)
    public int getPonctionAnnulationMaxDays() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_PONCTION_ANNULATION_MAX_DAYS)
            .map(AppConfiguration::getValue)
            .map(valeur -> {
                try {
                    return Integer.parseInt(valeur.trim());
                } catch (NumberFormatException _) {
                    return 1;
                }
            })
            .orElse(1);
    }

    /** Plafond par defaut d'une ponction, en pourcentage du montant d'une vente. */
    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_PONCTION_PLAFOND_DEFAUT_CACHE)
    public BigDecimal getPonctionPlafondDefaut() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_PONCTION_PLAFOND_DEFAUT)
            .map(AppConfiguration::getValue)
            .map(valeur -> {
                try {
                    return new BigDecimal(valeur.trim());
                } catch (NumberFormatException _) {
                    return PLAFOND_PONCTION_DEFAUT;
                }
            })
            // Hors des bornes, la valeur saisie ne veut rien dire : on retombe sur le defaut.
            .filter(plafond -> plafond.signum() > 0 && plafond.compareTo(CENT) <= 0)
            .orElse(PLAFOND_PONCTION_DEFAUT);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_CANCEL_SALE_MAX_DAYS_CACHE)
    public int getCancelSaleMaxDays() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_CANCEL_SALE_MAX_DAYS)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return Integer.parseInt(v.trim());
                } catch (NumberFormatException _) {
                    return 30;
                }
            })
            .orElse(30);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_SEUIL_VARIATION_PRIX_CACHE)
    public int getSeuilVariationPrix() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_SEUIL_VARIATION_PRIX)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return Integer.parseInt(v.trim());
                } catch (NumberFormatException _) {
                    return 20;
                }
            })
            .orElse(20);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_RECEPTION_MIN_EXPIRY_DAYS_CACHE)
    public long getReceptionMinExpiryDays() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_RECEPTION_MIN_EXPIRY_DAYS)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return Integer.parseInt(v.trim());
                } catch (NumberFormatException _) {
                    return 90;
                }
            })
            .orElse(90);
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

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_COUVERTURE_MOIS_CLASSIQUE_CACHE)
    public int getCouvertureMoisClassique() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_COUVERTURE_MOIS_CLASSIQUE)
            .map(AppConfiguration::getValue)
            .map(value -> {
                try {
                    return Integer.parseInt(value.trim());
                } catch (NumberFormatException _) {
                    return 2;
                }
            })
            .orElse(2);
    }

    /**
     * Retourne le mode d'acceptation des substitutions PharmaML EP.
     * Défaut : {@link AcceptationSubstitutionMode#MANUEL}.
     */
    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_ACCEPTATION_SUBSTITUTION_CACHE)
    public AcceptationSubstitutionMode getAcceptationSubstitutionMode() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_ACCEPTATION_SUBSTITUTION)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return AcceptationSubstitutionMode.valueOf(v.trim());
                } catch (IllegalArgumentException e) {
                    return AcceptationSubstitutionMode.MANUEL;
                }
            })
            .orElse(AcceptationSubstitutionMode.MANUEL);
    }

    /**
     * Retourne le mode de rangement automatique configuré pour la réception.
     * Défaut : {@link PutawayMode#MANUAL}.
     */
    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_PUTAWAY_MODE_CACHE)
    public PutawayMode getPutawayMode() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_PUTAWAY_MODE)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return PutawayMode.valueOf(v.trim());
                } catch (IllegalArgumentException e) {
                    return PutawayMode.MANUAL;
                }
            })
            .orElse(PutawayMode.MANUAL);
    }

    /**
     * Retourne le modèle de réapprovisionnement actuellement configuré.
     */
    @Transactional(readOnly = true)
    public ModelReapprovisionnement getCurrentModelReappro() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_MODEL_REAPPRO)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return ModelReapprovisionnement.valueOf(v);
                } catch (IllegalArgumentException e) {
                    return ModelReapprovisionnement.CLASSIQUE;
                }
            })
            .orElse(ModelReapprovisionnement.CLASSIQUE);
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
        eviderLeCache(EntityConstant.APP_MODEL_REAPPRO);
    }


    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_DELAI_RETOUR_FOURNISSEUR_CACHE)
    public int getDelaiRetourFournisseur() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_DELAI_RETOUR_FOURNISSEUR)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return Integer.parseInt(v.trim());
                } catch (NumberFormatException _) {
                    return 365;
                }
            })
            .orElse(365);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_AP_DEFAULT_CREDIT_DAYS_CACHE)
    public int getApDefaultCreditDays() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_AP_DEFAULT_CREDIT_DAYS)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return Integer.parseInt(v.trim());
                } catch (NumberFormatException _) {
                    return 30;
                }
            })
            .orElse(30);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_AP_DEFAULT_CRITIQUE_DAYS_CACHE)
    public int getApDefaultCritiqueDays() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_AP_DEFAULT_CRITIQUE_DAYS)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return Integer.parseInt(v.trim());
                } catch (NumberFormatException _) {
                    return 30;
                }
            })
            .orElse(30);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_NOTIF_AVOIR_EMAIL_ENABLED_CACHE)
    public boolean isNotifAvoirEmailEnabled() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_NOTIF_AVOIR_EMAIL_ENABLED)
            .map(c -> "1".equals(c.getValue().trim()))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_NOTIF_AVOIR_SMS_ENABLED_CACHE)
    public boolean isNotifAvoirSmsEnabled() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_NOTIF_AVOIR_SMS_ENABLED)
            .map(c -> "1".equals(c.getValue().trim()))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_DELAI_RETOUR_CLIENT_CACHE)
    public int getDelaiRetourClient() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_DELAI_RETOUR_CLIENT)
            .map(c -> {
                try {
                    return Integer.parseInt(c.getValue().trim());
                } catch (NumberFormatException e) {
                    return 30;
                }
            })
            .orElse(30);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_DELAI_VALIDITE_AVOIR_CACHE)
    public int getDelaiValiditeAvoir() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_DELAI_VALIDITE_AVOIR)
            .map(c -> {
                try {
                    return Integer.parseInt(c.getValue().trim());
                } catch (NumberFormatException e) {
                    return 90;
                }
            })
            .orElse(90);
    }

    @Transactional(readOnly = true)
    @Cacheable(EntityConstant.APP_DELAI_REGLEMENT_FACTURE)
    public int getDelaiReglement() {
        return appConfigurationRepository
            .findById(EntityConstant.APP_DELAI_REGLEMENT_FACTURE)
            .map(AppConfiguration::getValue)
            .map(v -> {
                try {
                    return Integer.parseInt(v.trim());
                } catch (NumberFormatException _) {
                    return 30;
                }
            })
            .orElse(30);
    }
    @Transactional
    public void update(AppConfiguration appConfiguration) {
        appConfigurationRepository.save(appConfiguration);
        eviderLeCache(appConfiguration.getName());
    }
    /**
     * Vide le cache d'un paramètre qui vient d'être écrit.
     *
     * <p>Sans cela, tous les lecteurs {@code @Cacheable} de cette classe continuent de servir
     * l'ancienne valeur pendant 24 heures : le paramètre passe à l'écran, et rien ne change dans
     * l'application.
     *
     * <p>Deux conventions de nommage cohabitent — le cache porte soit le nom du paramètre
     * ({@code EXCLUDE_FREE_UNIT}), soit ce nom suffixé ({@code APP_SEUIL_VARIATION_PRIX_CACHE}).
     * Les deux sont tentées, ce qui évite d'entretenir une table de correspondance qu'on oublierait
     * de compléter en ajoutant un paramètre.
     *
     * <p>Programmatique et non {@code @CacheEvict} : l'annotation passe par le proxy, donc reste
     * sans effet dès qu'une méthode de cette classe en appelle une autre. Ici l'appel direct
     * fonctionne dans tous les cas.
     */
    private void eviderLeCache(String nomParametre) {
        CacheManager gestionnaire = cacheManager.getIfAvailable();
        if (gestionnaire == null) {
            return;
        }
        List<Cache> caches = Stream
            .of(nomParametre, nomParametre + "_CACHE")
            .map(gestionnaire::getCache)
            .filter(Objects::nonNull)
            .toList();

        if (caches.isEmpty()) {
            // Un paramètre sans cache est le cas courant ; en journaliser la trace permet de
            // distinguer « rien à vider » de « cache mal nommé, donc jamais vidé ».
            LOG.debug("Aucun cache à vider pour le paramètre {}", nomParametre);
            return;
        }

        // Après validation de la transaction : vider avant laisserait une lecture concurrente
        // repeupler le cache avec la valeur d'avant l'écriture.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        caches.forEach(Cache::clear);
                    }
                }
            );
        } else {
            caches.forEach(Cache::clear);
        }
    }

    private AppConfigurationDto toDto(AppConfiguration appConfiguration) {
        return new AppConfigurationDto(
            appConfiguration.getName(),
            appConfiguration.getDescription(),
            appConfiguration.getValue(),
            appConfiguration.getCreated(),
            appConfiguration.getUpdated(),
            appConfiguration.getValueType(),
            appConfiguration.getOtherValue(),
            appConfiguration.getOptions()
        );
    }
}
