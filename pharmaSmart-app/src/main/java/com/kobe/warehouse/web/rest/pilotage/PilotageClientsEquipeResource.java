package com.kobe.warehouse.web.rest.pilotage;

import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.pilotage.ClientsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.EquipePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.pilotage.ClientsEquipePilotageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Onglet « Clients & équipe » du pilotage ; la fréquentation passe par l'analyse (heure × jour de semaine). */
@RestController
@RequestMapping("/api/pilotage/clients-equipe")
@RequiresNavAccess("pilotage.clients-equipe")
public class PilotageClientsEquipeResource {

    private final ClientsEquipePilotageService clientsEquipePilotageService;

    public PilotageClientsEquipeResource(ClientsEquipePilotageService clientsEquipePilotageService) {
        this.clientsEquipePilotageService = clientsEquipePilotageService;
    }

    @GetMapping("/clients")
    public ResponseEntity<ClientsPilotageDTO> analyserClients(RequetePilotageDTO requete) {
        return ResponseEntity.ok(clientsEquipePilotageService.analyserClients(requete));
    }

    @GetMapping("/equipe")
    public ResponseEntity<EquipePilotageDTO> analyserEquipe(RequetePilotageDTO requete) {
        return ResponseEntity.ok(clientsEquipePilotageService.analyserEquipe(requete));
    }
}
