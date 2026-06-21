package com.srms.api.modules.vendor.service;

import com.srms.api.modules.vendor.entity.Vendor;
import com.srms.api.modules.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class VendorService {
    private final VendorRepository repo;
    public List<Vendor> list(String schoolId) { return repo.findBySchoolIdOrderByNameAsc(schoolId); }
    public Vendor create(String schoolId, Vendor v) { v.setSchoolId(schoolId); return repo.save(v); }
    public Vendor update(String schoolId, String id, Vendor updated) {
        Vendor v = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        v.setStatus(updated.getStatus()); v.setContractExpiry(updated.getContractExpiry());
        v.setContactPerson(updated.getContactPerson()); v.setPhone(updated.getPhone()); v.setEmail(updated.getEmail());
        return repo.save(v);
    }
}
