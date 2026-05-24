package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.HistoriquePlanification}
 **/
@StaticMetamodel(HistoriquePlanification.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class HistoriquePlanification_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #planificationId
	 **/
	public static final String PLANIFICATION_ID = "planificationId";
	
	/**
	 * @see #executionDebut
	 **/
	public static final String EXECUTION_DEBUT = "executionDebut";
	
	/**
	 * @see #executionFin
	 **/
	public static final String EXECUTION_FIN = "executionFin";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #generationCode
	 **/
	public static final String GENERATION_CODE = "generationCode";
	
	/**
	 * @see #nombreFactures
	 **/
	public static final String NOMBRE_FACTURES = "nombreFactures";
	
	/**
	 * @see #message
	 **/
	public static final String MESSAGE = "message";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.HistoriquePlanification}
	 **/
	public static volatile EntityType<HistoriquePlanification> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriquePlanification#id}
	 **/
	public static volatile SingularAttribute<HistoriquePlanification, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriquePlanification#planificationId}
	 **/
	public static volatile SingularAttribute<HistoriquePlanification, Integer> planificationId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriquePlanification#executionDebut}
	 **/
	public static volatile SingularAttribute<HistoriquePlanification, LocalDateTime> executionDebut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriquePlanification#executionFin}
	 **/
	public static volatile SingularAttribute<HistoriquePlanification, LocalDateTime> executionFin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriquePlanification#statut}
	 **/
	public static volatile SingularAttribute<HistoriquePlanification, ExecutionStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriquePlanification#generationCode}
	 **/
	public static volatile SingularAttribute<HistoriquePlanification, Integer> generationCode;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriquePlanification#nombreFactures}
	 **/
	public static volatile SingularAttribute<HistoriquePlanification, Integer> nombreFactures;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriquePlanification#message}
	 **/
	public static volatile SingularAttribute<HistoriquePlanification, String> message;

}

