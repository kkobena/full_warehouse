package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.PlanificationCertificationFne}
 **/
@StaticMetamodel(PlanificationCertificationFne.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class PlanificationCertificationFne_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #heureDeclenchement
	 **/
	public static final String HEURE_DECLENCHEMENT = "heureDeclenchement";
	
	/**
	 * @see #actif
	 **/
	public static final String ACTIF = "actif";
	
	/**
	 * @see #prochaineExecution
	 **/
	public static final String PROCHAINE_EXECUTION = "prochaineExecution";
	
	/**
	 * @see #derniereExecution
	 **/
	public static final String DERNIERE_EXECUTION = "derniereExecution";
	
	/**
	 * @see #dernierStatut
	 **/
	public static final String DERNIER_STATUT = "dernierStatut";
	
	/**
	 * @see #dernierMessage
	 **/
	public static final String DERNIER_MESSAGE = "dernierMessage";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.PlanificationCertificationFne}
	 **/
	public static volatile EntityType<PlanificationCertificationFne> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#id}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#libelle}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#heureDeclenchement}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, LocalTime> heureDeclenchement;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#actif}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, Boolean> actif;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#prochaineExecution}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, LocalDateTime> prochaineExecution;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#derniereExecution}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, LocalDateTime> derniereExecution;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#dernierStatut}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, ExecutionStatut> dernierStatut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#dernierMessage}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, String> dernierMessage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#created}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationCertificationFne#updated}
	 **/
	public static volatile SingularAttribute<PlanificationCertificationFne, LocalDateTime> updated;

}

