package com.srms.api.modules.teacher.service;
import com.srms.api.common.BulkImportResult;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.entity.Department;
import com.srms.api.modules.academic.entity.SchoolClass;
import com.srms.api.modules.academic.repository.DepartmentRepository;
import com.srms.api.modules.academic.repository.SchoolClassRepository;
import com.srms.api.modules.academic.repository.TeacherClassSubjectRepository;
import com.srms.api.modules.teacher.dto.TeacherDto;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.entity.TeacherSignatureAsset;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import com.srms.api.modules.teacher.repository.TeacherSignatureAssetRepository;
import com.srms.api.modules.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final TeacherSignatureAssetRepository signatureAssetRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final TimetableRepository timetableRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final DepartmentRepository departmentRepository;

    // Signature intentionally left null here — see TeacherSignatureAsset's javadoc. A teacher
    // LIST is used to populate ordinary staff-list UIs, and shouldn't carry every teacher's
    // base64 signature image just because one caller (the report card) needs one teacher's.
    public List<Teacher> findAll(String schoolId) { return teacherRepository.findBySchoolId(schoolId); }

    public Teacher findById(String id, String schoolId) {
        Teacher t = teacherRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
        signatureAssetRepository.findById(id).ifPresent(asset -> t.setSignatureUrl(asset.getSignatureUrl()));
        return t;
    }
    public Teacher create(String schoolId, TeacherDto dto) {
        assertEmailNotTaken(schoolId, dto.getEmail(), null);
        Teacher t = new Teacher();
        t.setSchoolId(schoolId);
        long count = teacherRepository.countBySchoolIdAndStatus(schoolId, Teacher.TeacherStatus.active);
        t.setStaffNumber("STF-" + schoolId.toUpperCase() + "-" + String.format("%03d", count + 1));
        t.setStatus(Teacher.TeacherStatus.active);
        mapDto(t, dto);
        Teacher saved = teacherRepository.save(t);
        saveSignatureIfProvided(saved.getId(), dto.getSignatureUrl());
        // save()'s returned instance can be a JPA merge() copy rather than the same object `t`
        // was — @Transient fields aren't guaranteed to survive that copy, so set it explicitly
        // from the known dto value rather than trusting it carried over.
        if (dto.getSignatureUrl() != null) saved.setSignatureUrl(dto.getSignatureUrl());
        return saved;
    }

    private void saveSignatureIfProvided(String teacherId, String signatureUrl) {
        if (signatureUrl == null) return;
        TeacherSignatureAsset asset = signatureAssetRepository.findById(teacherId)
                .orElse(TeacherSignatureAsset.builder().teacherId(teacherId).build());
        asset.setSignatureUrl(signatureUrl);
        signatureAssetRepository.save(asset);
    }

    /**
     * Class/subject assignment and HOD department-verification are keyed on a single Teacher
     * row per email (Teacher.email, via findByEmailIgnoreCaseAndSchoolId — see AuthService's
     * ensureTeacherProfile) — a second row sharing that email doesn't just show wrong data,
     * it makes those single-result lookups ambiguous. Enforced here so it applies uniformly to
     * the create form, edits, and bulk import, not just the login-account auto-provisioning path.
     */
    private void assertEmailNotTaken(String schoolId, String email, String excludingTeacherId) {
        if (email == null || email.isBlank()) return;
        teacherRepository.findByEmailIgnoreCaseAndSchoolId(email, schoolId).ifPresent(existing -> {
            if (!existing.getId().equals(excludingTeacherId)) {
                throw new BusinessException("A teacher with this email already exists: " + existing.getFirstName() + " " + existing.getLastName());
            }
        });
    }
    public BulkImportResult bulkCreate(String schoolId, List<TeacherDto> dtos) {
        int imported = 0;
        List<BulkImportResult.RowError> errors = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            try {
                create(schoolId, dtos.get(i));
                imported++;
            } catch (Exception e) {
                errors.add(new BulkImportResult.RowError(i, e.getMessage()));
            }
        }
        return new BulkImportResult(imported, errors);
    }

    public Teacher update(String id, String schoolId, TeacherDto dto) {
        Teacher t = findById(id, schoolId);
        assertEmailNotTaken(schoolId, dto.getEmail(), id);
        mapDto(t, dto);
        Teacher saved = teacherRepository.save(t);
        saveSignatureIfProvided(id, dto.getSignatureUrl());
        if (dto.getSignatureUrl() != null) saved.setSignatureUrl(dto.getSignatureUrl());
        return saved;
    }
    public void delete(String id, String schoolId) {
        Teacher t = findById(id, schoolId); t.setStatus(Teacher.TeacherStatus.inactive); teacherRepository.save(t);
    }

    /**
     * Permanently erases a staff record — see StudentService.deletePermanently's javadoc for
     * the same reasoning (this is the separate, explicit, irreversible action; the soft
     * delete above is what ordinary "Delete" means). Removes what's genuinely this teacher's
     * own data (their signature, their subject/class teaching assignments, their timetable
     * slots) and clears references to them elsewhere (a class's "class teacher", a
     * department's head) rather than deleting those records outright. Deliberately leaves
     * their authorship on academic history alone — TermGrade.teacherId, Assessment.teacherId,
     * and AttendanceRecord.teacherId record who graded/created/marked something that belongs
     * to a student's record, not the teacher's; that history must survive a staff departure.
     */
    @Transactional
    public void deletePermanently(String id, String schoolId) {
        Teacher t = findById(id, schoolId);

        signatureAssetRepository.findById(id).ifPresent(signatureAssetRepository::delete);
        teacherClassSubjectRepository.deleteAll(teacherClassSubjectRepository.findByTeacherIdAndSchoolId(id, schoolId));
        timetableRepository.deleteAll(timetableRepository.findBySchoolIdAndTeacherId(schoolId, id));

        for (SchoolClass c : schoolClassRepository.findBySchoolIdAndClassTeacherId(schoolId, id)) {
            c.setClassTeacherId(null);
            c.setClassTeacherName(null);
            schoolClassRepository.save(c);
        }
        for (Department d : departmentRepository.findBySchoolIdAndHeadTeacherId(schoolId, id)) {
            d.setHeadTeacherId(null);
            departmentRepository.save(d);
        }

        teacherRepository.delete(t);
    }
    private void mapDto(Teacher t, TeacherDto dto) {
        if (dto.getFirstName() != null) t.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) t.setLastName(dto.getLastName());
        if (dto.getEmail() != null) t.setEmail(dto.getEmail());
        if (dto.getPhone() != null) t.setPhone(dto.getPhone());
        if (dto.getSubject() != null) t.setSubject(dto.getSubject());
        if (dto.getQualification() != null) t.setQualification(dto.getQualification());
        if (dto.getDepartment() != null) t.setDepartment(dto.getDepartment());
        if (dto.getDateJoined() != null) t.setDateJoined(dto.getDateJoined());
        if (dto.getGender() != null) t.setGender(dto.getGender());
        if (dto.getNationalId() != null) t.setNationalId(dto.getNationalId());
        if (dto.getStatus() != null) t.setStatus(Teacher.TeacherStatus.valueOf(dto.getStatus()));
        if (dto.getSalary() > 0) t.setSalary(dto.getSalary());
        t.setEmergencyContactName(dto.getEmergencyContactName());
        t.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        t.setProfessionalLicenseNo(dto.getProfessionalLicenseNo());
        t.setTeachingExperienceYears(dto.getTeachingExperienceYears());
        t.setBankName(dto.getBankName());
        t.setBankAccount(dto.getBankAccount());
        t.setAddress(dto.getAddress());
        t.setContractType(dto.getContractType());
        t.setContractEndDate(dto.getContractEndDate());
        t.setSalaryBand(dto.getSalaryBand());
        t.setTpin(dto.getTpin());
        t.setPaymentMethod(dto.getPaymentMethod());
        t.setNapsaEnrolled(dto.getNapsaEnrolled());
        if (dto.getSignatureUrl() != null) t.setSignatureUrl(dto.getSignatureUrl());
    }
}
