package com.srms.api.modules.vendor.repository;

import com.srms.api.modules.vendor.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, String> {
    List<Vendor> findBySchoolIdOrderByNameAsc(String schoolId);
}
