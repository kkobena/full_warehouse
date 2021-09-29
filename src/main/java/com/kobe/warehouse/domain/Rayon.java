package com.kobe.warehouse.domain;


import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


/**
 * A Rayon.
 */
@Entity
@Table(name = "rayon", uniqueConstraints = { @UniqueConstraint(columnNames = { "libelle", "storage_id" }),
		@UniqueConstraint(columnNames = { "code", "storage_id" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Rayon implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
	@SequenceGenerator(name = "sequenceGenerator")
	private Long id;
	@Column(name = "created_at")
	private Instant createdAt;
	@Column(name = "updated_at")
	private Instant updatedAt;
	@NotNull
	@Column(name = "code", nullable = false)
	private String code;
	@NotNull
	@Column(name = "libelle", nullable = false)
	private String libelle;
	@OneToMany(mappedBy = "rayon")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private Set<StockProduit> stockProduits = new HashSet<>();
	@ManyToOne(optional = false)
	@NotNull
	private Storage storage;
	@Column(name = "exclude",columnDefinition = "boolean default false")
	private boolean exclude;
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
    public Rayon id(Long id) {
        this.id = id;
        return this;
    }
	public Instant getCreatedAt() {
		return createdAt;
	}

	public Rayon createdAt(Instant createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Rayon updatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}



	public String getCode() {
		return code;
	}

	public Rayon code(String code) {
		this.code = code;
		return this;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLibelle() {
		return libelle;
	}

	public Rayon libelle(String libelle) {
		this.libelle = libelle;
		return this;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	public Set<StockProduit> getStockProduits() {
		return stockProduits;
	}

	public Rayon stockProduits(Set<StockProduit> stockProduits) {
		this.stockProduits = stockProduits;
		return this;
	}

	public Rayon addStockProduit(StockProduit stockProduit) {
		this.stockProduits.add(stockProduit);
		stockProduit.setRayon(this);
		return this;
	}

	public Rayon removeStockProduit(StockProduit stockProduit) {
		this.stockProduits.remove(stockProduit);
		stockProduit.setRayon(null);
		return this;
	}

	public void setStockProduits(Set<StockProduit> stockProduits) {
		this.stockProduits = stockProduits;
	}

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public boolean isExclude() {
		return exclude;
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
            ", stockProduits=" + stockProduits +

            ", exclude=" + exclude +
            '}';
    }
}
