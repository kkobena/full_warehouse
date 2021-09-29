package com.kobe.warehouse.domain;



import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class AssuredCustomer extends Customer implements Serializable {
    private static final long serialVersionUID = 1L;
    @ManyToOne
    private RemiseClient remise;
    @Column(name = "sexe")
    private String sexe;
    @Column(name = "dat_naiss")
    private LocalDate datNaiss;
    @OneToMany(mappedBy = "assuredCustomer")
    private Set<ThirdPartySales> sales = new HashSet<>();

    public RemiseClient getRemise() {
        return remise;
    }

    public void setRemise(RemiseClient remise) {
        this.remise = remise;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public LocalDate getDatNaiss() {
        return datNaiss;
    }

    public void setDatNaiss(LocalDate datNaiss) {
        this.datNaiss = datNaiss;
    }

    public Set<ThirdPartySales> getSales() {
        return sales;
    }

    public void setSales(Set<ThirdPartySales> sales) {
        this.sales = sales;
    }
}

