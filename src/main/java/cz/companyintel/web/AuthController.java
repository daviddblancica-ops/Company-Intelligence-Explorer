package cz.companyintel.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/session")
    public AuthSessionResponse session(Authentication authentication, CsrfToken csrfToken) {
        csrfToken.getToken();
        return AuthSessionResponse.from(authentication);
    }
}
