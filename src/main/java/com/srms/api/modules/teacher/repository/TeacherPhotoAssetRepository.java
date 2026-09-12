package com.srms.api.modules.teacher.repository;

import com.srms.api.modules.teacher.entity.TeacherPhotoAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherPhotoAssetRepository extends JpaRepository<TeacherPhotoAsset, String> {
}
