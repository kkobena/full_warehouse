package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.service.dto.enumeration.InventoryExportSummaryEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.util.CollectionUtils;

public class StoreInventoryGroupExport {

    private final List<InventoryExportSummary> totaux = new ArrayList<>();
    private final List<InventoryExportSummary> totauxVente = new ArrayList<>();
    private final List<InventoryExportSummary> totauxEcart = new ArrayList<>();
    private final InventoryExportSummary achatAvant;
    private final InventoryExportSummary achatApres;
    private final InventoryExportSummary venteAvant;
    private final InventoryExportSummary venteApres;
    private final InventoryExportSummary achatEcart;
    private final InventoryExportSummary venteEcart;
    private long id;
    private String code;
    private String libelle;
    private List<StoreInventoryLineExport> items = new ArrayList<>();
    private long storageId;
    private String storageLibelle;
    private long inventoryAmountBegin;
    private long inventoryValueCostBegin;
    private long gapAmount;
    private long gapCostAmount;

    public StoreInventoryGroupExport() {
        achatAvant = new InventoryExportSummary();
        achatAvant.setName(InventoryExportSummaryEnum.ACHAT_AVANT);
        achatApres = new InventoryExportSummary();
        achatApres.setName(InventoryExportSummaryEnum.ACHAT_APRES);

        venteAvant = new InventoryExportSummary();
        venteAvant.setName(InventoryExportSummaryEnum.VENTE_AVANT);

        venteApres = new InventoryExportSummary();
        venteApres.setName(InventoryExportSummaryEnum.VENTE_APRES);

        achatEcart = new InventoryExportSummary();
        achatEcart.setName(InventoryExportSummaryEnum.ACHAT_ECART);

        venteEcart = new InventoryExportSummary();
        venteEcart.setName(InventoryExportSummaryEnum.VENTE_ECART);

        totaux.add(achatAvant);
        totaux.add(achatApres);

        totauxVente.add(venteAvant);
        totauxVente.add(venteApres);

        totauxEcart.add(achatEcart);
        totauxEcart.add(venteEcart);
    }

    public StoreInventoryGroupExport(
        long id,
        String code,
        String libelle,
        List<StoreInventoryLineExport> items,
        long storageId,
        String storageLibelle
    ) {
        this();
        this.id = id;
        this.code = code;
        this.libelle = libelle;
        this.items = items;
        this.storageId = storageId;
        this.storageLibelle = storageLibelle;
    }

    public String getRayonDisplayName() {
        return this.code + " " + this.libelle;
    }

    public List<InventoryExportSummary> getTotauxVente() {
        return totauxVente;
    }

    public List<InventoryExportSummary> getTotauxEcart() {
        return totauxEcart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StoreInventoryGroupExport that = (StoreInventoryGroupExport) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public List<InventoryExportSummary> getTotaux() {
        return totaux;
    }

    public void computeSummary(StoreInventoryLineExport e) {
        int quantityOnHand = Objects.nonNull(e.getQuantityOnHand()) ? e.getQuantityOnHand() : 0;
        int gap = Objects.nonNull(e.getGap()) ? e.getGap() : 0;
        int quantityInit = Objects.nonNull(e.getQuantityInit()) ? e.getQuantityInit() : 0;
        achatAvant.setValue(achatAvant.getValue() + ((long) e.getPrixAchat() * quantityInit));
        achatApres.setValue(achatApres.getValue() + ((long) e.getPrixAchat() * quantityOnHand));

        venteAvant.setValue(venteAvant.getValue() + ((long) e.getPrixUni() * quantityInit));
        venteApres.setValue(venteApres.getValue() + ((long) e.getPrixUni() * quantityOnHand));

        achatEcart.setValue(achatEcart.getValue() + ((long) e.getPrixAchat() * gap));
        venteEcart.setValue(venteEcart.getValue() + ((long) e.getPrixUni() * gap));
    }

    public long getStorageId() {
        if (!CollectionUtils.isEmpty(items)) {
            try {
                storageId = getItems().getFirst().getStorageId();
            } catch (Exception ignored) {}
        }
        return storageId;
    }

    public void setStorageId(long storageId) {
        this.storageId = storageId;
    }

    public String getStorageLibelle() {
        if (!CollectionUtils.isEmpty(items)) {
            storageLibelle = getItems().getFirst().getStorageLibelle();
        }
        return storageLibelle;
    }

    public void setStorageLibelle(String storageLibelle) {
        this.storageLibelle = storageLibelle;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public List<StoreInventoryLineExport> getItems() {
        return items;
    }

    public void setItems(List<StoreInventoryLineExport> items) {
        this.items = items;
    }

    public long getInventoryAmountBegin() {
        return inventoryAmountBegin;
    }

    public void setInventoryAmountBegin(long inventoryAmountBegin) {
        this.inventoryAmountBegin = inventoryAmountBegin;
    }

    public long getInventoryValueCostBegin() {
        return inventoryValueCostBegin;
    }

    public void setInventoryValueCostBegin(long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
    }

    public long getGapAmount() {
        return gapAmount;
    }

    public void setGapAmount(long gapAmount) {
        this.gapAmount = gapAmount;
    }

    public long getGapCostAmount() {
        return gapCostAmount;
    }

    public void setGapCostAmount(long gapCostAmount) {
        this.gapCostAmount = gapCostAmount;
    }

    public InventoryExportSummary getAchatAvant() {
        return achatAvant;
    }

    public InventoryExportSummary getAchatApres() {
        return achatApres;
    }

    public InventoryExportSummary getVenteAvant() {
        return venteAvant;
    }

    public InventoryExportSummary getVenteApres() {
        return venteApres;
    }

    public InventoryExportSummary getAchatEcart() {
        return achatEcart;
    }

    public InventoryExportSummary getVenteEcart() {
        return venteEcart;
    }
}
