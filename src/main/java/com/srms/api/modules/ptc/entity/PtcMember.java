package com.srms.api.modules.ptc.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "ptc_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PtcMember extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String name;
    private String position; // Chairperson, Vice Chairperson, Secretary, Treasurer, Member
    private String memberType; // PARENT, TEACHER, ADMIN
    private String studentName;
    private String email;
    private String phone;
    private String termStartDate;
    private String termEndDate;
    @Builder.Default private String status = "ACTIVE";
}
