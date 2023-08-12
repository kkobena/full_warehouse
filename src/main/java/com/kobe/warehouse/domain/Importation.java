package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ImportationStatus;
import com.kobe.warehouse.domain.enumeration.ImportationType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.annotations.Type;

@Getter
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
    private LocalDateTime created = LocalDateTime.now();
    @Column(name = "updated_at")
    private LocalDateTime updated;
    @Type(type = "io.hypersistence.utils.hibernate.type.json.JsonType")
    @Column(columnDefinition = "json", name = "ligne_en_erreur")
    private Set<Object> ligneEnErreur = new HashSet<>();

  public Importation setLigneEnErreur(Set<Object> ligneEnErreur) {
        this.ligneEnErreur = ligneEnErreur;
        return this;
    }

  public Importation setId(Long id) {
        this.id = id;
        return this;
    }

  public Importation setImportationType(ImportationType importationType) {
        this.importationType = importationType;
        return this;
    }

  public Importation setTotalZise(int totalZise) {
        this.totalZise = totalZise;
        return this;
    }

  public Importation setSize(int size) {
        this.size = size;
        return this;
    }

  public Importation setErrorSize(int errorSize) {
        this.errorSize = errorSize;
        return this;
    }

  public Importation setImportationStatus(ImportationStatus importationStatus) {
        this.importationStatus = importationStatus;
        return this;
    }

  public Importation setUser(User user) {
        this.user = user;
        return this;
    }

  public Importation setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

  public Importation setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }
}
