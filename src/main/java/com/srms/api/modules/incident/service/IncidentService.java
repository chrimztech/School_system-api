package com.srms.api.modules.incident.service;

import com.srms.api.modules.incident.entity.Incident;
import com.srms.api.modules.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class IncidentService {
    private final IncidentRepository repo;
    public List<Incident> list(String schoolId) { return repo.findBySchoolIdOrderByIncidentDateDesc(schoolId); }
    public Incident create(String schoolId, Incident i) { i.setSchoolId(schoolId); return repo.save(i); }
    public Incident update(String schoolId, String id, Incident updated) {
        Incident i = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        i.setStatus(updated.getStatus()); i.setDescription(updated.getDescription());
        return repo.save(i);
    }
    public void resolve(String schoolId, String id) {
        Incident i = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        i.setStatus("Resolved"); repo.save(i);
    }
}
