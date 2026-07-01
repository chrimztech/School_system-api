package com.srms.api.modules.ptc.repository;
import com.srms.api.modules.ptc.entity.PtcMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface PtcMemberRepository extends JpaRepository<PtcMember, String> {
    List<PtcMember> findBySchoolIdOrderByNameAsc(String schoolId);
}
