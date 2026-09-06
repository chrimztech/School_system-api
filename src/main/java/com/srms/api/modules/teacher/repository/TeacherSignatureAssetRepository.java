package com.srms.api.modules.teacher.repository;

import com.srms.api.modules.teacher.entity.TeacherSignatureAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherSignatureAssetRepository extends JpaRepository<TeacherSignatureAsset, String> {
}
