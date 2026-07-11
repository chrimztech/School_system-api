package com.srms.api.modules.testimonial.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.testimonial.entity.Testimonial;
import com.srms.api.modules.testimonial.service.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/testimonials")
@RequiredArgsConstructor
public class PlatformTestimonialController {

    private final TestimonialService testimonialService;

    private boolean isSuper(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Testimonial>>> getAll(Authentication auth) {
        if (!isSuper(auth)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only system administrators can manage testimonials"));
        }
        return ResponseEntity.ok(ApiResponse.ok(testimonialService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Testimonial>> create(Authentication auth, @RequestBody Testimonial testimonial) {
        if (!isSuper(auth)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only system administrators can manage testimonials"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(testimonialService.create(testimonial)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Testimonial>> update(Authentication auth, @PathVariable String id, @RequestBody Testimonial patch) {
        if (!isSuper(auth)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only system administrators can manage testimonials"));
        }
        return ResponseEntity.ok(ApiResponse.ok(testimonialService.update(id, patch)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication auth, @PathVariable String id) {
        if (!isSuper(auth)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only system administrators can manage testimonials"));
        }
        testimonialService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
