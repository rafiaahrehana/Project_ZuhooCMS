package com.businessos.auth.authentication;

import com.businessos.auth.password.ResetPasswordRequest;
import com.businessos.auth.token.TokenRepository;
import com.businessos.auth.token.TokenService;
import com.businessos.auth.token.TokenType;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.crm.client.ClientService;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.security.JwtService;
import com.businessos.shared.audit.AuditService;
import com.businessos.shared.email.EmailService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.notification.NotificationPreferenceService;
import com.businessos.security.SecurityUtil;
import com.businessos.auth.authentication.google.GoogleTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers AuthServiceImpl.resetPassword()'s two mutually-exclusive paths after the
 * "forgot password" flow switched from a JWT link to a numeric code:
 *  - email+code: the new "forgot password" path.
 *  - token: the pre-existing JWT link ClientServiceImpl.invite() still emails to let
 *    a newly-invited client set their initial portal password. This is the one path
 *    that can't be exercised through the browser in this dev environment (no SMTP
 *    credentials configured to actually deliver the invite email), so it's covered
 *    here instead - using the real JwtService bean to mint the token, exactly the
 *    way ClientServiceImpl.invite() does, rather than hand-crafting one.
 */
class ResetPasswordTest {

    private UserRepository userRepository;
    private TokenRepository tokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthServiceImpl authService;

    private static final String SECRET =
        "bHVzT3M3S3k4d1VhQlhuUXZtZjdkTkVqcFpzMlJpNlFGdFljV2VBb0doSXBLbERNbFZiMDkzNFhuVzY=";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(TokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        // Real bean, not a mock - the whole point is to prove a token this class
        // issues is one AuthServiceImpl.resetPassword() actually accepts.
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);

        authService = new AuthServiceImpl(
            userRepository,
            mock(CompanyRepository.class),
            mock(EmployeeRepository.class),
            mock(ClientRepository.class),
            tokenRepository,
            mock(TokenService.class),
            passwordEncoder,
            mock(GoogleTokenVerifier.class),
            mock(ClientService.class),
            jwtService,
            mock(AuthenticationManager.class),
            mock(EmailService.class),
            mock(AuditService.class),
            mock(NotificationPreferenceService.class),
            mock(SecurityUtil.class)
        );

        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded:" + inv.getArgument(0));
    }

    @Test
    void tokenPath_acceptsTheSameTokenClientInviteWouldMint() {
        String email = "invited-client@example.com";
        User user = User.builder().email(email).password("old-hash").build();
        ReflectionTestUtils.setField(user, "id", 42L);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Exactly ClientServiceImpl.invite()'s call: 7-day PASSWORD_RESET action token.
        String token = jwtService.generateActionToken(email, TokenType.PASSWORD_RESET, 7L * 24 * 60 * 60 * 1000);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword("BrandNewPass1!");
        request.setConfirmPassword("BrandNewPass1!");

        authService.resetPassword(request);

        verify(passwordEncoder).encode("BrandNewPass1!");
        assertEquals("encoded:BrandNewPass1!", user.getPassword());
        verify(userRepository).save(user);
        verify(tokenRepository).revokeAllByUserIdAndType(42L, TokenType.REFRESH);
    }

    @Test
    void tokenPath_rejectsAGarbageToken() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("not-a-real-jwt");
        request.setNewPassword("BrandNewPass1!");
        request.setConfirmPassword("BrandNewPass1!");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertTrue(ex.getMessage().contains("Invalid or expired reset link"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void tokenPath_rejectsATokenOfTheWrongActionType() {
        String email = "someone@example.com";
        // A refresh-flow-style token has no actionType claim at all, unlike a
        // PASSWORD_RESET action token - extractActionType() returns null for it,
        // which must not equal TokenType.PASSWORD_RESET.
        String accessToken = jwtService.generateAccessToken(email, "COMPANY_OWNER", null);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(accessToken);
        request.setNewPassword("BrandNewPass1!");
        request.setConfirmPassword("BrandNewPass1!");

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        verifyNoInteractions(userRepository);
    }

    @Test
    void codePath_stillWorksForForgotPassword() {
        String email = "forgetful-owner@example.com";
        User user = User.builder().email(email).password("old-hash").build();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setPasswordResetCode("123456");
        user.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setCode("123456");
        request.setNewPassword("AnotherPass1!");
        request.setConfirmPassword("AnotherPass1!");

        authService.resetPassword(request);

        assertEquals("encoded:AnotherPass1!", user.getPassword());
        assertNull(user.getPasswordResetCode());
        assertNull(user.getPasswordResetCodeExpiresAt());
        verify(tokenRepository).revokeAllByUserIdAndType(7L, TokenType.REFRESH);
    }

    @Test
    void codePath_rejectsAnExpiredCode() {
        String email = "forgetful-owner@example.com";
        User user = User.builder().email(email).password("old-hash").build();
        user.setPasswordResetCode("123456");
        user.setPasswordResetCodeExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setCode("123456");
        request.setNewPassword("AnotherPass1!");
        request.setConfirmPassword("AnotherPass1!");

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        verify(userRepository, never()).save(any());
    }
}
