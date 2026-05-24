package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.SuggestionReassort}
 **/
@StaticMetamodel(SuggestionReassort.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class SuggestionReassort_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #reference
	 **/
	public static final String REFERENCE = "reference";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #ligneReassorts
	 **/
	public static final String LIGNE_REASSORTS = "ligneReassorts";
	
	/**
	 * @see #magasin
	 **/
	public static final String MAGASIN = "magasin";
	
	/**
	 * @see #lastUserEdit
	 **/
	public static final String LAST_USER_EDIT = "lastUserEdit";
	
	/**
	 * @see #typeReassort
	 **/
	public static final String TYPE_REASSORT = "typeReassort";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.SuggestionReassort}
	 **/
	public static volatile EntityType<SuggestionReassort> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionReassort#id}
	 **/
	public static volatile SingularAttribute<SuggestionReassort, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionReassort#reference}
	 **/
	public static volatile SingularAttribute<SuggestionReassort, String> reference;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionReassort#createdAt}
	 **/
	public static volatile SingularAttribute<SuggestionReassort, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionReassort#updatedAt}
	 **/
	public static volatile SingularAttribute<SuggestionReassort, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionReassort#ligneReassorts}
	 **/
	public static volatile SetAttribute<SuggestionReassort, LigneReassort> ligneReassorts;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionReassort#magasin}
	 **/
	public static volatile SingularAttribute<SuggestionReassort, Magasin> magasin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionReassort#lastUserEdit}
	 **/
	public static volatile SingularAttribute<SuggestionReassort, AppUser> lastUserEdit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionReassort#typeReassort}
	 **/
	public static volatile SingularAttribute<SuggestionReassort, TypeReassort> typeReassort;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SuggestionReassort#statut}
	 **/
	public static volatile SingularAttribute<SuggestionReassort, StatutReassort> statut;

}

