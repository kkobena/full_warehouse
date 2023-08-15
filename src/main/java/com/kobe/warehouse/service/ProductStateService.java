package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.ProductState;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import java.util.List;

public interface ProductStateService {
  void removeByProduitAndState(Produit produit, ProductStateEnum state);

  void remove(ProductState state);

  void addState(Produit produit, ProductStateEnum state);

  List<ProductState> fetchByProduit(Produit produit);
  List<ProductState> fetchByProduitAndState(Produit produit,ProductStateEnum state);
}
