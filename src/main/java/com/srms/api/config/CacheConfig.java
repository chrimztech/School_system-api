package com.srms.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * In-process cache for small, rarely-changed, per-school config that's otherwise re-read from
 * Postgres on very hot paths — grading bands and CA/midterm/exam weight config are both fetched
 * on every single score save and every term-grade computation (see GradingScaleService,
 * GradeWeightConfigService), yet only ever change when a school admin edits Settings.
 *
 * This is Caffeine (single-instance, in-JVM), not Redis — correct for how this app is deployed
 * today (one backend instance). If/when this runs behind a load balancer across multiple
 * instances, swap this bean for a Redis-backed CacheManager (e.g. spring-boot-starter-data-redis
 * + RedisCacheManager) so a write on one instance evicts the value everywhere — with Caffeine,
 * each instance's cache is local, so another instance could keep serving a stale value for up to
 * the TTL below after an edit made through a different instance. The @Cacheable/@CacheEvict call
 * sites don't change either way — only this bean does.
 */
@Configuration
public class CacheConfig {
    public static final String GRADING_BANDS = "gradingBands";
    public static final String GRADE_WEIGHTS = "gradeWeights";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(GRADING_BANDS, GRADE_WEIGHTS);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(5_000));
        return manager;
    }
}
