package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JoinFormula;

/**
 * A Magasin.
 */
@Entity
@Table(name = "magasin")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Magasin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @NotNull
    @Column(name = "full_name", nullable = false, unique = true)
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "note")
    private String note;

    @Column(name = "registre")
    private String registre;

    @Column(name = "compte_contribuable")
    private String compteContribuable;

    @Column(name = "num_comptable")
    private String numComptable;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinFormula("(SELECT s.id FROM storage s WHERE s.storage_type='PRINCIPAL' AND s.magasin_id=id LIMIT 1)")
    private Storage primaryStorage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinFormula("(SELECT s.id FROM storage s WHERE s.storage_type='POINT_DE_VENTE' AND s.magasin_id=id LIMIT 1)")
    private Storage pointOfSale;

    @Column(name = "welcome_message")
    private String welcomeMessage;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_magasin", nullable = false, length = 20)
    private TypeMagasin typeMagasin = TypeMagasin.OFFICINE;

    @Column(name = "email")
    @Email
    private String email;

    @Column(name = "compte_bancaire")
    private String compteBancaire;

    @Column(name = "registre_imposition")
    private String registreImposition;

    public String getCompteBancaire() {
        return compteBancaire;
    }

    public void setCompteBancaire(String compteBancaire) {
        this.compteBancaire = compteBancaire;
    }

    public @Email String getEmail() {
        return email;
    }

    public void setEmail(@Email String email) {
        this.email = email;
    }

    public @NotNull String getFullName() {
        return fullName;
    }

    public void setFullName(@NotNull String fullName) {
        this.fullName = fullName;
    }

    public @NotNull TypeMagasin getTypeMagasin() {
        return typeMagasin;
    }

    public Magasin setTypeMagasin(@NotNull TypeMagasin typeMagasin) {
        this.typeMagasin = typeMagasin;
        return this;
    }

    public Storage getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(Storage pointOfSale) {
        this.pointOfSale = pointOfSale;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public Magasin setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
        return this;
    }

    public Storage getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(Storage primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Magasin name(String name) {
        this.name = name;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Magasin phone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Magasin address(String address) {
        this.address = address;
        return this;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Magasin note(String note) {
        this.note = note;
        return this;
    }

    public String getRegistre() {
        return registre;
    }

    public void setRegistre(String registre) {
        this.registre = registre;
    }

    public Magasin registre(String registre) {
        this.registre = registre;
        return this;
    }

    public String getCompteContribuable() {
        return compteContribuable;
    }

    public void setCompteContribuable(String compteContribuable) {
        this.compteContribuable = compteContribuable;
    }

    public String getNumComptable() {
        return numComptable;
    }

    public void setNumComptable(String numComptable) {
        this.numComptable = numComptable;
    }

    public String getRegistreImposition() {
        return registreImposition;
    }

    public Magasin setRegistreImposition(String registreImposition) {
        this.registreImposition = registreImposition;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Magasin)) {
            return false;
        }
        return id != null && id.equals(((Magasin) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Magasin{"
            + "id="
            + getId()
            + ", name='"
            + getName()
            + "'"
            + ", phone='"
            + getPhone()
            + "'"
            + ", address='"
            + getAddress()
            + "'"
            + ", note='"
            + getNote()
            + "'"
            + ", registre='"
            + getRegistre()
            + "'"
            + "}";
    }
}
