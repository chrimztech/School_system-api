package com.srms.api.modules.fee.repository;
import com.srms.api.modules.fee.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, String> {
    List<FeePayment> findBySchoolIdOrderByPaymentDateDesc(String schoolId);
    List<FeePayment> findBySchoolIdAndStudentId(String schoolId, String studentId);
    Optional<FeePayment> findByReferenceNumber(String referenceNumber);
    @Query("SELECT SUM(f.amount) FROM FeePayment f WHERE f.schoolId=:schoolId AND f.status='completed'")
    Double sumCollected(String schoolId);
}
