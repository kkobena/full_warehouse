package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.ProductState;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import com.kobe.warehouse.repository.ProductStateRepository;
import com.kobe.warehouse.service.ProductStateService;
import com.kobe.warehouse.service.dto.ProductStateEnumProjection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductStateServiceImpl implements ProductStateService {

    private final ProductStateRepository productStateRepository;

    public ProductStateServiceImpl(ProductStateRepository productStateRepository) {
        this.productStateRepository = productStateRepository;
    }

    @Override
    public void removeByProduitAndState(Produit produit, ProductStateEnum state) {
        productStateRepository.removeProductStateByProduitIdAndState(produit.getId(), state);
    }

    @Override
    public void remove(ProductState state) {
        productStateRepository.delete(state);
    }

    @Override
    public void addState(Produit produit, ProductStateEnum state) {
        ProductState productState = new ProductState();
        productState.setProduit(produit);
        productState.setState(state);
        this.productStateRepository.save(productState);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductState> fetchByProduit(Produit produit) {
        return this.productStateRepository.findProductStateByProduitId(produit.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductState> fetchByProduitAndState(Produit produit, ProductStateEnum state) {
        return this.productStateRepository.findProductStateByStateAndProduitId(state, produit.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByStateAndProduitId(ProductStateEnum productStateEnum, Long produitId) {
        return this.productStateRepository.existsByStateAndProduitId(productStateEnum, produitId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<ProductStateEnum> getProductStateByProduitId(Long produitId) {
        return this.productStateRepository.findDistinctByProduitId(produitId)
            .stream()
            .map(ProductStateEnumProjection::getState)
            .collect(Collectors.toSet());
    }
}
