package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CritereTournant;
import com.kobe.warehouse.domain.enumeration.FrequenceTournant;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.PlanningInventaireTournant}
 **/
@StaticMetamodel(PlanningInventaireTournant.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class PlanningInventaireTournant_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #frequence
	 **/
	public static final String FREQUENCE = "frequence";
	
	/**
	 * @see #critere
	 **/
	public static final String CRITERE = "critere";
	
	/**
	 * @see #storage
	 **/
	public static final String STORAGE = "storage";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #prochaineExecution
	 **/
	public static final String PROCHAINE_EXECUTION = "prochaineExecution";
	
	/**
	 * @see #actif
	 **/
	public static final String ACTIF = "actif";
	
	/**
	 * @see #critereIndexCourant
	 **/
	public static final String CRITERE_INDEX_COURANT = "critereIndexCourant";
	
	/**
	 * @see #classeParetoCourante
	 **/
	public static final String CLASSE_PARETO_COURANTE = "classeParetoCourante";
	
	/**
	 * @see #nbExecutions
	 **/
	public static final String NB_EXECUTIONS = "nbExecutions";
	
	/**
	 * @see #derniereExecution
	 **/
	public static final String DERNIERE_EXECUTION = "derniereExecution";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.PlanningInventaireTournant}
	 **/
	public static volatile EntityType<PlanningInventaireTournant> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#id}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#libelle}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#frequence}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, FrequenceTournant> frequence;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#critere}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, CritereTournant> critere;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#storage}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, Storage> storage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#user}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#prochaineExecution}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, LocalDate> prochaineExecution;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#actif}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, Boolean> actif;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#critereIndexCourant}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, Integer> critereIndexCourant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#classeParetoCourante}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, String> classeParetoCourante;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#nbExecutions}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, Integer> nbExecutions;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#derniereExecution}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, LocalDate> derniereExecution;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#createdAt}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanningInventaireTournant#updatedAt}
	 **/
	public static volatile SingularAttribute<PlanningInventaireTournant, LocalDateTime> updatedAt;

}

