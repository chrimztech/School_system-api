package com.srms.api.modules.student.repository;

import com.srms.api.modules.student.entity.Student;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the school-scoped, filtered query used by the paginated student list — the
 * server-side counterpart to what the frontend's search box and status/grade filters used to
 * do entirely client-side against a fully-loaded roster. Kept as a small standalone
 * Specification builder (rather than a pile of derived-query method names) since "search
 * across several fields, case-insensitively, plus optional exact filters, only when each is
 * actually given" doesn't fit Spring Data's method-name query derivation.
 */
public final class StudentSpecifications {
    private StudentSpecifications() {}

    public static Specification<Student> search(String schoolId, String query, String status, Integer grade) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("schoolId"), schoolId));
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("middleName")), like),
                        cb.like(cb.lower(root.get("admissionNumber")), like),
                        cb.like(cb.lower(root.get("guardian")), like),
                        cb.like(cb.lower(root.get("guardianPhone")), like),
                        cb.like(cb.lower(root.get("guardianEmail")), like)
                ));
            }
            if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(cb.lower(root.get("status").as(String.class)), status.toLowerCase()));
            }
            if (grade != null) {
                predicates.add(cb.equal(root.get("grade"), grade));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
