package com.srms.api.modules.school.repository;

import com.srms.api.modules.school.entity.SchoolBrandingAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolBrandingAssetRepository extends JpaRepository<SchoolBrandingAsset, String> {
}
