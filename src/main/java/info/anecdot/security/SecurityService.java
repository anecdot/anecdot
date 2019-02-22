package info.anecdot.security;

import info.anecdot.settings.SettingsService;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Stephan Grundner
 */
@Service
public class SecurityService  {

    @Autowired
    private SettingsService settingsService;

    private String resolveEmail(Authentication authentication) {
        if (authentication instanceof KeycloakAuthenticationToken) {
            KeycloakAuthenticationToken authenticationToken = (KeycloakAuthenticationToken) authentication;
            KeycloakPrincipal principal = (KeycloakPrincipal) authenticationToken.getPrincipal();
            KeycloakSecurityContext securityContext = principal.getKeycloakSecurityContext();
            AccessToken accessToken = securityContext.getToken();
            String email = accessToken.getEmail();

            return email;
        }

        return null;
    }

    private boolean hasAccess(String path) {
        AtomicBoolean granted = new AtomicBoolean(true);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AntPathMatcher pathMatcher = new AntPathMatcher();

        settingsService.eachSettingsForUri(path, settings -> {

            for (Permission permission : settings.getPermissions()) {
                String relativePath = path;
                if (relativePath.startsWith(settings.getPath())) {
                    relativePath = relativePath.substring(settings.getPath().length(), relativePath.length());

                    if (!relativePath.startsWith("/")) {
                        relativePath = "/" + relativePath;
                    }
                    boolean restricted = pathMatcher.match(permission.getPattern(), relativePath);
                    if (restricted) {

                        if (permission.getUsers().isEmpty() && permission.getRoles().isEmpty()) {
                            granted.set(permission.getKind() == Permission.Kind.ALLOW);

                            continue;
                        }

                        String email = resolveEmail(authentication);
                        boolean emailFound = permission.getUsers().contains(email);
                        boolean authorityFound = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .anyMatch(authority -> permission.getRoles().stream().anyMatch(it -> it.equalsIgnoreCase(authority)));

                        boolean match = (emailFound || authorityFound);

                        if (permission.getKind() == Permission.Kind.ALLOW) {
                            granted.set(match);
                        } else {

                            if (match) {
                                granted.set(false);
                            }
                        }
                    }
                }
            }
        });

        return granted.get();
    }

    public boolean hasAccess(HttpServletRequest request) {
        UrlPathHelper pathHelper = new UrlPathHelper();
        String path = pathHelper.getPathWithinApplication(request);

        return hasAccess(path);
    }

    public boolean hasAccess() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        return hasAccess(request);
    }
}
