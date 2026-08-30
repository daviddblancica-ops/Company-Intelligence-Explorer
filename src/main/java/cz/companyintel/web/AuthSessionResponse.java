package cz.companyintel.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class AuthSessionResponse {

    private final boolean authenticated;
    private final String username;
    private final List<String> roles;
    private final boolean canEdit;
    private final boolean canAdmin;

    private AuthSessionResponse(
            boolean authenticated,
            String username,
            List<String> roles,
            boolean canEdit,
            boolean canAdmin) {
        this.authenticated = authenticated;
        this.username = username;
        this.roles = roles;
        this.canEdit = canEdit;
        this.canAdmin = canAdmin;
    }

    public static AuthSessionResponse from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return anonymous();
        }
        List<String> roles = new ArrayList<String>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().startsWith("ROLE_")) {
                roles.add(authority.getAuthority().substring(5));
            }
        }
        Collections.sort(roles);
        boolean admin = roles.contains("ADMIN");
        return new AuthSessionResponse(true, authentication.getName(), roles,
                admin || roles.contains("EDITOR"), admin);
    }

    public static AuthSessionResponse anonymous() {
        return new AuthSessionResponse(false, null, Collections.<String>emptyList(), false, false);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public boolean isCanAdmin() {
        return canAdmin;
    }
}
