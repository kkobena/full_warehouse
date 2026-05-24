package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ClientTiersPayant}
 **/
@StaticMetamodel(ClientTiersPayant.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ClientTiersPayant_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #tiersPayant
	 **/
	public static final String TIERS_PAYANT = "tiersPayant";
	
	/**
	 * @see #assuredCustomer
	 **/
	public static final String ASSURED_CUSTOMER = "assuredCustomer";
	
	/**
	 * @see #num
	 **/
	public static final String NUM = "num";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #priorite
	 **/
	public static final String PRIORITE = "priorite";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #taux
	 **/
	public static final String TAUX = "taux";
	
	/**
	 * @see #consoMensuelle
	 **/
	public static final String CONSO_MENSUELLE = "consoMensuelle";
	
	/**
	 * @see #consommations
	 **/
	public static final String CONSOMMATIONS = "consommations";
	
	/**
	 * @see #clientTiersPayantTauxHistoriques
	 **/
	public static final String CLIENT_TIERS_PAYANT_TAUX_HISTORIQUES = "clientTiersPayantTauxHistoriques";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ClientTiersPayant}
	 **/
	public static volatile EntityType<ClientTiersPayant> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#id}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#tiersPayant}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, TiersPayant> tiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#assuredCustomer}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, AssuredCustomer> assuredCustomer;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#num}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, String> num;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#created}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#updated}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#priorite}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, PrioriteTiersPayant> priorite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#statut}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, TiersPayantStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#taux}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, Integer> taux;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#consoMensuelle}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, Long> consoMensuelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#consommations}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, Set> consommations;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClientTiersPayant#clientTiersPayantTauxHistoriques}
	 **/
	public static volatile SingularAttribute<ClientTiersPayant, Set> clientTiersPayantTauxHistoriques;

}

