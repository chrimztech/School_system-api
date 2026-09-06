package com.srms.api.modules.school.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Head teacher signature and school stamp — split out of the {@link School} entity so that
 * loading a school (which happens on effectively every request, via tenant-context resolution)
 * doesn't also pull two base64-encoded images (up to ~13MB each) that only the report-card page
 * and the Settings branding form ever actually need. Keyed 1:1 by schoolId, deliberately not a
 * JPA relationship (no @OneToOne) — a plain separate table with its own repository means these
 * columns are never part of the SQL Hibernate runs for School unless this repository is
 * explicitly queried, with no lazy-loading/bytecode-enhancement caveats to worry about.
 */
@Entity
@Table(name = "school_branding_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolBrandingAsset {
    @Id
    @Column(name = "school_id")
    private String schoolId;

    @Column(columnDefinition = "TEXT")
    private String headTeacherSignatureUrl;

    @Column(columnDefinition = "TEXT")
    private String schoolStampUrl;
}
