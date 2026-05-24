package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.SemoisClasseConfig}
 **/
@StaticMetamodel(SemoisClasseConfig.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class SemoisClasseConfig_ {

	
	/**
	 * @see #classeCriticite
	 **/
	public static final String CLASSE_CRITICITE = "classeCriticite";
	
	/**
	 * @see #coefficientSecurite
	 **/
	public static final String COEFFICIENT_SECURITE = "coefficientSecurite";
	
	/**
	 * @see #nbMoisHistorique
	 **/
	public static final String NB_MOIS_HISTORIQUE = "nbMoisHistorique";
	
	/**
	 * @see #limitePeremption
	 **/
	public static final String LIMITE_PEREMPTION = "limitePeremption";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.SemoisClasseConfig}
	 **/
	public static volatile EntityType<SemoisClasseConfig> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisClasseConfig#classeCriticite}
	 **/
	public static volatile SingularAttribute<SemoisClasseConfig, ClasseCriticite> classeCriticite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisClasseConfig#coefficientSecurite}
	 **/
	public static volatile SingularAttribute<SemoisClasseConfig, BigDecimal> coefficientSecurite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisClasseConfig#nbMoisHistorique}
	 **/
	public static volatile SingularAttribute<SemoisClasseConfig, Integer> nbMoisHistorique;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisClasseConfig#limitePeremption}
	 **/
	public static volatile SingularAttribute<SemoisClasseConfig, Boolean> limitePeremption;

}

