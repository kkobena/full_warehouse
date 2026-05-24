package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.SemoisSuggestionView}
 **/
@StaticMetamodel(SemoisSuggestionView.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class SemoisSuggestionView_ {

	
	/**
	 * @see #produitId
	 **/
	public static final String PRODUIT_ID = "produitId";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #codeCip
	 **/
	public static final String CODE_CIP = "codeCip";
	
	/**
	 * @see #fournisseurId
	 **/
	public static final String FOURNISSEUR_ID = "fournisseurId";
	
	/**
	 * @see #fournisseurLibelle
	 **/
	public static final String FOURNISSEUR_LIBELLE = "fournisseurLibelle";
	
	/**
	 * @see #classeCriticite
	 **/
	public static final String CLASSE_CRITICITE = "classeCriticite";
	
	/**
	 * @see #coefficientSecurite
	 **/
	public static final String COEFFICIENT_SECURITE = "coefficientSecurite";
	
	/**
	 * @see #delaiLivraisonJours
	 **/
	public static final String DELAI_LIVRAISON_JOURS = "delaiLivraisonJours";
	
	/**
	 * @see #vmm
	 **/
	public static final String VMM = "vmm";
	
	/**
	 * @see #margeSecurite
	 **/
	public static final String MARGE_SECURITE = "margeSecurite";
	
	/**
	 * @see #stockObjectif
	 **/
	public static final String STOCK_OBJECTIF = "stockObjectif";
	
	/**
	 * @see #stockActuel
	 **/
	public static final String STOCK_ACTUEL = "stockActuel";
	
	/**
	 * @see #quantiteACommander
	 **/
	public static final String QUANTITE_ACOMMANDER = "quantiteACommander";
	
	/**
	 * @see #dateDernierCalcul
	 **/
	public static final String DATE_DERNIER_CALCUL = "dateDernierCalcul";
	
	/**
	 * @see #facteurSaisonnier
	 **/
	public static final String FACTEUR_SAISONNIER = "facteurSaisonnier";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.SemoisSuggestionView}
	 **/
	public static volatile EntityType<SemoisSuggestionView> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#produitId}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Integer> produitId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#libelle}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#codeCip}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, String> codeCip;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#fournisseurId}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Integer> fournisseurId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#fournisseurLibelle}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, String> fournisseurLibelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#classeCriticite}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, ClasseCriticite> classeCriticite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#coefficientSecurite}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Double> coefficientSecurite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#delaiLivraisonJours}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Integer> delaiLivraisonJours;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#vmm}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Integer> vmm;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#margeSecurite}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Integer> margeSecurite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#stockObjectif}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Integer> stockObjectif;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#stockActuel}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Long> stockActuel;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#quantiteACommander}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Long> quantiteACommander;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#dateDernierCalcul}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, Instant> dateDernierCalcul;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SemoisSuggestionView#facteurSaisonnier}
	 **/
	public static volatile SingularAttribute<SemoisSuggestionView, BigDecimal> facteurSaisonnier;

}

