package com.srms.api.modules.school.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.school.dto.SchoolDto;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SchoolService {
    private final SchoolRepository schoolRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SchoolDto> findAll() {
        return schoolRepository.findByActiveTrue().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SchoolDto findById(String id) {
        return toDto(findEntityById(id));
    }

    public SchoolDto create(SchoolDto dto) {
        School school = new School();
        mapDto(school, dto);
        if (school.getSubscriptionStatus() == null) {
            school.setSubscriptionStatus(School.SubscriptionStatus.trial);
        }
        applyDefaults(school);
        return toDto(schoolRepository.save(school));
    }

    public SchoolDto update(String id, SchoolDto dto) {
        School school = findEntityById(id);
        mapDto(school, dto);
        applyDefaults(school);
        return toDto(schoolRepository.save(school));
    }

    public void delete(String id) {
        School school = findEntityById(id);
        school.setActive(false);
        schoolRepository.save(school);
    }

    private School findEntityById(String id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School", id));
    }

    public SchoolDto toDto(School school) {
        return SchoolDto.builder()
                .id(school.getId())
                .name(school.getName())
                .shortCode(school.getShortCode())
                .motto(school.getMotto())
                .district(school.getDistrict())
                .province(school.getProvince())
                .type(school.getType())
                .ownership(school.getOwnership())
                .category(school.getCategory())
                .gender(school.getGender())
                .curriculum(school.getCurriculum())
                .languageOfInstruction(school.getLanguageOfInstruction())
                .email(school.getEmail())
                .phone(school.getPhone())
                .altPhone(school.getAltPhone())
                .website(school.getWebsite())
                .physicalAddress(school.getPhysicalAddress())
                .poBox(school.getPoBox())
                .city(school.getCity())
                .postalCode(school.getPostalCode())
                .gpsCoordinates(school.getGpsCoordinates())
                .headTeacher(school.getHeadTeacher())
                .headTeacherEmail(school.getHeadTeacherEmail())
                .deputyHead(school.getDeputyHead())
                .boardChair(school.getBoardChair())
                .primaryColor(school.getPrimaryColor())
                .secondaryColor(school.getSecondaryColor())
                .accentColor(school.getAccentColor())
                .fontFamily(school.getFontFamily())
                .reportFooter(school.getReportFooter())
                .logoUrl(school.getLogoUrl())
                .registrationNo(school.getRegistrationNo())
                .tpinNo(school.getTpinNo())
                .moeCode(school.getMoeCode())
                .examCentreNo(school.getExamCentreNo())
                .yearFounded(school.getYearFounded())
                .weekStart(school.getWeekStart())
                .gradingScale(school.getGradingScale())
                .passMark(school.getPassMark())
                .currency(school.getCurrency())
                .bankName(school.getBankName())
                .bankAccount(school.getBankAccount())
                .bankBranch(school.getBankBranch())
                .termStart(school.getTermStart())
                .termEnd(school.getTermEnd())
                .currentTerm(school.getCurrentTerm())
                .currentYear(school.getCurrentYear())
                .totalStudents(school.getTotalStudents())
                .totalTeachers(school.getTotalTeachers())
                .totalClasses(school.getTotalClasses())
                .subscriptionStatus(school.getSubscriptionStatus() == null ? null : school.getSubscriptionStatus().name())
                .planId(school.getPlanId())
                .billingCycle(school.getBillingCycle())
                .amount(school.getSubscriptionAmount())
                .campusLimit(school.getCampusLimit())
                .nextInvoiceDate(school.getNextInvoiceDate())
                .renewalDate(school.getRenewalDate())
                .learnerLimit(school.getLearnerLimit())
                .smsQuota(school.getSmsQuota())
                .smsUsed(school.getSmsUsed())
                .supportLevel(school.getSupportLevel())
                .billingContact(school.getBillingContact())
                .notes(school.getSubscriptionNotes())
                .offlineMode(school.getOfflineMode())
                .active(school.isActive())
                .levels(readJson(school.getLevelsJson(), new TypeReference<List<String>>() {}, new ArrayList<>()))
                .campuses(readJson(school.getCampusesJson(), new TypeReference<List<SchoolDto.CampusDto>>() {}, new ArrayList<>()))
                .features(readJson(school.getFeaturesJson(), new TypeReference<Map<String, Boolean>>() {}, new LinkedHashMap<>()))
                .build();
    }

    private void mapDto(School school, SchoolDto dto) {
        if (dto.getName() != null) school.setName(dto.getName());
        if (dto.getShortCode() != null) school.setShortCode(dto.getShortCode());
        if (dto.getMotto() != null) school.setMotto(dto.getMotto());
        if (dto.getDistrict() != null) school.setDistrict(dto.getDistrict());
        if (dto.getProvince() != null) school.setProvince(dto.getProvince());
        if (dto.getType() != null) school.setType(dto.getType());
        if (dto.getOwnership() != null) school.setOwnership(dto.getOwnership());
        if (dto.getCategory() != null) school.setCategory(dto.getCategory());
        if (dto.getGender() != null) school.setGender(dto.getGender());
        if (dto.getCurriculum() != null) school.setCurriculum(dto.getCurriculum());
        if (dto.getLanguageOfInstruction() != null) school.setLanguageOfInstruction(dto.getLanguageOfInstruction());
        if (dto.getEmail() != null) school.setEmail(dto.getEmail());
        if (dto.getPhone() != null) school.setPhone(dto.getPhone());
        if (dto.getAltPhone() != null) school.setAltPhone(dto.getAltPhone());
        if (dto.getWebsite() != null) school.setWebsite(dto.getWebsite());
        if (dto.getPhysicalAddress() != null) school.setPhysicalAddress(dto.getPhysicalAddress());
        if (dto.getPoBox() != null) school.setPoBox(dto.getPoBox());
        if (dto.getCity() != null) school.setCity(dto.getCity());
        if (dto.getPostalCode() != null) school.setPostalCode(dto.getPostalCode());
        if (dto.getGpsCoordinates() != null) school.setGpsCoordinates(dto.getGpsCoordinates());
        if (dto.getHeadTeacher() != null) school.setHeadTeacher(dto.getHeadTeacher());
        if (dto.getHeadTeacherEmail() != null) school.setHeadTeacherEmail(dto.getHeadTeacherEmail());
        if (dto.getDeputyHead() != null) school.setDeputyHead(dto.getDeputyHead());
        if (dto.getBoardChair() != null) school.setBoardChair(dto.getBoardChair());
        if (dto.getPrimaryColor() != null) school.setPrimaryColor(dto.getPrimaryColor());
        if (dto.getSecondaryColor() != null) school.setSecondaryColor(dto.getSecondaryColor());
        if (dto.getAccentColor() != null) school.setAccentColor(dto.getAccentColor());
        if (dto.getFontFamily() != null) school.setFontFamily(dto.getFontFamily());
        if (dto.getReportFooter() != null) school.setReportFooter(dto.getReportFooter());
        if (dto.getLogoUrl() != null) school.setLogoUrl(dto.getLogoUrl());
        if (dto.getRegistrationNo() != null) school.setRegistrationNo(dto.getRegistrationNo());
        if (dto.getTpinNo() != null) school.setTpinNo(dto.getTpinNo());
        if (dto.getMoeCode() != null) school.setMoeCode(dto.getMoeCode());
        if (dto.getExamCentreNo() != null) school.setExamCentreNo(dto.getExamCentreNo());
        if (dto.getYearFounded() != null) school.setYearFounded(dto.getYearFounded());
        if (dto.getWeekStart() != null) school.setWeekStart(dto.getWeekStart());
        if (dto.getGradingScale() != null) school.setGradingScale(dto.getGradingScale());
        if (dto.getPassMark() != null) school.setPassMark(dto.getPassMark());
        if (dto.getCurrency() != null) school.setCurrency(dto.getCurrency());
        if (dto.getBankName() != null) school.setBankName(dto.getBankName());
        if (dto.getBankAccount() != null) school.setBankAccount(dto.getBankAccount());
        if (dto.getBankBranch() != null) school.setBankBranch(dto.getBankBranch());
        if (dto.getTermStart() != null) school.setTermStart(dto.getTermStart());
        if (dto.getTermEnd() != null) school.setTermEnd(dto.getTermEnd());
        if (dto.getCurrentTerm() != null) school.setCurrentTerm(dto.getCurrentTerm());
        if (dto.getCurrentYear() != null) school.setCurrentYear(dto.getCurrentYear());
        if (dto.getTotalStudents() != null) school.setTotalStudents(dto.getTotalStudents());
        if (dto.getTotalTeachers() != null) school.setTotalTeachers(dto.getTotalTeachers());
        if (dto.getTotalClasses() != null) school.setTotalClasses(dto.getTotalClasses());
        if (dto.getPlanId() != null) school.setPlanId(dto.getPlanId());
        if (dto.getBillingCycle() != null) school.setBillingCycle(dto.getBillingCycle());
        if (dto.getAmount() != null) school.setSubscriptionAmount(dto.getAmount());
        if (dto.getCampusLimit() != null) school.setCampusLimit(dto.getCampusLimit());
        if (dto.getNextInvoiceDate() != null) school.setNextInvoiceDate(dto.getNextInvoiceDate());
        if (dto.getRenewalDate() != null) school.setRenewalDate(dto.getRenewalDate());
        if (dto.getLearnerLimit() != null) school.setLearnerLimit(dto.getLearnerLimit());
        if (dto.getSmsQuota() != null) school.setSmsQuota(dto.getSmsQuota());
        if (dto.getSmsUsed() != null) school.setSmsUsed(dto.getSmsUsed());
        if (dto.getSupportLevel() != null) school.setSupportLevel(dto.getSupportLevel());
        if (dto.getBillingContact() != null) school.setBillingContact(dto.getBillingContact());
        if (dto.getNotes() != null) school.setSubscriptionNotes(dto.getNotes());
        if (dto.getOfflineMode() != null) school.setOfflineMode(dto.getOfflineMode());
        if (dto.getActive() != null) school.setActive(dto.getActive());
        if (dto.getSubscriptionStatus() != null && !dto.getSubscriptionStatus().isBlank()) {
            school.setSubscriptionStatus(normaliseStatus(dto.getSubscriptionStatus()));
        }
        if (dto.getLevels() != null) school.setLevelsJson(writeJson(dto.getLevels()));
        if (dto.getCampuses() != null) school.setCampusesJson(writeJson(dto.getCampuses()));
        if (dto.getFeatures() != null) school.setFeaturesJson(writeJson(dto.getFeatures()));
    }

    private void applyDefaults(School school) {
        String planId = normalisePlanId(school.getPlanId());
        String billingCycle = normaliseBillingCycle(school.getBillingCycle());

        school.setPlanId(planId);
        school.setBillingCycle(billingCycle);

        if (school.getSubscriptionStatus() == null) {
            school.setSubscriptionStatus(School.SubscriptionStatus.trial);
        }
        if (school.getCurrentTerm() <= 0) {
            school.setCurrentTerm(1);
        }
        if (school.getCurrentYear() <= 0) {
            school.setCurrentYear(Year.now().getValue());
        }
        if (school.getCampusLimit() == null || school.getCampusLimit() <= 0) {
            school.setCampusLimit(defaultCampusLimit(planId));
        }
        if (school.getLearnerLimit() <= 0) {
            school.setLearnerLimit(defaultLearnerLimit(planId));
        }
        if (school.getSmsQuota() <= 0) {
            school.setSmsQuota(defaultSmsQuota(planId));
        }
        if (school.getSubscriptionAmount() == null || school.getSubscriptionAmount() <= 0) {
            school.setSubscriptionAmount(defaultAmount(planId, billingCycle));
        }
        if (school.getSupportLevel() == null || school.getSupportLevel().isBlank()) {
            school.setSupportLevel(defaultSupportLevel(planId));
        }
        if (school.getCurrency() == null || school.getCurrency().isBlank()) {
            school.setCurrency("ZMW");
        }
        if (school.getOfflineMode() == null) {
            school.setOfflineMode(Boolean.FALSE);
        }
        if (school.getLevelsJson() == null || school.getLevelsJson().isBlank()) {
            school.setLevelsJson(writeJson(defaultLevelsForType(school.getType())));
        }
        if (school.getCampusesJson() == null || school.getCampusesJson().isBlank()) {
            school.setCampusesJson(writeJson(defaultCampuses(school)));
        }
        if (school.getFeaturesJson() == null || school.getFeaturesJson().isBlank()) {
            school.setFeaturesJson(writeJson(defaultFeatures(planId)));
        }
    }

    private School.SubscriptionStatus normaliseStatus(String value) {
        return switch ((value == null ? "" : value).trim().toLowerCase()) {
            case "active" -> School.SubscriptionStatus.active;
            case "past_due" -> School.SubscriptionStatus.past_due;
            case "suspended" -> School.SubscriptionStatus.suspended;
            default -> School.SubscriptionStatus.trial;
        };
    }

    private String normalisePlanId(String planId) {
        return switch ((planId == null ? "" : planId).trim().toLowerCase()) {
            case "growth", "advanced", "enterprise" -> planId.trim().toLowerCase();
            default -> "core";
        };
    }

    private String normaliseBillingCycle(String billingCycle) {
        return "annual".equalsIgnoreCase(billingCycle) ? "annual" : "monthly";
    }

    private int defaultCampusLimit(String planId) {
        return switch (planId) {
            case "growth" -> 2;
            case "advanced" -> 5;
            case "enterprise" -> 20;
            default -> 1;
        };
    }

    private int defaultLearnerLimit(String planId) {
        return switch (planId) {
            case "growth" -> 1000;
            case "advanced" -> 1500;
            case "enterprise" -> 5000;
            default -> 500;
        };
    }

    private int defaultSmsQuota(String planId) {
        return switch (planId) {
            case "growth" -> 10000;
            case "advanced" -> 20000;
            case "enterprise" -> 50000;
            default -> 2500;
        };
    }

    private int defaultAmount(String planId, String billingCycle) {
        boolean annual = "annual".equalsIgnoreCase(billingCycle);
        return switch (planId) {
            case "growth" -> annual ? 42000 : 4200;
            case "advanced" -> annual ? 82000 : 8200;
            case "enterprise" -> annual ? 145000 : 14500;
            default -> annual ? 19000 : 1900;
        };
    }

    private String defaultSupportLevel(String planId) {
        return switch (planId) {
            case "enterprise" -> "Dedicated";
            case "growth", "advanced" -> "Priority";
            default -> "Standard";
        };
    }

    private List<String> defaultLevelsForType(String type) {
        String next = type == null ? "PRIMARY" : type.trim().toUpperCase();
        return switch (next) {
            case "NURSERY" -> List.of("ECE");
            case "SECONDARY" -> List.of("JUNIOR_SECONDARY", "SENIOR_SECONDARY");
            case "COMBINED" -> List.of("PRIMARY", "JUNIOR_SECONDARY", "SENIOR_SECONDARY");
            case "FULL" -> List.of("ECE", "PRIMARY", "JUNIOR_SECONDARY", "SENIOR_SECONDARY");
            default -> List.of("PRIMARY");
        };
    }

    private List<SchoolDto.CampusDto> defaultCampuses(School school) {
        return List.of(SchoolDto.CampusDto.builder()
                .id((school.getShortCode() == null ? "school" : school.getShortCode().toLowerCase()) + "-main")
                .name((school.getName() == null ? "School" : school.getName()) + " Main Campus")
                .code((school.getShortCode() == null ? "SCH" : school.getShortCode()) + "1")
                .district(school.getDistrict())
                .city(school.getCity())
                .address(school.getPhysicalAddress())
                .phone(school.getPhone())
                .status("active")
                .levels(defaultLevelsForType(school.getType()))
                .studentCount(school.getTotalStudents())
                .teacherCount(school.getTotalTeachers())
                .build());
    }

    private Map<String, Boolean> defaultFeatures(String planId) {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("sms", true);
        features.put("momo", true);
        features.put("offlineMode", true);

        if (!"core".equals(planId)) {
            features.put("ussd", true);
            features.put("ecz", true);
            features.put("library", true);
            features.put("transport", true);
            features.put("inventory", true);
            features.put("bursaries", true);
            features.put("canteen", true);
            features.put("lostFound", true);
            features.put("multiCurrency", true);
        }
        if ("advanced".equals(planId) || "enterprise".equals(planId)) {
            features.put("hostel", true);
            features.put("hr", true);
            features.put("studentWelfare", true);
            features.put("staffDevelopment", true);
            features.put("facilities", true);
            features.put("procurement", true);
            features.put("vendorManagement", true);
            features.put("security", true);
            features.put("compliance", true);
            features.put("reporting", true);
            features.put("analytics", true);
            features.put("strategicPlan", true);
        }
        if ("enterprise".equals(planId)) {
            features.put("districtManagement", true);
            features.put("customBranding", true);
        }
        return features;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to write school metadata", exception);
        }
    }

    private <T> T readJson(String raw, TypeReference<T> typeReference, T fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(raw, typeReference);
        } catch (Exception exception) {
            return fallback;
        }
    }
}
