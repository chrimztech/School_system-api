package com.srms.api.modules.ptc.repository;
import com.srms.api.modules.ptc.entity.PtcMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface PtcMeetingRepository extends JpaRepository<PtcMeeting, String> {
    List<PtcMeeting> findBySchoolIdOrderByMeetingDateDesc(String schoolId);
    List<PtcMeeting> findBySchoolIdAndPublishedTrueOrderByMeetingDateDesc(String schoolId);
}
