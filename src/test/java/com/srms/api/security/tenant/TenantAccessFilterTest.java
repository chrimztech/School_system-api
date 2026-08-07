package com.srms.api.security.tenant;

import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantAccessFilterTest {
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final TenantAccessFilter filter = new TenantAccessFilter(new TenantResolver(schools, "school.edu.zm"));

    @BeforeEach
    void setUp() {
        School lubu = new School();
        lubu.setId("school-a");
        lubu.setSlug("lubu");
        when(schools.findBySlugAndActiveTrue("lubu")).thenReturn(Optional.of(lubu));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsMatchingHostUserAndPath() throws Exception {
        authenticate("user-a", "school-a", "PARENT");
        Result result = execute("lubu.school.edu.zm", "/api/schools/school-a/students");

        assertThat(result.response.getStatus()).isEqualTo(200);
        assertThat(result.chain.getRequest()).isNotNull();
    }

    @Test
    void deniesChangingPathToAnotherSchool() throws Exception {
        authenticate("user-a", "school-a", "SCHOOL_ADMIN");
        Result result = execute("lubu.school.edu.zm", "/api/schools/school-b/students");

        assertThat(result.response.getStatus()).isEqualTo(403);
        assertThat(result.chain.getRequest()).isNull();
    }

    @Test
    void deniesUsingAnotherSchoolsTokenOnHost() throws Exception {
        authenticate("user-b", "school-b", "PARENT");
        Result result = execute("lubu.school.edu.zm", "/api/schools/school-a/students");

        assertThat(result.response.getStatus()).isEqualTo(403);
    }

    @Test
    void deniesChangedSchoolHeader() throws Exception {
        authenticate("user-a", "school-a", "PARENT");
        MockHttpServletRequest request = request("lubu.school.edu.zm", "/api/schools/school-a/students");
        request.addHeader("X-School-Id", "school-b");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void returnsNotFoundForUnknownOrInactiveSlug() throws Exception {
        Result result = execute("missing.school.edu.zm", "/api/auth/login");

        assertThat(result.response.getStatus()).isEqualTo(404);
        assertThat(result.chain.getRequest()).isNull();
    }

    private void authenticate(String userId, String schoolId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, schoolId, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Result execute(String host, String path) throws Exception {
        MockHttpServletRequest request = request(host, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return new Result(response, chain);
    }

    private MockHttpServletRequest request(String host, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServerName(host);
        request.setRequestURI(path);
        return request;
    }

    private record Result(MockHttpServletResponse response, MockFilterChain chain) {}
}
