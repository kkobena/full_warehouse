package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ImportationStatus;
import com.kobe.warehouse.domain.enumeration.ImportationType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Importation}
 **/
@StaticMetamodel(Importation.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Importation_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #importationType
	 **/
	public static final String IMPORTATION_TYPE = "importationType";
	
	/**
	 * @see #totalZise
	 **/
	public static final String TOTAL_ZISE = "totalZise";
	
	/**
	 * @see #size
	 **/
	public static final String SIZE = "size";
	
	/**
	 * @see #errorSize
	 **/
	public static final String ERROR_SIZE = "errorSize";
	
	/**
	 * @see #importationStatus
	 **/
	public static final String IMPORTATION_STATUS = "importationStatus";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #ligneEnErreur
	 **/
	public static final String LIGNE_EN_ERREUR = "ligneEnErreur";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Importation}
	 **/
	public static volatile EntityType<Importation> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#id}
	 **/
	public static volatile SingularAttribute<Importation, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#importationType}
	 **/
	public static volatile SingularAttribute<Importation, ImportationType> importationType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#totalZise}
	 **/
	public static volatile SingularAttribute<Importation, Integer> totalZise;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#size}
	 **/
	public static volatile SingularAttribute<Importation, Integer> size;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#errorSize}
	 **/
	public static volatile SingularAttribute<Importation, Integer> errorSize;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#importationStatus}
	 **/
	public static volatile SingularAttribute<Importation, ImportationStatus> importationStatus;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#user}
	 **/
	public static volatile SingularAttribute<Importation, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#created}
	 **/
	public static volatile SingularAttribute<Importation, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#updated}
	 **/
	public static volatile SingularAttribute<Importation, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Importation#ligneEnErreur}
	 **/
	public static volatile SingularAttribute<Importation, Set> ligneEnErreur;

}

