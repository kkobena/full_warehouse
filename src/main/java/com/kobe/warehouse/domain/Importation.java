package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ImportationStatus;
import com.kobe.warehouse.domain.enumeration.ImportationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "importation",
    indexes = {
        @Index(columnList = "importation_status", name = "importation_status_index"),
        @Index(columnList = "importation_type", name = "importation_type_index"),
        @Index(columnList = "created_at", name = "created_at_index"),
    }
)
public class Importation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "importation_type", nullable = false)
    private ImportationType importationType;

    private int totalZise;
    private int size;
    private int errorSize;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "importation_status", nullable = false)
    private ImportationStatus importationStatus;

    @NotNull
    @ManyToOne(optional = false)
    private AppUser user;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updated;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", name = "ligne_en_erreur")
    private Set<Object> ligneEnErreur = new HashSet<>();

    public Long getId() {
        return id;
    }

    public Importation setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull ImportationType getImportationType() {
        return importationType;
    }

    public Importation setImportationType(ImportationType importationType) {
        this.importationType = importationType;
        return this;
    }

    public int getTotalZise() {
        return totalZise;
    }

    public Importation setTotalZise(int totalZise) {
        this.totalZise = totalZise;
        return this;
    }

    public int getSize() {
        return size;
    }

    public Importation setSize(int size) {
        this.size = size;
        return this;
    }

    public int getErrorSize() {
        return errorSize;
    }

    public Importation setErrorSize(int errorSize) {
        this.errorSize = errorSize;
        return this;
    }

    public @NotNull ImportationStatus getImportationStatus() {
        return importationStatus;
    }

    public Importation setImportationStatus(ImportationStatus importationStatus) {
        this.importationStatus = importationStatus;
        return this;
    }

    public @NotNull AppUser getUser() {
        return user;
    }

    public Importation setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public @NotNull LocalDateTime getCreated() {
        return created;
    }

    public Importation setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public Importation setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public Set<Object> getLigneEnErreur() {
        return ligneEnErreur;
    }

    public Importation setLigneEnErreur(Set<Object> ligneEnErreur) {
        this.ligneEnErreur = ligneEnErreur;
        return this;
    }
}
