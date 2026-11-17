package com.kobe.warehouse.service.referential.magasin;

import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.service.dto.MagasinDTO;
import java.util.List;
import java.util.Set;

public interface MagasinService {
    MagasinDTO save(MagasinDTO magasin);

    MagasinDTO currentUserMagasin();

    MagasinDTO findById(Integer id);

    void delete(Integer id);

    List<MagasinDTO> findAll(Set<TypeMagasin> types);

    boolean hasDepot();

    /**
     * Remonte jusqu'à l'officine dont ce magasin dépend, ou lui-même s'il est déjà racine.
     *
     * <p>C'est elle qui porte la licence, l'identité FNE et le compte chez le grossiste : tout ce
     * qui engage l'établissement doit interroger la racine et non le magasin courant.
     */
    MagasinDTO resoudreOfficineRacine(Integer magasinId);

    /**
     * Magasins où l'utilisateur courant peut affecter quelqu'un : le sien et ses rattachés. Un seul
     * élément signifie qu'il n'y a pas de choix à offrir.
     */
    List<MagasinDTO> listerLesMagasinsDAffectation();

    /**
     * Magasins que l'utilisateur courant peut lire. Un seul élément : l'écran n'a pas de sélecteur
     * à offrir, ni de colonne magasin à montrer.
     */
    List<MagasinDTO> listerLaPorteeDeSupervision();
}
