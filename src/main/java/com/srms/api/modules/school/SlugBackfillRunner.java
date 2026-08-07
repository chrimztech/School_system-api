package com.srms.api.modules.school;

import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import com.srms.api.modules.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Assigns a slug to any school that predates the slug field (nullable in the schema since
 * there's no migration tool to backfill it in one shot). Tenant subdomain resolution can't
 * reach a school with no slug, so this keeps every active school reachable. Runs on every
 * boot and is a no-op once no schools are missing a slug. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlugBackfillRunner implements ApplicationRunner {
    private final SchoolRepository schoolRepository;
    private final SchoolService schoolService;

    @Override
    public void run(ApplicationArguments args) {
        for (School school : schoolRepository.findAll()) {
            if (school.getSlug() != null && !school.getSlug().isBlank()) continue;
            String slug = schoolService.generateUniqueSlug(school);
            school.setSlug(slug);
            schoolRepository.save(school);
            log.info("Backfilled slug '{}' for school {}", slug, school.getId());
        }
    }
}
