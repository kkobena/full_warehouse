package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Suggestion}
 **/
@StaticMetamodel(Suggestion.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Suggestion_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #suggessionReference
	 **/
	public static final String SUGGESSION_REFERENCE = "suggessionReference";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #suggestionLines
	 **/
	public static final String SUGGESTION_LINES = "suggestionLines";
	
	/**
	 * @see #magasin
	 **/
	public static final String MAGASIN = "magasin";
	
	/**
	 * @see #lastUserEdit
	 **/
	public static final String LAST_USER_EDIT = "lastUserEdit";
	
	/**
	 * @see #typeSuggession
	 **/
	public static final String TYPE_SUGGESSION = "typeSuggession";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #validePar
	 **/
	public static final String VALIDE_PAR = "validePar";
	
	/**
	 * @see #dateValidation
	 **/
	public static final String DATE_VALIDATION = "dateValidation";
	
	/**
	 * @see #fournisseur
	 **/
	public static final String FOURNISSEUR = "fournisseur";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Suggestion}
	 **/
	public static volatile EntityType<Suggestion> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#id}
	 **/
	public static volatile SingularAttribute<Suggestion, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#suggessionReference}
	 **/
	public static volatile SingularAttribute<Suggestion, String> suggessionReference;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#createdAt}
	 **/
	public static volatile SingularAttribute<Suggestion, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#updatedAt}
	 **/
	public static volatile SingularAttribute<Suggestion, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#suggestionLines}
	 **/
	public static volatile SetAttribute<Suggestion, SuggestionLine> suggestionLines;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#magasin}
	 **/
	public static volatile SingularAttribute<Suggestion, Magasin> magasin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#lastUserEdit}
	 **/
	public static volatile SingularAttribute<Suggestion, AppUser> lastUserEdit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#typeSuggession}
	 **/
	public static volatile SingularAttribute<Suggestion, TypeSuggession> typeSuggession;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#statut}
	 **/
	public static volatile SingularAttribute<Suggestion, StatutSuggession> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#validePar}
	 **/
	public static volatile SingularAttribute<Suggestion, AppUser> validePar;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#dateValidation}
	 **/
	public static volatile SingularAttribute<Suggestion, LocalDateTime> dateValidation;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Suggestion#fournisseur}
	 **/
	public static volatile SingularAttribute<Suggestion, Fournisseur> fournisseur;

}

