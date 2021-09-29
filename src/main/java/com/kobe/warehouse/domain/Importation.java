package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ImportationStatus;
import com.kobe.warehouse.domain.enumeration.ImportationType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "importation",
    indexes = {
        @Index(columnList = "importation_status", name = "importation_status_index"),
        @Index(columnList = "importation_type", name = "importation_type_index"),
        @Index(columnList = "created_at", name = "created_at_index")

    }
)
public class Importation implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "importation_type", nullable = false)
    private ImportationType importationType;
    private int totalZise;
    private int size;
    private int errorSize;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "importation_status", nullable = false)
    private ImportationStatus importationStatus;
    @NotNull
    @ManyToOne(optional = false)
    private User user;
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant created=Instant.now();
    @Column(name = "updated_at")
    private Instant updated;


    public Long getId() {
        return id;
    }

    public Importation setId(Long id) {
        this.id = id;
        return this;
    }

    public ImportationType getImportationType() {
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

    public ImportationStatus getImportationStatus() {
        return importationStatus;
    }

    public Importation setImportationStatus(ImportationStatus importationStatus) {
        this.importationStatus = importationStatus;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Importation setUser(User user) {
        this.user = user;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public Importation setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getUpdated() {
        return updated;
    }

    public Importation setUpdated(Instant updated) {
        this.updated = updated;
        return this;
    }
}
