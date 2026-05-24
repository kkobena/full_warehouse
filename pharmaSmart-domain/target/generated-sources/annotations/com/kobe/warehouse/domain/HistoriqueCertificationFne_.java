package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.HistoriqueCertificationFne}
 **/
@StaticMetamodel(HistoriqueCertificationFne.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class HistoriqueCertificationFne_ {

	
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
	 * @see #nbCertifiees
	 **/
	public static final String NB_CERTIFIEES = "nbCertifiees";
	
	/**
	 * @see #nbEchecs
	 **/
	public static final String NB_ECHECS = "nbEchecs";
	
	/**
	 * @see #message
	 **/
	public static final String MESSAGE = "message";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.HistoriqueCertificationFne}
	 **/
	public static volatile EntityType<HistoriqueCertificationFne> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueCertificationFne#id}
	 **/
	public static volatile SingularAttribute<HistoriqueCertificationFne, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueCertificationFne#planificationId}
	 **/
	public static volatile SingularAttribute<HistoriqueCertificationFne, Integer> planificationId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueCertificationFne#executionDebut}
	 **/
	public static volatile SingularAttribute<HistoriqueCertificationFne, LocalDateTime> executionDebut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueCertificationFne#executionFin}
	 **/
	public static volatile SingularAttribute<HistoriqueCertificationFne, LocalDateTime> executionFin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueCertificationFne#statut}
	 **/
	public static volatile SingularAttribute<HistoriqueCertificationFne, ExecutionStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueCertificationFne#nbCertifiees}
	 **/
	public static volatile SingularAttribute<HistoriqueCertificationFne, Integer> nbCertifiees;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueCertificationFne#nbEchecs}
	 **/
	public static volatile SingularAttribute<HistoriqueCertificationFne, Integer> nbEchecs;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueCertificationFne#message}
	 **/
	public static volatile SingularAttribute<HistoriqueCertificationFne, String> message;

}

