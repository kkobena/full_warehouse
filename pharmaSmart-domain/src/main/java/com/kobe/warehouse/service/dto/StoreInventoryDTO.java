package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.AppUserNames;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.domain.enumeration.InventoryType;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StoreInventoryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private long inventoryValueCostBegin;
    private long inventoryAmountBegin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long inventoryValueCostAfter;
    private long inventoryAmountAfter;
    private List<StoreInventoryLineDTO> storeInventoryLines = new ArrayList<>();
    private String userFullName;
    private String abbrName;
    private String statut;
    private InventoryType inventoryType;
    private CategoryInventory inventoryCategory;
    private StorageDTO storage;
    private RayonDTO rayon;
    private int gapCost;
    private int gapAmount;
    private String description;

    public StoreInventoryDTO() {}

    public StoreInventoryDTO(StoreInventory storeInventory) {
        copy(storeInventory, this);
    }

    public StoreInventoryDTO(StoreInventory storeInventory, List<StoreInventoryLineDTO> storeInventoryLines) {
        this(storeInventory);
        this.storeInventoryLines = storeInventoryLines;
    }

    /** Builder vierge — tous les champs sont à poser à la main. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder amorcé depuis l'entité : même projection que
     * {@link #StoreInventoryDTO(StoreInventory)}, mais réouvrable.
     *
     * <p>À préférer au constructeur dès que l'appelant doit corriger un champ après coup —
     * typiquement le statut, qu'un service peut vouloir dériver de l'état des lignes plutôt
     * que de recopier celui de l'entité.
     */
    public static Builder builder(StoreInventory storeInventory) {
        return new Builder().from(storeInventory);
    }

    /**
     * Projection entité → DTO, partagée par le constructeur et le builder : une seule
     * définition du mapping, quel que soit le point d'entrée.
     */
    private static void copy(StoreInventory source, StoreInventoryDTO target) {
        target.id = source.getId();
        target.inventoryValueCostBegin = source.getInventoryValueCostBegin();
        target.inventoryAmountBegin = source.getInventoryAmountBegin();
        target.createdAt = source.getCreatedAt();
        target.updatedAt = source.getUpdatedAt();
        target.inventoryValueCostAfter = source.getInventoryValueCostAfter();
        target.inventoryAmountAfter = source.getInventoryAmountAfter();
        AppUser user = source.getUser();
        target.abbrName = AppUserNames.shortName(user);
        target.statut = source.getStatut().name();
        target.inventoryType = source.getInventoryType();
        target.inventoryCategory = new CategoryInventory(source.getInventoryCategory());
        if (Objects.nonNull(source.getStorage())) {
            target.storage = new StorageDTO(source.getStorage());
        }
        if (Objects.nonNull(source.getRayon())) {
            target.rayon = new RayonDTO(source.getRayon());
        }
        if (Objects.nonNull(source.getGapCost())) {
            target.gapCost = source.getGapCost();
        }
        if (Objects.nonNull(source.getGapAmount())) {
            target.gapAmount = source.getGapAmount();
        }
        target.description = source.getDescription();
        if (Objects.isNull(source.getDescription())) {
            target.description = "Inventaire du " + source.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }

    /**
     * Builder de {@link StoreInventoryDTO}.
     *
     * <p>Il accumule dans l'instance qu'il finira par rendre, plutôt que de dupliquer les
     * dix-huit champs du DTO : celui-ci est mutable de bout en bout (setters publics), un
     * miroir n'apporterait aucune immuabilité et doublerait la surface à maintenir.
     */
    public static final class Builder {

        private final StoreInventoryDTO dto = new StoreInventoryDTO();

        private Builder() {}

        /** Applique la projection de l'entité, en écrasant ce qui a déjà été posé. */
        public Builder from(StoreInventory storeInventory) {
            copy(storeInventory, this.dto);
            return this;
        }

        public Builder id(Long id) {
            this.dto.id = id;
            return this;
        }

        public Builder statut(InventoryStatut statut) {
            this.dto.statut = Objects.nonNull(statut) ? statut.name() : null;
            return this;
        }

        public Builder inventoryType(InventoryType inventoryType) {
            this.dto.inventoryType = inventoryType;
            return this;
        }

        public Builder inventoryCategory(CategoryInventory inventoryCategory) {
            this.dto.inventoryCategory = inventoryCategory;
            return this;
        }

        public Builder storage(StorageDTO storage) {
            this.dto.storage = storage;
            return this;
        }

        public Builder rayon(RayonDTO rayon) {
            this.dto.rayon = rayon;
            return this;
        }

        public Builder description(String description) {
            this.dto.description = description;
            return this;
        }

        public Builder abbrName(String abbrName) {
            this.dto.abbrName = abbrName;
            return this;
        }

        public Builder userFullName(String userFullName) {
            this.dto.userFullName = userFullName;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.dto.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.dto.updatedAt = updatedAt;
            return this;
        }

        public Builder inventoryValueCostBegin(long inventoryValueCostBegin) {
            this.dto.inventoryValueCostBegin = inventoryValueCostBegin;
            return this;
        }

        public Builder inventoryAmountBegin(long inventoryAmountBegin) {
            this.dto.inventoryAmountBegin = inventoryAmountBegin;
            return this;
        }

        public Builder inventoryValueCostAfter(long inventoryValueCostAfter) {
            this.dto.inventoryValueCostAfter = inventoryValueCostAfter;
            return this;
        }

        public Builder inventoryAmountAfter(long inventoryAmountAfter) {
            this.dto.inventoryAmountAfter = inventoryAmountAfter;
            return this;
        }

        public Builder gapCost(int gapCost) {
            this.dto.gapCost = gapCost;
            return this;
        }

        public Builder gapAmount(int gapAmount) {
            this.dto.gapAmount = gapAmount;
            return this;
        }

        public Builder storeInventoryLines(List<StoreInventoryLineDTO> storeInventoryLines) {
            this.dto.storeInventoryLines = Objects.nonNull(storeInventoryLines) ? storeInventoryLines : new ArrayList<>();
            return this;
        }

        public StoreInventoryDTO build() {
            return this.dto;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getInventoryValueCostBegin() {
        return inventoryValueCostBegin;
    }

    public void setInventoryValueCostBegin(long inventoryValueCostBegin) {
        this.inventoryValueCostBegin = inventoryValueCostBegin;
    }

    public long getInventoryAmountBegin() {
        return inventoryAmountBegin;
    }

    public void setInventoryAmountBegin(long inventoryAmountBegin) {
        this.inventoryAmountBegin = inventoryAmountBegin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getInventoryValueCostAfter() {
        return inventoryValueCostAfter;
    }

    public void setInventoryValueCostAfter(long inventoryValueCostAfter) {
        this.inventoryValueCostAfter = inventoryValueCostAfter;
    }

    public long getInventoryAmountAfter() {
        return inventoryAmountAfter;
    }

    public void setInventoryAmountAfter(long inventoryAmountAfter) {
        this.inventoryAmountAfter = inventoryAmountAfter;
    }

    public List<StoreInventoryLineDTO> getStoreInventoryLines() {
        return storeInventoryLines;
    }

    public void setStoreInventoryLines(List<StoreInventoryLineDTO> storeInventoryLines) {
        this.storeInventoryLines = storeInventoryLines;
    }

    public String getDescription() {
        return description;
    }

    public StoreInventoryDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getAbbrName() {
        return abbrName;
    }

    public StoreInventoryDTO setAbbrName(String abbrName) {
        this.abbrName = abbrName;
        return this;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public InventoryType getInventoryType() {
        return inventoryType;
    }

    public StoreInventoryDTO setInventoryType(InventoryType inventoryType) {
        this.inventoryType = inventoryType;
        return this;
    }

    public CategoryInventory getInventoryCategory() {
        return inventoryCategory;
    }

    public StoreInventoryDTO setInventoryCategory(CategoryInventory inventoryCategory) {
        this.inventoryCategory = inventoryCategory;
        return this;
    }

    public StorageDTO getStorage() {
        return storage;
    }

    public StoreInventoryDTO setStorage(StorageDTO storage) {
        this.storage = storage;
        return this;
    }

    public RayonDTO getRayon() {
        return rayon;
    }

    public StoreInventoryDTO setRayon(RayonDTO rayon) {
        this.rayon = rayon;
        return this;
    }

    public int getGapCost() {
        return gapCost;
    }

    public StoreInventoryDTO setGapCost(int gapCost) {
        this.gapCost = gapCost;
        return this;
    }

    public int getGapAmount() {
        return gapAmount;
    }

    public StoreInventoryDTO setGapAmount(int gapAmount) {
        this.gapAmount = gapAmount;
        return this;
    }
}
