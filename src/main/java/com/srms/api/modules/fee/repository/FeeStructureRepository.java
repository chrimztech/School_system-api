package com.srms.api.modules.fee.repository;
import com.srms.api.modules.fee.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface FeeStructureRepository extends JpaRepository<FeeStructure, String> {
    List<FeeStructure> findBySchoolIdOrderByAcademicYearDescGradeFromAscTermAsc(String schoolId);
}
