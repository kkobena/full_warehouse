package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.SemoisConfiguration}
 **/
@StaticMetamodel(SemoisConfiguration.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class SemoisConfiguration_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #classeCriticite
	 **/
	public static final String CLASSE_CRITICITE = "classeCriticite";
	
	/**
	 * @see #delaiLivraisonJours
	 **/
	public static final String DELAI_LIVRAISON_JOURS = "delaiLivraisonJours";
	
	/**
	 * @see #frequenceCommandeJours
	 **/
	public static final String FREQUENCE_COMMANDE_JOURS = "frequenceCommandeJours";
	
	/**
	 * @see #margeSecurite
	 **/
	public static final String MARGE_SECURITE = "margeSecurite";
	
	/**
	 * @see #stockObjectifCalcule
	 **/
	public static final String STOCK_OBJECTIF_CALCULE = "stockObjectifCalcule";
	
	/**
	 * @see #vmmCalcule
	 **/
	public static final String VMM_CALCULE = "vmmCalcule";
	
	/**
	 * @see #dateDernierCalcul
	 **/
	public static final String DATE_DERNIER_CALCUL = "dateDernierCalcul";
	
	/**
	 * @see #facteurSaisonnierActuel
	 **/
	public static final String FACTEUR_SAISONNIER_ACTUEL = "facteurSaisonnierActuel";
	
	/**
	 * @see #facteurSaisonnierManuel
	 **/
	public static final String FACTEUR_SAISONNIER_MANUEL = "facteurSaisonnierManuel";
	
	/**
	 * @see #limitePeremption
	 **/
	public static final String LIMITE_PEREMPTION = "limitePeremption";
	
	/**
	 * @see #exclusionDate
	 **/
	public static final String EXCLUSION_DATE = "exclusionDate";
	
	/**
	 * @see #exclusionDureeJours
	 **/
	public static final String EXCLUSION_DUREE_JOURS = "exclusionDureeJours";
	
	/**
	 * @see #exclusionMotif
	 **/
	public static final String EXCLUSION_MOTIF = "exclusionMotif";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.SemoisConfiguration}
	 **/
	public static volatile EntityType<SemoisConfiguration> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#id}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#produit}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#classeCriticite}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, ClasseCriticite> classeCriticite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#delaiLivraisonJours}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Integer> delaiLivraisonJours;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#frequenceCommandeJours}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Integer> frequenceCommandeJours;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#margeSecurite}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Integer> margeSecurite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#stockObjectifCalcule}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Integer> stockObjectifCalcule;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#vmmCalcule}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Integer> vmmCalcule;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#dateDernierCalcul}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, LocalDateTime> dateDernierCalcul;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#facteurSaisonnierActuel}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, BigDecimal> facteurSaisonnierActuel;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#facteurSaisonnierManuel}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Boolean> facteurSaisonnierManuel;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#limitePeremption}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Boolean> limitePeremption;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#exclusionDate}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, LocalDateTime> exclusionDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#exclusionDureeJours}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, Integer> exclusionDureeJours;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#exclusionMotif}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, String> exclusionMotif;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#createdAt}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisConfiguration#updatedAt}
	 **/
	public static volatile SingularAttribute<SemoisConfiguration, LocalDateTime> updatedAt;

}

