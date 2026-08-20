package com.businessos.modules.demo;

import com.businessos.auth.authentication.LoginResponse;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.security.JwtService;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "See Demo" on the landing page calls this to drop an anonymous visitor into
 * the seeded demo tenant.
 *
 * Mints a short-lived access token for the demo owner directly - there is no
 * password exchange, and the demo account's stored password is random and
 * never disclosed, so the only way into the demo is this endpoint. Every
 * mutation the token could attempt is blocked by DemoReadOnlyFilter, which is
 * what makes handing out sessions to the whole internet acceptable.
 *
 * No refresh token on purpose: when the access token dies, the demo is over.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/demo")
public class DemoSessionController {

    /** Demo sessions outlive a coffee break but not a lunch: 45 minutes. */
    private static final long DEMO_SESSION_MS = 45L * 60 * 1000;

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JwtService jwtService;

    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;

    @PostMapping("/session")
    public ResponseEntity<LoginResponse> start() {
        if (!demoEnabled) {
            throw new ResourceNotFoundException("The demo is not available right now");
        }
        Company company = companyRepository.findBySubdomain(DemoDataSeeder.DEMO_SUBDOMAIN)
                .orElseThrow(() -> new ResourceNotFoundException("The demo is not available right now"));
        User demoUser = userRepository.findByEmail(DemoDataSeeder.DEMO_OWNER_EMAIL)
                .orElseThrow(() -> new ResourceNotFoundException("The demo is not available right now"));

        String accessToken = jwtService.generateAccessToken(
                demoUser.getEmail(), demoUser.getRole().name(), company.getId(), DEMO_SESSION_MS);

        return ResponseEntity.ok(new LoginResponse(
                demoUser.getId(), demoUser.getFirstName(), demoUser.getEmail(),
                demoUser.getRole(), company.getId(), accessToken, null));
    }
}
