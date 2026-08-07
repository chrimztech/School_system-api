package com.srms.api.security.tenant;

import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantResolverTest {
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final TenantResolver resolver = new TenantResolver(schools, "school.edu.zm");

    @Test
    void resolvesMainDomainAsPlatform() {
        assertThat(resolver.resolve("school.edu.zm")).contains(TenantResolution.platform());
    }

    @Test
    void resolvesOnlyActiveSchoolSlugs() {
        School school = new School();
        school.setId("school-a");
        school.setSlug("lubu");
        when(schools.findBySlugAndActiveTrue("lubu")).thenReturn(Optional.of(school));

        assertThat(resolver.resolve("LUBU.school.edu.zm."))
                .contains(TenantResolution.tenant("school-a", "lubu"));
        assertThat(resolver.resolve("missing.school.edu.zm")).isEmpty();
    }

    @Test
    void rejectsNestedAndUnrecognizedHosts() {
        assertThat(resolver.resolve("other.lubu.school.edu.zm")).isEmpty();
        assertThat(resolver.resolve("school.edu.zm.attacker.example")).isEmpty();
    }
}
