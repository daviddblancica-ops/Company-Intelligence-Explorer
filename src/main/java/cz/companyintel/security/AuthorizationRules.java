package cz.companyintel.security;

public final class AuthorizationRules {

    public static final String READ = "hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')";
    public static final String EDIT = "hasAnyRole('ADMIN', 'EDITOR')";
    public static final String ADMIN = "hasRole('ADMIN')";

    private AuthorizationRules() {
    }
}
