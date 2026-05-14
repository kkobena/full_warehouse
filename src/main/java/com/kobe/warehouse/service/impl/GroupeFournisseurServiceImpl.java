package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.DeliveryReceiptRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.util.Condition;
import com.kobe.warehouse.repository.util.SpecificationBuilder;
import com.kobe.warehouse.service.GroupeFournisseurService;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.projection.GroupeFournisseurAchat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gère les fournisseurs principaux (parent_id IS NULL) — remplace l'ancienne entité GroupeFournisseur.
 * Les endpoints REST /api/groupe-fournisseurs continuent de fonctionner via cette implémentation.
 */
@Service
@Transactional
public class GroupeFournisseurServiceImpl implements GroupeFournisseurService {

    private final Logger log = LoggerFactory.getLogger(GroupeFournisseurServiceImpl.class);
    private final FournisseurRepository fournisseurRepository;
    private final DeliveryReceiptRepository deliveryReceiptRepository;

    public GroupeFournisseurServiceImpl(FournisseurRepository fournisseurRepository, DeliveryReceiptRepository deliveryReceiptRepository) {
        this.fournisseurRepository = fournisseurRepository;
        this.deliveryReceiptRepository = deliveryReceiptRepository;
    }

    @Override
    public GroupeFournisseurDTO save(GroupeFournisseurDTO dto) {
        log.debug("Request to save GroupeFournisseur (as parent Fournisseur) : {}", dto);
        Fournisseur fournisseur = dto.getId() != null
            ? fournisseurRepository.getReferenceById(dto.getId())
            : new Fournisseur();
        fournisseur
            .libelle(dto.getLibelle())
            .addressePostal(dto.getAddresspostale())
            .numFaxe(dto.getNumFaxe())
            .setEmail(dto.getEmail())
            .phone(dto.getTel())
            .setOdre(Objects.requireNonNullElse(dto.getOdre(), 100))
            .setCodeOfficePharmaMl(dto.getCodeOfficePharmaMl())
            .setCodeRecepteurPharmaMl(dto.getCodeRecepteurPharmaMl())
            .setUrlPharmaMl(dto.getUrlPharmaMl())
            .setIdRecepteurPharmaMl(dto.getIdRecepteurPharmaMl())
            .setDelaiLivraisonJours(dto.getDelaiLivraisonJours())
            .setFrequenceCommandeJours(dto.getFrequenceCommandeJours())
            .setJoursCredit(dto.getJoursCredit())
            .setJoursCritique(dto.getJoursCritique())
            .setPalierRfa(dto.getPalierRfa())
            .setTauxRfa(dto.getTauxRfa());
        return new GroupeFournisseurDTO(fournisseurRepository.save(fournisseur));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroupeFournisseurDTO> findAll(String search, Pageable pageable) {
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "libelle"));
        if (StringUtils.hasLength(search)) {
            SpecificationBuilder<Fournisseur> builder = new SpecificationBuilder<>();
            Specification<Fournisseur> spec = builder
                .with(new String[] { "libelle" }, search + "%", Condition.OperationType.LIKE, Condition.LogicalOperatorType.END)
                .build();
            return fournisseurRepository.findByParentIsNull(spec, page).map(GroupeFournisseurDTO::new);
        }
        return fournisseurRepository.findByParentIsNull(page).map(GroupeFournisseurDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GroupeFournisseurDTO> findOne(Integer id) {
        return fournisseurRepository.findById(id).map(GroupeFournisseurDTO::new);
    }

    @Override
    public void delete(Integer id) {
        fournisseurRepository.deleteById(id);
    }

    @Override
    public ResponseDTO importation(InputStream inputStream) {
        AtomicInteger count = new AtomicInteger();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').build().parse(br);
            records.forEach(record -> {
                if (count.getAndIncrement() == 0) return;
                Fournisseur fournisseur = new Fournisseur();
                fournisseur.setLibelle(record.get(0));
                fournisseur.setOdre(count.get());
                fournisseurRepository.save(fournisseur);
            });
        } catch (IOException e) {
            log.error("importation : {}", e.getMessage(), e);
        }
        return new ResponseDTO().size(count.get());
    }

    @Override
    public List<GroupeFournisseurDTO> findTopNToDisplay() {
        return fournisseurRepository.findByParentIsNullOrderByOdreAsc().stream()
            .limit(5)
            .map(GroupeFournisseurDTO::new)
            .toList();
    }

    @Override
    public Page<GroupeFournisseurAchat> fetchAchats(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return deliveryReceiptRepository.fetchAchats(fromDate, toDate, OrderStatut.CLOSED, pageable);
    }
}
