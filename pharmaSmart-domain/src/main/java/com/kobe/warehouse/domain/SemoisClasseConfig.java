package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "semois_classe_config")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SemoisClasseConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "classe_criticite", length = 10, nullable = false)
    private ClasseCriticite classeCriticite;

    @NotNull
    @DecimalMin("0.1")
    @DecimalMax("2.0")
    @Column(name = "coefficient_securite", precision = 3, scale = 2, nullable = false)
    private BigDecimal coefficientSecurite;

    @NotNull
    @Min(3)
    @Column(name = "nb_mois_historique", nullable = false)
    private Integer nbMoisHistorique;

    @NotNull
    @Column(name = "limite_peremption", nullable = false)
    private Boolean limitePeremption = false;

    public ClasseCriticite getClasseCriticite() { return classeCriticite; }
    public SemoisClasseConfig setClasseCriticite(ClasseCriticite classeCriticite) { this.classeCriticite = classeCriticite; return this; }

    public BigDecimal getCoefficientSecurite() { return coefficientSecurite; }
    public SemoisClasseConfig setCoefficientSecurite(BigDecimal coefficientSecurite) { this.coefficientSecurite = coefficientSecurite; return this; }

    public Integer getNbMoisHistorique() { return nbMoisHistorique; }
    public SemoisClasseConfig setNbMoisHistorique(Integer nbMoisHistorique) { this.nbMoisHistorique = nbMoisHistorique; return this; }

    public Boolean getLimitePeremption() { return limitePeremption; }
    public SemoisClasseConfig setLimitePeremption(Boolean limitePeremption) { this.limitePeremption = limitePeremption; return this; }
}
