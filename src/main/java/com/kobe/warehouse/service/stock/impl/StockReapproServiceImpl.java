package com.kobe.warehouse.service.stock.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.dto.ProduitQuantitySold;
import com.kobe.warehouse.service.utils.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StockReapproServiceImpl {

    private final Logger log = LoggerFactory.getLogger(StockReapproServiceImpl.class);
    private final EntityManager entityManager;
    private final AppConfigurationService appConfigurationService;
    private final TransactionTemplate transactionTemplate;
    private final SalesRepository salesRepository;
    private final ObjectMapper objectMapper;

    public StockReapproServiceImpl(
        EntityManager entityManager,
        AppConfigurationService appConfigurationService,
        TransactionTemplate transactionTemplate,
        SalesRepository salesRepository,
        ObjectMapper objectMapper
    ) {
        this.entityManager = entityManager;
        this.appConfigurationService = appConfigurationService;

        this.transactionTemplate = transactionTemplate;
        this.salesRepository = salesRepository;
        this.objectMapper = objectMapper;
    }

    //   @Scheduled(cron = "0 0 9-11 * * *")
    @Scheduled(cron = "0 0/30 * * * *")
    public void computeStockReapprovisionnement() {
        Optional<AppConfiguration> appConfiguration = getLastReapproDate();
        appConfiguration.ifPresent(configuration -> {
            boolean computeReappro = true;

            if (StringUtils.isNotEmpty(configuration.getValue())) {
                LocalDate date = LocalDate.parse(configuration.getValue());
                computeReappro = date.getMonthValue() != LocalDate.now().getMonthValue();
            }
            if (computeReappro) {
                LocalDateTime start = LocalDateTime.now();
                compute();
                configuration.setUpdated(LocalDateTime.now());
                configuration.setValue(LocalDate.now().toString());
                appConfigurationService.update(configuration);
                LocalDateTime end = LocalDateTime.now();
                long period = TimeUnit.SECONDS.convert(Duration.between(start, end));
                log.info("Temps passé en seconde {} app {}", period, appConfiguration);
            }
        });
    }

    private void compute() {
        final LocalDate lastMonth = DateUtil.getLastMonthFromNow();
        final LocalDate threeMonthAgo = DateUtil.getNthLastMonthFromNow(3);
        final int dailyNumberStock = Integer.parseInt(
            appConfigurationService
                .findOneById(EntityConstant.APP_DAY_STOCK)
                .map(AppConfiguration::getValue)
                .orElse(EntityConstant.APP_DAY_STOCK_DEFAULT_VALUE + "")
        );
        final int nbLimit = Integer.parseInt(
            appConfigurationService
                .findOneById(EntityConstant.APP_LIMIT_NBR_DAY_REAPPRO)
                .map(AppConfiguration::getValue)
                .orElse(EntityConstant.APP_LIMIT_NBR_DAY_REAPPRO_DEFAULT_VALUE + "")
        );
        final double denominateur = Double.parseDouble(
            appConfigurationService
                .findOneById(EntityConstant.APP_DENOMINATEUR_REAPPRO)
                .map(AppConfiguration::getValue)
                .orElse(EntityConstant.APP_DENOMINATEUR_REAPPRO_DEFAULT_VALUE + "")
        );
        fetchProductAndQuantitySold(threeMonthAgo, lastMonth)
            .parallelStream()
            .forEach(tuple -> computeProduitQtyReappro(tuple, dailyNumberStock, nbLimit, denominateur));
    }

    private List<ProduitQuantitySold> fetchProductAndQuantitySold(LocalDate threeMonthAgo, LocalDate lastMonth) {
        try {
            return objectMapper.readValue(salesRepository.fetchProductQuantitySold(threeMonthAgo, lastMonth), new TypeReference<>() {});
        } catch (Exception e) {
            log.info(e.getMessage());
            return Collections.emptyList();
        }
    }

    private void computeProduitQtyReappro(ProduitQuantitySold tuple, int dayStock, int delayReappro, double denominateurReappro) {
        long id = tuple.id();
        int qtySold = tuple.qtySold();
        int itemQtySold = tuple.itemQtySold();
        int itemQty = tuple.itemQty();
        if (itemQtySold > 0) {
            qtySold = qtySold + ((int) Math.ceil(itemQtySold / (double) itemQty));
        }
        double soldQuantityAvg = qtySold / denominateurReappro;
        int seuilMin = (int) Math.ceil(soldQuantityAvg * dayStock);
        int quantityReappro = (int) Math.ceil(soldQuantityAvg * delayReappro);
        updateProduit(id, seuilMin, quantityReappro);
    }

    private void updateProduit(long id, int seuilMin, int quantityReappro) {
        transactionTemplate.execute(
            new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                    CriteriaUpdate<Produit> update = cb.createCriteriaUpdate(Produit.class);
                    Root<Produit> root = update.from(Produit.class);
                    update.set(Produit_.qtyAppro, quantityReappro);
                    update.set(Produit_.qtySeuilMini, seuilMin);
                    update.set(Produit_.updatedAt, LocalDateTime.now());
                    update.where(cb.equal(root.get(Produit_.id), id));
                    entityManager.createQuery(update).executeUpdate();
                }
            }
        );
    }

    private Optional<AppConfiguration> getLastReapproDate() {
        return appConfigurationService.findOneById(EntityConstant.APP_LAST_DAY_REAPPRO);
    }
}
