package com.kobe.warehouse.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.*;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.ImportationRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.service.dto.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ImportationCustomer {
    private final Logger log = LoggerFactory.getLogger(ImportationCustomer.class);
    private final TiersPayantRepository tiersPayantRepository;
    private final StorageService storageService;
    private final AssuredCustomerRepository assuredCustomerRepository;
    private final UninsuredCustomerRepository uninsuredCustomerRepository;
    private final ImportationRepository importationRepository;
    private final TransactionTemplate transactionTemplate;

    public ImportationCustomer(TiersPayantRepository tiersPayantRepository, StorageService storageService, AssuredCustomerRepository assuredCustomerRepository, UninsuredCustomerRepository uninsuredCustomerRepository, ImportationRepository importationRepository, TransactionTemplate transactionTemplate) {
        this.tiersPayantRepository = tiersPayantRepository;
        this.storageService = storageService;
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
        this.importationRepository = importationRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public ResponseDTO updateStocFromJSON(InputStream input) throws IOException {
        ResponseDTO response=new ResponseDTO();
        ObjectMapper mapper = new ObjectMapper();
        User user=this.storageService.getUserFormImport();
        AtomicInteger errorSize = new AtomicInteger(0);
        AtomicInteger size = new AtomicInteger(0);
        List<CustomerDTO> list = mapper.readValue(input, new TypeReference<>() {
        });
        int totalSize = list.size();
        response.setTotalSize(totalSize);
        log.info("size===>> {}", list.size());
        transactionTemplate.setPropagationBehavior(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        Importation importation = importation(user);
        importation.setTotalZise(totalSize);
        saveImportation(importation);
        for (CustomerDTO p : list) {
            try {
                if (p instanceof AssuredCustomerDTO){
                    processImportation((AssuredCustomerDTO)p,  errorSize, size,importation);
                }else{
                    processImportationUninsuredCustomer((UninsuredCustomerDTO) p,errorSize, size,importation);
                }

            } catch (Exception e) {
                log.debug("updateStocFromJSON ===>> {}", e);
            }

        }
        response.setSize(size.get());
        updateImportation(errorSize.get(), size.get(),importation);
        return response;
    }
    private Importation current() {
        return importationRepository.findFirstByImportationTypeOrderByCreatedDesc(ImportationType.CLIENTS);
    }

    private void processImportation(final AssuredCustomerDTO p, AtomicInteger errorSize, AtomicInteger size, Importation importation) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                AssuredCustomer assuredCustomer = null;
                try {
                    assuredCustomer = fromExternalDto(p);
                    buildClientTiersPayantExternalFromDto(p.getTiersPayants(), assuredCustomer);
                    buidAyantDroit(p.getAyantDroits(), assuredCustomer);
                    assuredCustomerRepository.save(assuredCustomer);
                    size.incrementAndGet();
                } catch (Exception e) {
                    log.debug("processImportation ===>> {}", e);
                    errorSize.incrementAndGet();
                    importation.getLigneEnErreur().add(assuredCustomer);
                }
            }
        });
    }

    private void processImportationUninsuredCustomer(final UninsuredCustomerDTO p, AtomicInteger errorSize, AtomicInteger size, Importation importation) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                UninsuredCustomer uninsuredCustomer = null;
                try {
                    uninsuredCustomer = fromExternalUninsuredCustomerDto(p);
                    uninsuredCustomerRepository.save(uninsuredCustomer);
                    size.incrementAndGet();
                } catch (Exception e) {
                    log.debug("processImportation ===>> {}", e);
                    errorSize.incrementAndGet();
                    importation.getLigneEnErreur().add(uninsuredCustomer);
                }
            }
        });
    }

    private UninsuredCustomer fromExternalUninsuredCustomerDto(UninsuredCustomerDTO dto) {
        UninsuredCustomer uninsuredCustomer = new UninsuredCustomer();
        uninsuredCustomer.setFirstName(dto.getFirstName());
        uninsuredCustomer.setLastName(dto.getLastName());
        uninsuredCustomer.setEmail(StringUtils.isNotEmpty(dto.getEmail()) ? dto.getEmail() : null);
        uninsuredCustomer.setPhone(dto.getPhone());
        uninsuredCustomer.setCreatedAt(Instant.now());
        uninsuredCustomer.setUpdatedAt(uninsuredCustomer.getUpdatedAt());
        uninsuredCustomer.setStatus(Status.ENABLE);
        uninsuredCustomer.setTypeAssure(TypeAssure.PRINCIPAL);
        uninsuredCustomer.setCode(dto.getCode());
        return uninsuredCustomer;
    }

    private AssuredCustomer fromExternalDto(AssuredCustomerDTO dto) {
        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setDatNaiss(dto.getDatNaiss());
        assuredCustomer.setSexe(StringUtils.isNotEmpty(dto.getSexe()) ? dto.getSexe() : null);
        assuredCustomer.setFirstName(dto.getFirstName());
        assuredCustomer.setLastName(dto.getLastName());
        assuredCustomer.setEmail(StringUtils.isNotEmpty(dto.getEmail()) ? dto.getEmail() : null);
        assuredCustomer.setPhone(dto.getPhone());
        assuredCustomer.setCreatedAt(Instant.now());
        assuredCustomer.setUpdatedAt(assuredCustomer.getUpdatedAt());
        assuredCustomer.setStatus(Status.ENABLE);
        assuredCustomer.setTypeAssure(TypeAssure.PRINCIPAL);
        assuredCustomer.setNumAyantDroit(dto.getNumAyantDroit());
        assuredCustomer.setCode(dto.getCode());
        return assuredCustomer;
    }

    private void buidAyantDroit(final List<AssuredCustomerDTO> ayantDroits, final AssuredCustomer assuredCustomer) {
        ayantDroits.forEach(ayantDroitDTO -> {
            AssuredCustomer ayantDroit = fromExternalDto(ayantDroitDTO);
            ayantDroit.setTypeAssure(TypeAssure.AYANT_DROIT);
            ayantDroit.setAssurePrincipal(assuredCustomer);
            assuredCustomer.getAyantDroits().add(ayantDroit);
        });
    }

    private void buildClientTiersPayantExternalFromDto(final List<ClientTiersPayantDTO> dtos, final AssuredCustomer assuredCustomer) {
        dtos.forEach(c -> {
            ClientTiersPayant o = new ClientTiersPayant();
            o.setCreated(Instant.now());
            o.setTiersPayant(tiersPayantRepository.findOneByNameOrFullName(c.getTiersPayantName(), c.getTiersPayantFullName()).orElse(null));
            o.setNum(c.getNum());
            o.setPlafondConso(c.getPlafondConso());
            o.setPlafondJournalier(c.getPlafondJournalier());
            o.setPriorite(c.getPriorite());
            o.setTaux(c.getTaux());
            o.setStatut(TiersPayantStatut.ACTIF);
            o.setUpdated(o.getCreated());
            o.setAssuredCustomer(assuredCustomer);
            assuredCustomer.getClientTiersPayants().add(o);
        });
    }

    private Importation importation(User user) {
        Importation importation = new Importation();
        importation.setImportationStatus(ImportationStatus.PROCESSING);
        importation.setImportationType(ImportationType.CLIENTS);
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
    private void updateImportation(final int errorSize, final int size,Importation importation) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                        importation.setUpdated(Instant.now());
                        importation.setSize(size);
                        importation.setErrorSize(errorSize);
                        importation.setImportationStatus(errorSize > 0 ? ImportationStatus.COMPLETED_ERRORS : ImportationStatus.COMPLETED);
                        importationRepository.save(importation);

                } catch (Exception e) {
                    log.debug("saveImportation ===>> {}", e);

                }
            }
        });


    }
}
