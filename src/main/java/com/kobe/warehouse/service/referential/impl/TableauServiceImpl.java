package com.kobe.warehouse.service.referential.impl;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Tableau;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.TableauRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.dto.TableauDTO;
import com.kobe.warehouse.service.referential.TableauService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TableauServiceImpl implements TableauService {

    private final TableauRepository tableauRepository;
    private final ProduitRepository produitRepository;
    private final LogsService logsService;

    public TableauServiceImpl(TableauRepository tableauRepository, ProduitRepository produitRepository, LogsService logsService) {
        this.tableauRepository = tableauRepository;
        this.produitRepository = produitRepository;
        this.logsService = logsService;
    }

    @Override
    @Transactional
    public TableauDTO save(TableauDTO tableauDTO) {
        return Optional.of(
            this.tableauRepository.saveAndFlush(
                    new Tableau().setId(tableauDTO.getId()).setCode(tableauDTO.getCode()).setValue(tableauDTO.getValue())
                )
        )
            .map(TableauDTO::new)
            .orElseThrow();
    }

    @Override
    public List<TableauDTO> findAll() {
        return this.tableauRepository.findAll().stream().map(TableauDTO::new).toList();
    }

    @Override
    public Optional<TableauDTO> findOne(Integer id) {
        return this.tableauRepository.findById(id).map(TableauDTO::new);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        this.tableauRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void associer(Integer id, List<Integer> produitIds) {
        Tableau tableau = this.tableauRepository.getReferenceById(id);

        produitIds.forEach(p -> {
            Produit produit = this.produitRepository.getReferenceById(p);
            try {
                produit.setTableau(tableau);
                produit.setUpdatedAt(LocalDateTime.now());
                this.produitRepository.save(produit);
                logsService.create(
                    TransactionType.UPDATE_PRODUCT,
                    String.format("Modification du produit %s", produit.getLibelle()),
                    produit.getId().toString()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    @Transactional
    public void dissocier(List<Integer> produitIds) {
        produitIds.forEach(p -> {
            Produit produit = this.produitRepository.getReferenceById(p);
            try {
                produit.setTableau(null);
                produit.setUpdatedAt(LocalDateTime.now());
                this.produitRepository.save(produit);
                logsService.create(
                    TransactionType.UPDATE_PRODUCT,
                    String.format("Modification du produit %s", produit.getLibelle()),
                    produit.getId().toString()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
