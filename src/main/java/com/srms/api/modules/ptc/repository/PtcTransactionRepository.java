package com.srms.api.modules.ptc.repository;
import com.srms.api.modules.ptc.entity.PtcTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface PtcTransactionRepository extends JpaRepository<PtcTransaction, String> {
    List<PtcTransaction> findBySchoolIdOrderByDateDesc(String schoolId);
}
