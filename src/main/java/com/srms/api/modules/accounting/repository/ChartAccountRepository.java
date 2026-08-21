package com.srms.api.modules.accounting.repository;

import com.srms.api.modules.accounting.entity.ChartAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChartAccountRepository extends JpaRepository<ChartAccount, String> {
    List<ChartAccount> findBySchoolIdOrderByCodeAsc(String schoolId);
}
