package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.util.Condition;
import com.kobe.warehouse.repository.util.SpecificationBuilder;
import com.kobe.warehouse.service.FournisseurService;
import com.kobe.warehouse.service.dto.FournisseurDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class FournisseurServiceImpl implements FournisseurService {

    private final Logger log = LoggerFactory.getLogger(FournisseurServiceImpl.class);
    private final FournisseurRepository fournisseurRepository;

    public FournisseurServiceImpl(FournisseurRepository fournisseurRepository) {
        this.fournisseurRepository = fournisseurRepository;
    }

    @Override
    public FournisseurDTO save(FournisseurDTO fournisseurDTO) {
        log.debug("Request to save Fournisseur : {}", fournisseurDTO);
        Fournisseur fournisseur = new Fournisseur()
            .id(fournisseurDTO.getId())
            .libelle(fournisseurDTO.getLibelle())
            .addressePostal(fournisseurDTO.getAddressePostal())
            .code(fournisseurDTO.getCode())
            .mobile(fournisseurDTO.getMobile())
            .numFaxe(fournisseurDTO.getNumFaxe())
            .phone(fournisseurDTO.getPhone())
            .site(fournisseurDTO.getSite())
            .setEmail(fournisseurDTO.getEmail())
            .setOdre(Objects.requireNonNullElse(fournisseurDTO.getOdre(), 100))
            .setDelaiLivraisonJours(fournisseurDTO.getDelaiLivraisonJours())
            .setFrequenceCommandeJours(fournisseurDTO.getFrequenceCommandeJours())
            .setIdentifiantRepartiteur(fournisseurDTO.getIdentifiantRepartiteur())
            .setJoursCredit(fournisseurDTO.getJoursCredit())
            .setJoursCritique(fournisseurDTO.getJoursCritique())
            .setPalierRfa(fournisseurDTO.getPalierRfa())
            .setTauxRfa(fournisseurDTO.getTauxRfa())
            .setUrlPharmaMl(fournisseurDTO.getUrlPharmaMl())
            .setCodeOfficePharmaMl(fournisseurDTO.getCodeOfficePharmaMl())
            .setCodeRecepteurPharmaMl(fournisseurDTO.getCodeRecepteurPharmaMl())
            .setIdRecepteurPharmaMl(fournisseurDTO.getIdRecepteurPharmaMl());
        if (fournisseurDTO.getParentId() != null) {
            fournisseur.setParent(fournisseurRepository.getReferenceById(fournisseurDTO.getParentId()));
        }
        fournisseur = fournisseurRepository.save(fournisseur);
        return new FournisseurDTO(fournisseur);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FournisseurDTO> findAll(String search, Pageable pageable) {
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "libelle"));
        if (!StringUtils.isEmpty(search)) {
            SpecificationBuilder<Fournisseur> builder = new SpecificationBuilder<>();
            Specification<Fournisseur> spec = builder
                .with(new String[]{"libelle"}, search + "%", Condition.OperationType.LIKE, Condition.LogicalOperatorType.OR)
                .with(new String[]{"code"}, search + "%", Condition.OperationType.LIKE, Condition.LogicalOperatorType.END)
                .build();
            return fournisseurRepository.findAll(spec, page).map(FournisseurDTO::new);
        }
        return fournisseurRepository.findAll(page).map(FournisseurDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FournisseurDTO> findOne(Integer id) {
        return fournisseurRepository.findById(id).map(FournisseurDTO::new);
    }

    @Override
    public void delete(Integer id) {
        fournisseurRepository.deleteById(id);
    }

    @Override
    public ResponseDTO importation(InputStream inputStream) {
        AtomicInteger count = new AtomicInteger(0);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').build().parse(br);
            records.forEach(record -> {
                if (count.getAndIncrement() == 0) return;
                Fournisseur fournisseur = new Fournisseur();
                fournisseur.setLibelle(record.get(0));
                fournisseur.setCode(record.get(1));
                fournisseur.setAddressePostal(record.get(3));
                fournisseur.setMobile(record.get(4));
                fournisseur.setPhone(record.get(5));
                fournisseur.setSite(record.get(6));
                fournisseur.setIdentifiantRepartiteur(record.get(7));
                String parentLibelle = record.get(2);
                if (!StringUtils.isEmpty(parentLibelle)) {
                    fournisseurRepository.findFirstByLibelleEqualsAndParentIsNull(parentLibelle)
                        .or(() -> fournisseurRepository.findFirstByLibelleEqualsAndParentIsNull(EntityConstant.AUTRES_FOURNISSEURS))
                        .ifPresent(fournisseur::setParent);
                }
                fournisseurRepository.save(fournisseur);
            });
        } catch (IOException e) {
            log.error("importation : ", e);
        }
        return new ResponseDTO().size(count.get());
    }

    @Override
    public Fournisseur findOneById(Integer id) {
        return this.fournisseurRepository.getReferenceById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FournisseurDTO> findParents() {
        return fournisseurRepository.findByParentIsNullOrderByOdreAsc().stream().map(FournisseurDTO::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FournisseurDTO> findParents(String search, Pageable pageable) {
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "odre", "libelle"));
        if (!StringUtils.isEmpty(search)) {
            SpecificationBuilder<Fournisseur> builder = new SpecificationBuilder<>();
            Specification<Fournisseur> spec = builder
                .with(new String[]{"libelle"}, search + "%", Condition.OperationType.LIKE, Condition.LogicalOperatorType.END)
                .build();
            return fournisseurRepository.findByParentIsNull(spec, page).map(FournisseurDTO::new);
        }
        return fournisseurRepository.findByParentIsNull(page).map(FournisseurDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FournisseurDTO> findAgences(Integer parentId) {
        return fournisseurRepository.findByParentId(parentId).stream().map(FournisseurDTO::new).toList();
    }

    @Override
    public Optional<Fournisseur> getParentByChildId(Integer childId) {
        return fournisseurRepository.getParentByChildId(childId);
    }

    @Override
    public Optional<Integer> getParentIdByChildId(Integer childId) {
        return fournisseurRepository.getParentIdByChildId(childId);
    }
}
