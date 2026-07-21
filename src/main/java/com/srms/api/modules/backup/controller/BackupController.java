package com.srms.api.modules.backup.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.backup.entity.Backup;
import com.srms.api.modules.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/schools/{schoolId}/backups")
@RequiredArgsConstructor
public class BackupController {
    private final BackupService backupService;
    private final UserRepository userRepository;

    // Backups contain every field of every table for a school — same trust level as raw DB
    // access, so only the roles that could already see this data through the app get it.
    private static final Set<String> ADMIN_ROLES = Set.of("SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    @GetMapping
    public ResponseEntity<ApiResponse<List<Backup>>> list(@PathVariable String schoolId, Authentication auth) {
        assertAdmin(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(backupService.list(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Backup>> create(@PathVariable String schoolId, Authentication auth) {
        assertAdmin(schoolId, auth);
        Backup backup = backupService.createBackup(schoolId, actorName(auth), Backup.TriggeredBy.MANUAL);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(backup));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        assertAdmin(schoolId, auth);
        Backup backup = backupService.get(schoolId, id);
        byte[] file = backupService.readFile(schoolId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(backup.getFileName()).build().toString())
                .body(file);
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        assertAdmin(schoolId, auth);
        backupService.restore(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Restore complete", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        assertAdmin(schoolId, auth);
        backupService.delete(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Backup deleted", null));
    }

    private String actorName(Authentication auth) {
        return userRepository.findById(auth.getName())
                .map(AppUser::getName)
                .orElse(auth.getName());
    }

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", "")).orElse("");
    }

    private void assertAdmin(String schoolId, Authentication auth) {
        String role = roleOf(auth);
        if (!ADMIN_ROLES.contains(role.toUpperCase())) {
            throw new ForbiddenException("Only school administrators can manage backups");
        }
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) return;
        String actorSchool = auth.getCredentials() == null ? null : auth.getCredentials().toString();
        if (!schoolId.equals(actorSchool)) {
            throw new ForbiddenException("You cannot access another school's backups");
        }
    }
}
