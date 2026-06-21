package com.srms.api.modules.auth.repository;

import com.srms.api.modules.auth.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByEmail(String email);
    List<AppUser> findBySchoolId(String schoolId);
    List<AppUser> findBySchoolIdAndActiveTrue(String schoolId);
    List<AppUser> findByActiveTrue();
}