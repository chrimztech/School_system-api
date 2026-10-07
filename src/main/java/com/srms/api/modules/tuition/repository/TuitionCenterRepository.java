package com.srms.api.modules.tuition.repository;

import com.srms.api.modules.tuition.entity.TuitionCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TuitionCenterRepository extends JpaRepository<TuitionCenter, String> {
    List<TuitionCenter> findAllByOrderByNameAsc();
    List<TuitionCenter> findByActiveTrueOrderByRatingDesc();
}
