package com.srms.api.modules.auth.repository;

import com.srms.api.modules.auth.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByEmail(String email);
    // Intentionally List, not Optional: phone is meant to be unique (see AppUser.phone's
    // @Column(unique = true)), but that constraint isn't reliably enforced across every
    // environment's DB, so a stray duplicate must not crash the query with
    // IncorrectResultSizeDataAccessException — callers decide how to handle >1 match.
    List<AppUser> findByPhone(String phone);
    List<AppUser> findBySchoolId(String schoolId);
    List<AppUser> findBySchoolIdAndActiveTrue(String schoolId);
    List<AppUser> findByActiveTrue();
}