package com.kobe.warehouse.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.Importation;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.ImportationStatus;
import com.kobe.warehouse.domain.enumeration.ImportationType;
import com.kobe.warehouse.repository.ImportationRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.service.dto.TiersPayantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ImportationTiersPayantService implements TiersPayantMapper {
    private final Logger log = LoggerFactory.getLogger(ImportationTiersPayantService.class);
   private final StorageService storageService;
    private final GroupeTiersPayantService groupeTiersPayantService;
    private final TiersPayantRepository tiersPayantRepository;
    private final ImportationRepository importationRepository;
    private final TransactionTemplate transactionTemplate;

    public ImportationTiersPayantService(StorageService storageService, GroupeTiersPayantService groupeTiersPayantService, TiersPayantRepository tiersPayantRepository, ImportationRepository importationRepository, TransactionTemplate transactionTemplate) {
        this.storageService = storageService;
        this.groupeTiersPayantService = groupeTiersPayantService;
        this.tiersPayantRepository = tiersPayantRepository;
        this.importationRepository = importationRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public ResponseDTO  updateStocFromJSON(InputStream input) throws IOException {
        ResponseDTO response=new ResponseDTO();
        ObjectMapper mapper = new ObjectMapper();
       User user=this.storageService.getUserFormImport();
        AtomicInteger errorSize = new AtomicInteger(0);
        AtomicInteger size = new AtomicInteger(0);
        List<TiersPayantDto> list = mapper.readValue(input, new TypeReference<>() {
        });
        int totalSize = list.size();
        response.setTotalSize(totalSize);
        log.info("size===>> {}", list.size());
        transactionTemplate.setPropagationBehavior(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        Importation importation = importation(user);
        importation.setTotalZise(totalSize);
        saveImportation(importation);
        for (TiersPayantDto p : list) {
            try {
                processImportation(p,  errorSize, size,user);
                updateImportation(errorSize.get(), size.get());
            } catch (Exception e) {
                log.debug("updateStocFromJSON ===>> {}", e);
            }

        }
        response.setSize(size.get());
        updateImportation(errorSize.get(), size.get());
        return response;
    }
    private void updateImportation(final int errorSize, final int size) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    Importation importation = importationRepository.findFirstByImportationTypeOrderByCreatedDesc(ImportationType.TIERS_PAYANT);
                    if (importation != null) {
                        importation.setUpdated(Instant.now());
                        importation.setSize(size);
                        importation.setErrorSize(errorSize);
                        importation.setImportationStatus(errorSize > 0 ? ImportationStatus.COMPLETED_ERRORS : ImportationStatus.COMPLETED);
                        importationRepository.save(importation);
                    }
                } catch (Exception e) {
                    log.debug("saveImportation ===>> {}", e);

                }
            }
        });


    }
    public Importation current(ImportationType importationType) {
        return importationRepository.findFirstByImportationTypeOrderByCreatedDesc(importationType);
    }
  private   void processImportation(final TiersPayantDto p,  AtomicInteger errorSize, AtomicInteger size,User user) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    TiersPayant tiersPayant = entityFromDto(p);
                    tiersPayant.setUpdatedBy(user);
                    GroupeTiersPayant groupeTiersPayant=groupeTiersPayantService.getOneByName(p.getGroupeTiersPayantName()).orElse(null);
                    tiersPayant.setGroupeTiersPayant(groupeTiersPayant);
                    tiersPayantRepository.save(tiersPayant);
                    size.incrementAndGet();
                }catch (Exception e){
                    log.debug("processImportation ===>> {}", e);
                    errorSize.incrementAndGet();
                }


            }
        });
    }

    private Importation importation(User user) {
        Importation importation = new Importation();
        importation.setImportationStatus(ImportationStatus.PROCESSING);
        importation.setImportationType(ImportationType.TIERS_PAYANT);
        importation.setCreated(Instant.now());
        importation.setUser(user);
        return importation;
    }

    private void saveImportation(Importation importation) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    importationRepository.save(importation);
                } catch (Exception e) {
                    log.debug("saveImportation ===>> {}", e);

                }
            }
        });
    }
}
