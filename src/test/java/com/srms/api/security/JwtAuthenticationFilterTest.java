package com.srms.api.security;

import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, users);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildsAuthenticationFromTheActiveDatabaseUser() throws Exception {
        AppUser user = AppUser.builder()
                .name("Parent")
                .passwordHash("unused")
                .role(AppUser.UserRole.PARENT)
                .schoolId("school-a")
                .active(true)
                .build();
        user.setId("user-a");
        when(tokens.validateToken("signed-token")).thenReturn(true);
        when(tokens.getUserId("signed-token")).thenReturn("user-a");
        when(users.findById("user-a")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer signed-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials())
                .isEqualTo("school-a");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_PARENT");
    }

    @Test
    void doesNotAuthenticateADeactivatedDatabaseUser() throws Exception {
        AppUser user = AppUser.builder()
                .name("Parent")
                .passwordHash("unused")
                .role(AppUser.UserRole.PARENT)
                .schoolId("school-a")
                .active(false)
                .build();
        user.setId("user-a");
        when(tokens.validateToken("signed-token")).thenReturn(true);
        when(tokens.getUserId("signed-token")).thenReturn("user-a");
        when(users.findById("user-a")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer signed-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
