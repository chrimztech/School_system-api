package com.srms.api.modules.facility.service;

import com.srms.api.modules.facility.entity.WorkOrder;
import com.srms.api.modules.facility.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class FacilityService {
    private final WorkOrderRepository repo;
    public List<WorkOrder> list(String schoolId) { return repo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public WorkOrder create(String schoolId, WorkOrder wo) { wo.setSchoolId(schoolId); return repo.save(wo); }
    public WorkOrder update(String schoolId, String id, WorkOrder updated) {
        WorkOrder wo = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        wo.setStatus(updated.getStatus()); wo.setPriority(updated.getPriority()); wo.setDueDate(updated.getDueDate()); wo.setOwner(updated.getOwner());
        return repo.save(wo);
    }
    public void close(String schoolId, String id) {
        WorkOrder wo = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        wo.setStatus("Closed"); repo.save(wo);
    }
}
