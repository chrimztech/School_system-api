package com.srms.api.modules.testimonial.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.testimonial.entity.Testimonial;
import com.srms.api.modules.testimonial.service.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Unauthenticated — mounted under /api/public/** (permitAll in SecurityConfig) so the pre-login page can read it. */
@RestController
@RequestMapping("/api/public/testimonials")
@RequiredArgsConstructor
public class TestimonialController {

    private final TestimonialService testimonialService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Testimonial>>> getApproved() {
        return ResponseEntity.ok(ApiResponse.ok(testimonialService.getApproved()));
    }
}
