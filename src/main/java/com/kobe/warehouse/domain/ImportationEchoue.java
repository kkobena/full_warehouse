package com.kobe.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "importation_echouee")
public class ImportationEchoue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "created", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    private Integer objectId;

    @OneToMany(mappedBy = "importationEchoue", orphanRemoval = true, cascade = { CascadeType.REMOVE, CascadeType.PERSIST })
    private List<ImportationEchoueLigne> importationEchoueLignes = new ArrayList<>();

    @Column(name = "is_commande", nullable = false)
    private boolean isCommande;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public void setObjectId(Integer objectId) {
        this.objectId = objectId;
    }

    public boolean isCommande() {
        return isCommande;
    }

    public void setCommande(boolean commande) {
        isCommande = commande;
    }

    public List<ImportationEchoueLigne> getImportationEchoueLignes() {
        return importationEchoueLignes;
    }

    public void setImportationEchoueLignes(List<ImportationEchoueLigne> importationEchoueLignes) {
        this.importationEchoueLignes = importationEchoueLignes;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}
