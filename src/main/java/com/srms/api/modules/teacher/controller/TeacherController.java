package com.srms.api.modules.teacher.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.common.BulkImportResult;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.teacher.dto.TeacherDto;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import com.srms.api.modules.teacher.service.TeacherService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController @RequestMapping("/api/schools/{schoolId}/teachers") @RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    @GetMapping public ResponseEntity<ApiResponse<List<Teacher>>> getAll(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(teacherService.findAll(schoolId))); }

    /** Resolves "my own" staff record the same way class/subject assignment already does — by
     * matching the authenticated account's email against Teacher.email — so a teacher, HOD, or
     * any other staff member with a Teacher row can view and edit their own profile (contact
     * details, portrait, signature) without needing the admin-only Teachers page at all. */
    private Teacher myTeacherRecord(String schoolId, Authentication auth) {
        AppUser user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated user was not found"));
        if (user.getEmail() == null) throw new ForbiddenException("No staff profile is linked to this account");
        return teacherRepository.findByEmailIgnoreCaseAndSchoolId(user.getEmail(), schoolId)
                .map(t -> teacherService.findById(t.getId(), schoolId))
                .orElseThrow(() -> new ForbiddenException("No staff profile is linked to this account"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Teacher>> getMyProfile(@PathVariable String schoolId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(myTeacherRecord(schoolId, auth)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Teacher>> updateMyProfile(@PathVariable String schoolId, @RequestBody Map<String, String> body, Authentication auth) {
        Teacher mine = myTeacherRecord(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(teacherService.updateOwnProfile(mine.getId(), schoolId, body)));
    }

    @GetMapping("/{id}") public ResponseEntity<ApiResponse<Teacher>> getById(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(teacherService.findById(id, schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<Teacher>> create(@PathVariable String schoolId, @RequestBody TeacherDto dto) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(teacherService.create(schoolId, dto))); }
    @PostMapping("/bulk") public ResponseEntity<ApiResponse<BulkImportResult>> bulkCreate(@PathVariable String schoolId, @RequestBody List<TeacherDto> dtos) { return ResponseEntity.ok(ApiResponse.ok(teacherService.bulkCreate(schoolId, dtos))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Teacher>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody TeacherDto dto) { return ResponseEntity.ok(ApiResponse.ok(teacherService.update(id, schoolId, dto))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) { teacherService.delete(id, schoolId); return ResponseEntity.ok(ApiResponse.ok("Teacher deactivated", null)); }

    // Irreversible: erases the staff record and their own tied data (signature, teaching
    // assignments, timetable slots) — see TeacherService.deletePermanently's javadoc for
    // exactly what is and isn't touched. Restricted to school-account-manager roles.
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> deletePermanently(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        teacherService.deletePermanently(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok("Teacher permanently deleted", null));
    }
}
