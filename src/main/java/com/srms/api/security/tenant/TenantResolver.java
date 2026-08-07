package com.srms.api.security.tenant;

import com.srms.api.modules.school.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class TenantResolver {
    private static final Pattern SLUG = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");
    private static final Pattern PRIVATE_IPV4 = Pattern.compile(
            "(?:127\\..+|10\\..+|192\\.168\\..+|172\\.(?:1[6-9]|2[0-9]|3[01])\\..+)");

    private final SchoolRepository schoolRepository;
    private final String baseDomain;

    public TenantResolver(
            SchoolRepository schoolRepository,
            @Value("${app.tenancy.base-domain:school.edu.zm}") String baseDomain) {
        this.schoolRepository = schoolRepository;
        this.baseDomain = normalizeHost(baseDomain);
    }

    public Optional<TenantResolution> resolve(String hostname) {
        String host = normalizeHost(hostname);
        if (host.equals(baseDomain)) {
            return Optional.of(TenantResolution.platform());
        }
        if (isDevelopmentHost(host)) {
            return Optional.of(TenantResolution.development());
        }

        String slug = subdomainSlug(host, baseDomain);
        if (slug == null && host.endsWith(".localhost")) {
            slug = subdomainSlug(host, "localhost");
        }
        if (slug == null || !SLUG.matcher(slug).matches()) {
            return Optional.empty();
        }

        return schoolRepository.findBySlugAndActiveTrue(slug)
                .map(school -> TenantResolution.tenant(school.getId(), school.getSlug()));
    }

    private static String subdomainSlug(String host, String parentDomain) {
        String suffix = "." + parentDomain;
        if (!host.endsWith(suffix)) return null;
        String candidate = host.substring(0, host.length() - suffix.length());
        return candidate.contains(".") ? null : candidate;
    }

    private static boolean isDevelopmentHost(String host) {
        return host.equals("localhost")
                || host.equals("::1")
                || host.equals("0:0:0:0:0:0:0:1")
                || PRIVATE_IPV4.matcher(host).matches();
    }

    private static String normalizeHost(String hostname) {
        String host = hostname == null ? "" : hostname.trim().toLowerCase(Locale.ROOT);
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        return host;
    }
}
