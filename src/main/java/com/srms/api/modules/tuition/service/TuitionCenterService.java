package com.srms.api.modules.tuition.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.tuition.dto.TuitionCenterSaveRequest;
import com.srms.api.modules.tuition.entity.TuitionCenter;
import com.srms.api.modules.tuition.repository.TuitionCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TuitionCenterService {
    private final TuitionCenterRepository repository;

    public List<TuitionCenter> listAll() {
        return repository.findAllByOrderByNameAsc();
    }

    public TuitionCenter create(TuitionCenterSaveRequest req, String actor) {
        TuitionCenter center = new TuitionCenter();
        apply(center, req);
        center.setCreatedBy(actor);
        return repository.save(center);
    }

    public TuitionCenter update(String id, TuitionCenterSaveRequest req, String actor) {
        TuitionCenter center = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tuition centre", id));
        apply(center, req);
        center.setUpdatedBy(actor);
        return repository.save(center);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    private void apply(TuitionCenter center, TuitionCenterSaveRequest req) {
        if (req.name() == null || req.name().isBlank()) throw new BusinessException("A name is required");
        if (req.subjects() == null || req.subjects().stream().noneMatch(s -> s != null && !s.isBlank())) {
            throw new BusinessException("At least one subject is required");
        }
        if (req.rating() != null && (req.rating() < 0 || req.rating() > 5)) {
            throw new BusinessException("Rating must be between 0 and 5");
        }
        center.setName(req.name().trim());
        center.setSubjectsText(req.subjects().stream().filter(s -> s != null && !s.isBlank()).map(String::trim).collect(Collectors.joining(", ")));
        center.setDistrict(req.district());
        center.setProvince(req.province());
        center.setAddress(req.address());
        center.setPhone(req.phone());
        center.setEmail(req.email());
        center.setWebsite(req.website());
        center.setRating(req.rating());
        center.setRatingNote(req.ratingNote());
        center.setActive(req.active() == null || req.active());
    }

    /**
     * For each requested subject, the active centres that teach it, nearest first: same district
     * as the school, then same province, then anywhere else — rating (highest first) breaks ties
     * within each tier. Centres with no rating sort after rated ones at the same proximity tier.
     */
    public Map<String, List<TuitionCenter>> recommend(List<String> subjects, String district, String province, int perSubjectLimit) {
        List<TuitionCenter> active = repository.findByActiveTrueOrderByRatingDesc();
        Map<String, List<TuitionCenter>> result = new LinkedHashMap<>();
        for (String subject : subjects) {
            String needle = subject.trim().toLowerCase();
            List<TuitionCenter> matches = active.stream()
                    .filter(c -> subjectsOf(c).contains(needle))
                    .sorted(Comparator.comparingInt((TuitionCenter c) -> proximityRank(c, district, province))
                            .thenComparing(c -> -ratingOrZero(c)))
                    .limit(perSubjectLimit)
                    .toList();
            result.put(subject, matches);
        }
        return result;
    }

    private static int proximityRank(TuitionCenter c, String district, String province) {
        if (district != null && !district.isBlank() && district.equalsIgnoreCase(c.getDistrict())) return 0;
        if (province != null && !province.isBlank() && province.equalsIgnoreCase(c.getProvince())) return 1;
        return 2;
    }

    private static double ratingOrZero(TuitionCenter c) {
        return c.getRating() == null ? 0 : c.getRating();
    }

    private static Set<String> subjectsOf(TuitionCenter c) {
        if (c.getSubjectsText() == null) return Set.of();
        return Arrays.stream(c.getSubjectsText().split(","))
                .map(String::trim).map(String::toLowerCase).filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
