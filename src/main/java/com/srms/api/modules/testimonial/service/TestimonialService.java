package com.srms.api.modules.testimonial.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.testimonial.entity.Testimonial;
import com.srms.api.modules.testimonial.repository.TestimonialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TestimonialService {

    private final TestimonialRepository testimonialRepository;

    public List<Testimonial> getApproved() {
        return testimonialRepository.findByApprovedTrueOrderByCreatedAtDesc();
    }

    public List<Testimonial> getAll() {
        return testimonialRepository.findAllByOrderByCreatedAtDesc();
    }

    public Testimonial create(Testimonial testimonial) {
        testimonial.setRating(Math.max(1, Math.min(5, testimonial.getRating())));
        return testimonialRepository.save(testimonial);
    }

    public Testimonial update(String id, Testimonial patch) {
        Testimonial testimonial = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial", id));
        if (patch.getAuthorName() != null) testimonial.setAuthorName(patch.getAuthorName());
        if (patch.getAuthorRole() != null) testimonial.setAuthorRole(patch.getAuthorRole());
        if (patch.getSchoolName() != null) testimonial.setSchoolName(patch.getSchoolName());
        if (patch.getQuote() != null) testimonial.setQuote(patch.getQuote());
        if (patch.getRating() > 0) testimonial.setRating(Math.max(1, Math.min(5, patch.getRating())));
        testimonial.setApproved(patch.isApproved());
        return testimonialRepository.save(testimonial);
    }

    public void delete(String id) {
        Testimonial testimonial = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial", id));
        testimonialRepository.delete(testimonial);
    }
}
