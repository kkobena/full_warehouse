package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.ProductState;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import com.kobe.warehouse.repository.ProductStateRepository;
import com.kobe.warehouse.service.ProductStateService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductStateServiceImpl implements ProductStateService {

    private final ProductStateRepository productStateRepository;

    public ProductStateServiceImpl(ProductStateRepository productStateRepository) {
        this.productStateRepository = productStateRepository;
    }

    @Override
    public void removeByProduitAndState(Produit produit, ProductStateEnum state) {
        productStateRepository.findProductStateByStateAndProduitId(state, produit.getId()).forEach(this::remove);
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
    public List<ProductState> fetchByProduit(Produit produit) {
        return this.productStateRepository.findProductStateByProduitId(produit.getId());
    }

    @Override
    public List<ProductState> fetchByProduitAndState(Produit produit, ProductStateEnum state) {
        return this.productStateRepository.findProductStateByStateAndProduitId(state, produit.getId());
    }
}
