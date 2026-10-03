package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientTiersPayantRepository extends JpaRepository<ClientTiersPayant, Integer> {
    List<ClientTiersPayant> findAllByAssuredCustomerId(Integer customerId);

    List<ClientTiersPayant> findAllByAssuredCustomerIdAndTiersPayantCategorie(Integer customerId, TiersPayantCategorie categorie);

    List<ClientTiersPayant> findAllByIdIn(Set<Integer> ids);

    List<ClientTiersPayant> findAllByTiersPayantId(Integer tiersPayantId);

    /** Au plus un : (tiers_payant_id, num) est unique en base. */
    Optional<ClientTiersPayant> findFirstByTiersPayantIdAndNum(Integer tiersPayantId, String num);

    /**
     * Dossiers qui portent déjà ces nom, prénom ET numéro de matricule, tous organismes confondus.
     * {@code idClientExclu} vaut -1 en création : un identifiant réel n'est jamais négatif.
     */
    @Query(
        "select c from ClientTiersPayant c where upper(c.assuredCustomer.firstName) = upper(:firstName) " +
        "and upper(c.assuredCustomer.lastName) = upper(:lastName) and c.num = :num " +
        "and c.assuredCustomer.id <> :idClientExclu order by c.id"
    )
    List<ClientTiersPayant> findDossiersIdentiques(
        @Param("firstName") String firstName,
        @Param("lastName") String lastName,
        @Param("num") String num,
        @Param("idClientExclu") Integer idClientExclu
    );
}
