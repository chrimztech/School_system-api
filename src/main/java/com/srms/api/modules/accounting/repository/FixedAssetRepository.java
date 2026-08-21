package com.srms.api.modules.accounting.repository;

import com.srms.api.modules.accounting.entity.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, String> {
    List<FixedAsset> findBySchoolIdOrderByPurchaseDateDesc(String schoolId);
}
