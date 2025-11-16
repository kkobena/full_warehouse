package com.kobe.warehouse.service.mvt_produit.builder;

import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Decondition;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.ProductsToDestroy;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourBonItem;
import com.kobe.warehouse.domain.RetourDepotItem;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import java.time.LocalDateTime;

public class InventoryTransactionBuilder {

    private InventoryTransaction inventoryTransaction;

    public InventoryTransactionBuilder(Object entity) {
        if (entity instanceof SalesLine salesLine) {
            inventoryTransaction = new InventoryTransaction()
                .setCreatedAt(salesLine.getUpdatedAt())
                .setProduit(salesLine.getProduit())
                .setMouvementType(salesLine.getQuantitySold() < 0 ? MouvementProduit.CANCEL_SALE : MouvementProduit.SALE)
                .setQuantity(salesLine.getQuantitySold())
                .setQuantityBefor(salesLine.getInitStock())
                .setQuantityAfter(salesLine.getAfterStock())
                .setCostAmount(salesLine.getCostAmount())
                .setEntityId(salesLine.getId().getId())
                .setUser(salesLine.getSales().getUser())
                .setMagasin(salesLine.getSales().getUser().getMagasin())
                .setRegularUnitPrice(salesLine.getRegularUnitPrice());
        } else if (entity instanceof OrderLine orderLine) {
            AppUser appUser = orderLine.getCommande().getUser();
            inventoryTransaction = new InventoryTransaction()
                .setCreatedAt(orderLine.getUpdatedAt())
                .setProduit(orderLine.getFournisseurProduit().getProduit())
                .setMouvementType(MouvementProduit.ENTREE_STOCK)
                .setQuantity(orderLine.getQuantityReceived())
                .setQuantityBefor(orderLine.getInitStock())
                .setQuantityAfter(orderLine.getFinalStock())
                .setCostAmount(orderLine.getOrderCostAmount())
                .setEntityId(Long.parseLong(orderLine.getId().getId() + ""))
                .setUser(appUser)
                .setMagasin(appUser.getMagasin())
                .setRegularUnitPrice(orderLine.getOrderUnitPrice());
        } else if (entity instanceof Ajustement ajustement) {
            Produit produit = ajustement.getProduit();
            FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
            inventoryTransaction = new InventoryTransaction()
                .setCreatedAt(ajustement.getDateMtv())
                .setProduit(produit)
                .setMouvementType(ajustement.getQtyMvt() > 0 ? MouvementProduit.AJUSTEMENT_IN : MouvementProduit.AJUSTEMENT_OUT)
                .setQuantity(ajustement.getQtyMvt())
                .setQuantityBefor(ajustement.getStockBefore())
                .setQuantityAfter(ajustement.getStockAfter())
                .setCostAmount(fournisseurProduit.getPrixAchat())
                .setEntityId(Long.parseLong(ajustement.getId() + ""))
                .setUser(ajustement.getAjust().getUser())
                .setMagasin(ajustement.getAjust().getUser().getMagasin())
                .setRegularUnitPrice(fournisseurProduit.getPrixUni());
        } else if (entity instanceof Decondition decondition) {
            Produit produit = decondition.getProduit();
            FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
            inventoryTransaction = new InventoryTransaction()
                .setCreatedAt(decondition.getDateMtv())
                .setProduit(produit)
                .setMouvementType(
                    TypeDeconditionnement.DECONDTION_IN == decondition.getTypeDeconditionnement()
                        ? MouvementProduit.DECONDTION_IN
                        : MouvementProduit.DECONDTION_OUT
                )
                .setQuantity(decondition.getQtyMvt())
                .setQuantityBefor(decondition.getStockBefore())
                .setQuantityAfter(decondition.getStockAfter())
                .setUser(decondition.getUser())
                .setMagasin(decondition.getUser().getMagasin())
                .setCostAmount(fournisseurProduit.getPrixAchat())
                .setEntityId(Long.parseLong(decondition.getId() + ""))
                .setRegularUnitPrice(fournisseurProduit.getPrixUni());
        } else if (entity instanceof ProductsToDestroy productsToDestroy) {
            FournisseurProduit fournisseurProduit = productsToDestroy.getFournisseurProduit();
            Produit produit = fournisseurProduit.getProduit();
            inventoryTransaction = new InventoryTransaction()
                .setCreatedAt(productsToDestroy.getUpdated())
                .setProduit(produit)
                .setMouvementType(MouvementProduit.RETRAIT_PERIME)
                .setQuantity(productsToDestroy.getQuantity())
                .setQuantityBefor(productsToDestroy.getStockInitial())
                .setQuantityAfter(productsToDestroy.getStockInitial() - productsToDestroy.getQuantity())
                .setCostAmount(productsToDestroy.getPrixAchat())
                .setEntityId(Long.parseLong(productsToDestroy.getId() + ""))
                .setUser(productsToDestroy.getUser())
                .setMagasin(productsToDestroy.getMagasin())
                .setRegularUnitPrice(productsToDestroy.getPrixUnit());
        } else if (entity instanceof RetourBonItem retourBonItem) {
            OrderLine orderLine = retourBonItem.getOrderLine();
            Produit produit = orderLine.getFournisseurProduit().getProduit();
            inventoryTransaction = new InventoryTransaction()
                .setCreatedAt(retourBonItem.getDateMtv())
                .setProduit(produit)
                .setMouvementType(MouvementProduit.RETOUR_FOURNISSEUR)
                .setQuantity(retourBonItem.getQtyMvt())
                .setQuantityBefor(retourBonItem.getInitStock())
                .setQuantityAfter(retourBonItem.getAfterStock())
                .setCostAmount(orderLine.getOrderCostAmount())
                .setEntityId(Long.parseLong(retourBonItem.getId() + ""))
                .setUser(retourBonItem.getRetourBon().getUser())
                .setMagasin(retourBonItem.getRetourBon().getUser().getMagasin())
                .setRegularUnitPrice(orderLine.getOrderUnitPrice());
        } else if (entity instanceof StoreInventoryLine storeInventoryLine) {
            Produit produit = storeInventoryLine.getProduit();
            StoreInventory storeInventory = storeInventoryLine.getStoreInventory();
            AppUser user = storeInventory.getUser();
            inventoryTransaction = new InventoryTransaction()
                .setCreatedAt(storeInventoryLine.getUpdatedAt())
                .setProduit(produit)
                .setMouvementType(MouvementProduit.INVENTAIRE)
                .setQuantity(storeInventoryLine.getQuantityOnHand())
                .setQuantityBefor(storeInventoryLine.getQuantityInit())
                .setQuantityAfter(storeInventoryLine.getQuantityOnHand())
                .setCostAmount(storeInventoryLine.getInventoryValueCost())
                .setEntityId(storeInventoryLine.getId())
                .setUser(user)
                .setMagasin(user.getMagasin())
                .setRegularUnitPrice(storeInventoryLine.getLastUnitPrice());
        } else if (entity instanceof RetourDepotItem retourDepotItem) {
            Produit produit = retourDepotItem.getProduit();
            AppUser user = retourDepotItem.getRetourDepot().getUser();
            inventoryTransaction = new InventoryTransaction()
                .setCreatedAt(LocalDateTime.now())
                .setProduit(produit)
                .setMouvementType(MouvementProduit.RETOUR_DEPOT)
                .setQuantity(retourDepotItem.getQtyMvt())
                .setQuantityBefor(retourDepotItem.getOfficineInitStock())
                .setQuantityAfter(retourDepotItem.getOfficineFinalStock())
                .setCostAmount(retourDepotItem.getPrixAchat())
                .setEntityId(Long.parseLong(retourDepotItem.getId() + ""))
                .setUser(user)
                .setMagasin(user.getMagasin())
                .setRegularUnitPrice(retourDepotItem.getRegularUnitPrice());
        }
    }

    public InventoryTransaction build() {
        return this.inventoryTransaction;
    }
}
