package com.srms.api.modules.fee.repository;
import com.srms.api.modules.fee.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, String> {
    List<FeePayment> findBySchoolIdOrderByPaymentDateDesc(String schoolId);
    List<FeePayment> findBySchoolIdAndStudentId(String schoolId, String studentId);
    Optional<FeePayment> findByReferenceNumber(String referenceNumber);
    @Query("SELECT SUM(f.amount) FROM FeePayment f WHERE f.schoolId=:schoolId AND f.status='completed'")
    Double sumCollected(String schoolId);

    // Atomic "win the race" transition — only ever applied by whichever caller (callback or a status
    // poll) gets here first. Returns 0 rows updated if the payment was already settled by someone else,
    // which callers use as the signal to skip re-applying the balance change.
    @Modifying
    @Query("UPDATE FeePayment f SET f.status = :status, f.gatewayResponseCode = :code WHERE f.id = :id AND f.status = 'pending'")
    int markStatusIfPending(@Param("id") String id, @Param("status") FeePayment.PaymentStatus status, @Param("code") String code);

    @Modifying
    @Query("UPDATE FeePayment f SET f.gatewayResponseCode = :code WHERE f.id = :id")
    void updateGatewayResponseCode(@Param("id") String id, @Param("code") String code);

    List<FeePayment> findByStatusAndGatewayProviderIsNotNullAndCreatedAtBefore(FeePayment.PaymentStatus status, LocalDateTime cutoff);
}
