package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import com.kobe.warehouse.domain.enumeration.Periodicite;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.PlanificationFacturation}
 **/
@StaticMetamodel(PlanificationFacturation.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class PlanificationFacturation_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #periodicite
	 **/
	public static final String PERIODICITE = "periodicite";
	
	/**
	 * @see #dernierePeriodeFin
	 **/
	public static final String DERNIERE_PERIODE_FIN = "dernierePeriodeFin";
	
	/**
	 * @see #heureDeclenchement
	 **/
	public static final String HEURE_DECLENCHEMENT = "heureDeclenchement";
	
	/**
	 * @see #factureProvisoire
	 **/
	public static final String FACTURE_PROVISOIRE = "factureProvisoire";
	
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
	 * Static metamodel type for {@link com.kobe.warehouse.domain.PlanificationFacturation}
	 **/
	public static volatile EntityType<PlanificationFacturation> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#id}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#libelle}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#periodicite}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, Periodicite> periodicite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#dernierePeriodeFin}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, LocalDate> dernierePeriodeFin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#heureDeclenchement}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, LocalTime> heureDeclenchement;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#factureProvisoire}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, Boolean> factureProvisoire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#actif}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, Boolean> actif;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#prochaineExecution}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, LocalDateTime> prochaineExecution;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#derniereExecution}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, LocalDateTime> derniereExecution;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#dernierStatut}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, ExecutionStatut> dernierStatut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#dernierMessage}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, String> dernierMessage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#created}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PlanificationFacturation#updated}
	 **/
	public static volatile SingularAttribute<PlanificationFacturation, LocalDateTime> updated;

}

