package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.SuggestionLine}
 **/
@StaticMetamodel(SuggestionLine.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class SuggestionLine_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #quantity
	 **/
	public static final String QUANTITY = "quantity";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #quantiteModifieeManuel
	 **/
	public static final String QUANTITE_MODIFIEE_MANUEL = "quantiteModifieeManuel";
	
	/**
	 * @see #suggestion
	 **/
	public static final String SUGGESTION = "suggestion";
	
	/**
	 * @see #fournisseurProduit
	 **/
	public static final String FOURNISSEUR_PRODUIT = "fournisseurProduit";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.SuggestionLine}
	 **/
	public static volatile EntityType<SuggestionLine> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionLine#id}
	 **/
	public static volatile SingularAttribute<SuggestionLine, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionLine#quantity}
	 **/
	public static volatile SingularAttribute<SuggestionLine, Integer> quantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionLine#createdAt}
	 **/
	public static volatile SingularAttribute<SuggestionLine, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionLine#updatedAt}
	 **/
	public static volatile SingularAttribute<SuggestionLine, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionLine#quantiteModifieeManuel}
	 **/
	public static volatile SingularAttribute<SuggestionLine, Boolean> quantiteModifieeManuel;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionLine#suggestion}
	 **/
	public static volatile SingularAttribute<SuggestionLine, Suggestion> suggestion;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionLine#fournisseurProduit}
	 **/
	public static volatile SingularAttribute<SuggestionLine, FournisseurProduit> fournisseurProduit;

}

