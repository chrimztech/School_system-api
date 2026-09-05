package com.srms.api.modules.facility.service;

import com.srms.api.modules.facility.entity.Asset;
import com.srms.api.modules.facility.entity.WorkOrder;
import com.srms.api.modules.facility.repository.AssetRepository;
import com.srms.api.modules.facility.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class FacilityService {
    private final WorkOrderRepository repo;
    private final AssetRepository assetRepo;
    public List<WorkOrder> list(String schoolId) { return repo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public WorkOrder create(String schoolId, WorkOrder wo) { wo.setSchoolId(schoolId); return repo.save(wo); }
    public WorkOrder update(String schoolId, String id, WorkOrder updated) {
        WorkOrder wo = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        if (updated.getStatus() != null) wo.setStatus(updated.getStatus());
        if (updated.getPriority() != null) wo.setPriority(updated.getPriority());
        if (updated.getDueDate() != null) wo.setDueDate(updated.getDueDate());
        if (updated.getOwner() != null) wo.setOwner(updated.getOwner());
        WorkOrder saved = repo.save(wo);
        if ("Closed".equals(saved.getStatus()) && Boolean.TRUE.equals(saved.getRecurring()) && saved.getFrequencyDays() != null) {
            scheduleNextOccurrence(schoolId, saved);
        }
        return saved;
    }
    public void close(String schoolId, String id) {
        WorkOrder wo = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        wo.setStatus("Closed"); repo.save(wo);
        if (Boolean.TRUE.equals(wo.getRecurring()) && wo.getFrequencyDays() != null) {
            scheduleNextOccurrence(schoolId, wo);
        }
    }

    private void scheduleNextOccurrence(String schoolId, WorkOrder previous) {
        LocalDate base;
        try { base = LocalDate.parse(previous.getDueDate()); } catch (Exception e) { base = LocalDate.now(); }
        LocalDate nextDue = base.plusDays(previous.getFrequencyDays());
        WorkOrder next = WorkOrder.builder()
            .schoolId(schoolId)
            .title(previous.getTitle())
            .location(previous.getLocation())
            .owner(previous.getOwner())
            .priority(previous.getPriority())
            .status("Open")
            .dueDate(nextDue.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .workOrderType(previous.getWorkOrderType())
            .contractorAssigned(previous.getContractorAssigned())
            .costEstimate(previous.getCostEstimate())
            .budgetCode(previous.getBudgetCode())
            .partsRequired(previous.getPartsRequired())
            .description(previous.getDescription())
            .safetyRisk(previous.getSafetyRisk())
            .recurring(true)
            .frequencyDays(previous.getFrequencyDays())
            .build();
        repo.save(next);
    }

    public List<Asset> listAssets(String schoolId) { return assetRepo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public Asset createAsset(String schoolId, Asset asset) { asset.setSchoolId(schoolId); return assetRepo.save(asset); }
    public Asset updateAsset(String schoolId, String id, Asset updated) {
        Asset asset = assetRepo.findById(id).filter(a -> a.getSchoolId().equals(schoolId)).orElseThrow();
        asset.setName(updated.getName());
        asset.setCategory(updated.getCategory());
        asset.setLocation(updated.getLocation());
        asset.setCondition(updated.getCondition());
        asset.setSerialNumber(updated.getSerialNumber());
        asset.setPurchaseDate(updated.getPurchaseDate());
        asset.setWarrantyExpiry(updated.getWarrantyExpiry());
        asset.setValue(updated.getValue());
        asset.setNotes(updated.getNotes());
        return assetRepo.save(asset);
    }
    public void deleteAsset(String schoolId, String id) {
        assetRepo.findById(id).filter(a -> a.getSchoolId().equals(schoolId)).ifPresent(assetRepo::delete);
    }
}
