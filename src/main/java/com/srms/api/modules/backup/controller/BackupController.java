package com.srms.api.modules.backup.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.backup.entity.Backup;
import com.srms.api.modules.backup.service.BackupService;
import com.srms.api.security.ModuleAccessService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/schools/{schoolId}/backups")
@RequiredArgsConstructor
public class BackupController {
    private final BackupService backupService;
    private final UserRepository userRepository;
    private final ModuleAccessService moduleAccessService;

    // Backups contain every field of every table for a school — same trust level as raw DB
    // access, so only the roles that could already see this data through the app get it.
    private static final Set<String> ADMIN_ROLES = Set.of("SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    @GetMapping
    public ResponseEntity<ApiResponse<List<Backup>>> list(@PathVariable String schoolId, Authentication auth) {
        assertAdmin(schoolId, auth, "read");
        return ResponseEntity.ok(ApiResponse.ok(backupService.list(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Backup>> create(@PathVariable String schoolId, Authentication auth) {
        assertAdmin(schoolId, auth, "full");
        Backup backup = backupService.createBackup(schoolId, actorName(auth), Backup.TriggeredBy.MANUAL);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(backup));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        assertAdmin(schoolId, auth, "read");
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
        assertAdmin(schoolId, auth, "full");
        backupService.restore(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Restore complete", null));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Void>> importBackup(@PathVariable String schoolId,
                                                            @RequestParam("file") MultipartFile file,
                                                            Authentication auth) {
        assertAdmin(schoolId, auth, "full");
        if (file.isEmpty()) {
            throw new BusinessException("Please choose a backup file to upload");
        }
        try {
            backupService.importAndRestore(schoolId, file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException("Could not read the uploaded file: " + e.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.ok("Restore complete", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        assertAdmin(schoolId, auth, "full");
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

    // The role-set gate below is the module-level check (wrapped so a super admin override can
    // supersede it); the tenant-ownership check that follows is left completely untouched.
    private void assertAdmin(String schoolId, Authentication auth, String minLevel) {
        String role = roleOf(auth);
        if (!moduleAccessService.isAllowed(schoolId, auth, "settings", minLevel, ADMIN_ROLES.contains(role.toUpperCase()))) {
            throw new ForbiddenException("Only school administrators can manage backups");
        }
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) return;
        String actorSchool = auth.getCredentials() == null ? null : auth.getCredentials().toString();
        if (!schoolId.equals(actorSchool)) {
            throw new ForbiddenException("You cannot access another school's backups");
        }
    }
}
