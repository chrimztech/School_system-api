package com.srms.api.modules.facility.repository;

import com.srms.api.modules.facility.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, String> {
    List<Asset> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
