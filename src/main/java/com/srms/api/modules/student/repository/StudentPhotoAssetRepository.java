package com.srms.api.modules.student.repository;

import com.srms.api.modules.student.entity.StudentPhotoAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentPhotoAssetRepository extends JpaRepository<StudentPhotoAsset, String> {
}
