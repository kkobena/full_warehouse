package com.kobe.warehouse.domain;


import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Rayon.
 */
@Getter
@Entity
@Table(name = "rayon", uniqueConstraints = {@UniqueConstraint(columnNames = {"libelle", "storage_id"}),
    @UniqueConstraint(columnNames = {"code", "storage_id"})})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Rayon implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @NotNull
    @Column(name = "code", nullable = false)
    private String code;
    @NotNull
    @Column(name = "libelle", nullable = false)
    private String libelle;

    @ManyToOne(optional = false)
    @NotNull
    private Storage storage;
    @Column(name = "exclude", columnDefinition = "boolean default false")
    private boolean exclude;

  public void setId(Long id) {
        this.id = id;
    }

    public Rayon id(Long id) {
        this.id = id;
        return this;
    }

  public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Rayon createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

  public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Rayon updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

  public void setCode(String code) {
        this.code = code;
    }

    public Rayon code(String code) {
        this.code = code;
        return this;
    }

  public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Rayon libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

  public void setStorage(Storage storage) {
        this.storage = storage;
    }

  public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rayon)) {
            return false;
        }
        return id != null && id.equals(((Rayon) o).id);
    }


    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Rayon{" +
            "id=" + id +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", code='" + code + '\'' +
            ", libelle='" + libelle + '\'' +


            ", exclude=" + exclude +
            '}';
    }
}
