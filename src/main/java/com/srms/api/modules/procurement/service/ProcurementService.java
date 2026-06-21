package com.srms.api.modules.procurement.service;

import com.srms.api.modules.procurement.entity.ProcurementRequest;
import com.srms.api.modules.procurement.repository.ProcurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class ProcurementService {
    private final ProcurementRepository repo;
    public List<ProcurementRequest> list(String schoolId) { return repo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public ProcurementRequest create(String schoolId, ProcurementRequest r) {
        r.setSchoolId(schoolId);
        if (r.getStatus() == null || r.getStatus().isBlank()) r.setStatus("Draft");
        return repo.save(r);
    }
    public ProcurementRequest update(String schoolId, String id, ProcurementRequest updated) {
        ProcurementRequest r = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        if (updated.getRequester() != null) r.setRequester(updated.getRequester());
        if (updated.getDepartment() != null) r.setDepartment(updated.getDepartment());
        if (updated.getItem() != null) r.setItem(updated.getItem());
        if (updated.getQuantity() != null) r.setQuantity(updated.getQuantity());
        if (updated.getAmount() != null) r.setAmount(updated.getAmount());
        if (updated.getPriority() != null) r.setPriority(updated.getPriority());
        if (updated.getStatus() != null) r.setStatus(updated.getStatus());
        if (updated.getVendor() != null) r.setVendor(updated.getVendor());
        if (updated.getNeedByDate() != null) r.setNeedByDate(updated.getNeedByDate());
        if (updated.getBudgetCode() != null) r.setBudgetCode(updated.getBudgetCode());
        if (updated.getDeliveryPoint() != null) r.setDeliveryPoint(updated.getDeliveryPoint());
        if (updated.getJustification() != null) r.setJustification(updated.getJustification());
        return repo.save(r);
    }
    public void approve(String schoolId, String id) {
        ProcurementRequest r = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        r.setStatus("Approved"); repo.save(r);
    }
}
