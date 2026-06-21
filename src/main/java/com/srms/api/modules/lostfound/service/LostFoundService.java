package com.srms.api.modules.lostfound.service;

import com.srms.api.modules.lostfound.entity.LostFoundItem;
import com.srms.api.modules.lostfound.repository.LostFoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class LostFoundService {
    private final LostFoundRepository repo;
    public List<LostFoundItem> getAll(String schoolId) { return repo.findBySchoolIdOrderByFoundDateDesc(schoolId); }
    public LostFoundItem create(String schoolId, LostFoundItem item) {
        item.setSchoolId(schoolId);
        if (item.getFoundDate() == null) item.setFoundDate(LocalDate.now());
        if (item.getStatus() == null) item.setStatus("UNCLAIMED");
        return repo.save(item);
    }
    public LostFoundItem update(String schoolId, String id, LostFoundItem updated) {
        LostFoundItem item = repo.findById(id).filter(i -> i.getSchoolId().equals(schoolId)).orElseThrow();
        item.setItemDescription(updated.getItemDescription()); item.setCategory(updated.getCategory()); item.setStatus(updated.getStatus()); item.setOwnerName(updated.getOwnerName()); item.setOwnerContact(updated.getOwnerContact());
        return repo.save(item);
    }
    public LostFoundItem claim(String schoolId, String id, String ownerName, String ownerContact) {
        LostFoundItem item = repo.findById(id).filter(i -> i.getSchoolId().equals(schoolId)).orElseThrow();
        item.setStatus("CLAIMED"); item.setClaimedDate(LocalDate.now()); item.setOwnerName(ownerName); item.setOwnerContact(ownerContact);
        return repo.save(item);
    }
}
