package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Qui autorise une dérogation (allergie, limite de crédit) : l'utilisateur lui-même s'il détient
 * le droit, sinon le collègue dont il présente la clé de sécurité et qui le détient.
 */
@Service
@Transactional(readOnly = true)
public class DerogationAuthorizer {

    private final NavAccessService navAccessService;
    private final UserService userService;
    private final UtilisationCleSecuriteService cleSecuriteService;

    public DerogationAuthorizer(NavAccessService navAccessService, UserService userService, UtilisationCleSecuriteService cleSecuriteService) {
        this.navAccessService = navAccessService;
        this.userService = userService;
        this.cleSecuriteService = cleSecuriteService;
    }

    public AppUser utilisateurCourant() {
        return userService.getUser();
    }

    /**
     * @param droit      code {@code ACTION} requis
     * @param cle        clé de sécurité d'un collègue, si l'utilisateur n'a pas le droit
     * @param operation  l'opération, pour les messages (« la délivrance malgré une allergie »)
     * @param errorKey   préfixe des clés d'erreur ({@code <errorKey>CleRequise}, {@code <errorKey>NonAutorisee})
     */
    public AppUser autoriser(String droit, String cle, String operation, String errorKey) {
        if (navAccessService.isAllowed(List.of(droit), NavAction.EXECUTE)) {
            return userService.getUser();
        }
        if (!StringUtils.hasText(cle)) {
            throw new ForbiddenOperationException(
                capitaliser(operation) + " : la clé de sécurité d'un pharmacien est requise.",
                errorKey + "CleRequise"
            );
        }
        return userService
            .getUserByPwdOrSecurityKey(cle)
            .filter(user ->
                user
                    .getAuthorities()
                    .stream()
                    .map(Authority::getName)
                    .anyMatch(role -> AuthoritiesConstants.ADMIN.equals(role) || cleSecuriteService.hasPrivilege(droit, role))
            )
            .orElseThrow(() -> new ForbiddenOperationException("Cette clé n'autorise pas " + operation + ".", errorKey + "NonAutorisee"));
    }

    private static String capitaliser(String texte) {
        return texte.isEmpty() ? texte : Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }
}
