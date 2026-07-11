package com.srms.api.modules.testimonial.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.testimonial.entity.Testimonial;
import com.srms.api.modules.testimonial.service.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Any signed-in user can submit a review of the system. Not under /api/public/** (requires a
 * valid JWT, same as every other authenticated endpoint) or /api/platform/** (no super-admin
 * needed to submit — just to approve). Submissions always land unapproved so they only reach the
 * public login page after a platform admin reviews them.
 */
@RestController
@RequestMapping("/api/testimonials")
@RequiredArgsConstructor
public class TestimonialSubmissionController {

    private final TestimonialService testimonialService;

    @PostMapping
    public ResponseEntity<ApiResponse<Testimonial>> submit(@RequestBody Testimonial testimonial) {
        testimonial.setApproved(false);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(testimonialService.create(testimonial)));
    }
}
