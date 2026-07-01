package com.srms.api.modules.ptc.service;

import com.srms.api.modules.ptc.entity.PtcMeeting;
import com.srms.api.modules.ptc.entity.PtcMember;
import com.srms.api.modules.ptc.entity.PtcTransaction;
import com.srms.api.modules.ptc.repository.PtcMeetingRepository;
import com.srms.api.modules.ptc.repository.PtcMemberRepository;
import com.srms.api.modules.ptc.repository.PtcTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class PtcService {
    private final PtcMemberRepository memberRepository;
    private final PtcMeetingRepository meetingRepository;
    private final PtcTransactionRepository transactionRepository;

    // Members
    public List<PtcMember> listMembers(String schoolId) { return memberRepository.findBySchoolIdOrderByNameAsc(schoolId); }

    public PtcMember createMember(String schoolId, PtcMember m) { m.setSchoolId(schoolId); return memberRepository.save(m); }

    public PtcMember updateMember(String schoolId, String id, PtcMember patch) {
        PtcMember m = memberRepository.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getName() != null) m.setName(patch.getName());
        if (patch.getPosition() != null) m.setPosition(patch.getPosition());
        if (patch.getMemberType() != null) m.setMemberType(patch.getMemberType());
        if (patch.getStudentName() != null) m.setStudentName(patch.getStudentName());
        if (patch.getEmail() != null) m.setEmail(patch.getEmail());
        if (patch.getPhone() != null) m.setPhone(patch.getPhone());
        if (patch.getTermStartDate() != null) m.setTermStartDate(patch.getTermStartDate());
        if (patch.getTermEndDate() != null) m.setTermEndDate(patch.getTermEndDate());
        if (patch.getStatus() != null) m.setStatus(patch.getStatus());
        return memberRepository.save(m);
    }

    public void deleteMember(String schoolId, String id) {
        memberRepository.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).ifPresent(memberRepository::delete);
    }

    // Meetings
    public List<PtcMeeting> listMeetings(String schoolId, boolean publishedOnly) {
        return publishedOnly
                ? meetingRepository.findBySchoolIdAndPublishedTrueOrderByMeetingDateDesc(schoolId)
                : meetingRepository.findBySchoolIdOrderByMeetingDateDesc(schoolId);
    }

    public PtcMeeting createMeeting(String schoolId, PtcMeeting meeting) { meeting.setSchoolId(schoolId); return meetingRepository.save(meeting); }

    public PtcMeeting updateMeeting(String schoolId, String id, PtcMeeting patch) {
        PtcMeeting meeting = meetingRepository.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getAgenda() != null) meeting.setAgenda(patch.getAgenda());
        if (patch.getMinutes() != null) meeting.setMinutes(patch.getMinutes());
        if (patch.getDecisions() != null) meeting.setDecisions(patch.getDecisions());
        if (patch.getStatus() != null) meeting.setStatus(patch.getStatus());
        if (patch.getAttendeesCount() > 0) meeting.setAttendeesCount(patch.getAttendeesCount());
        return meetingRepository.save(meeting);
    }

    public PtcMeeting publishMeeting(String schoolId, String id) {
        PtcMeeting meeting = meetingRepository.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        meeting.setPublished(true);
        return meetingRepository.save(meeting);
    }

    // Transactions
    public List<PtcTransaction> listTransactions(String schoolId) { return transactionRepository.findBySchoolIdOrderByDateDesc(schoolId); }

    public PtcTransaction createTransaction(String schoolId, PtcTransaction t) { t.setSchoolId(schoolId); return transactionRepository.save(t); }

    public PtcTransaction updateTransaction(String schoolId, String id, PtcTransaction patch) {
        PtcTransaction t = transactionRepository.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getType() != null) t.setType(patch.getType());
        if (patch.getCategory() != null) t.setCategory(patch.getCategory());
        if (patch.getDescription() != null) t.setDescription(patch.getDescription());
        if (patch.getAmount() != null) t.setAmount(patch.getAmount());
        if (patch.getStatus() != null) t.setStatus(patch.getStatus());
        return transactionRepository.save(t);
    }
}
