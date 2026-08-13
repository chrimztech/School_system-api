package com.srms.api.modules.hostel.service;

import com.srms.api.modules.hostel.entity.HostelAllocation;
import com.srms.api.modules.hostel.entity.HostelLeave;
import com.srms.api.modules.hostel.entity.HostelRoom;
import com.srms.api.modules.hostel.repository.HostelAllocationRepository;
import com.srms.api.modules.hostel.repository.HostelLeaveRepository;
import com.srms.api.modules.hostel.repository.HostelRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class HostelService {
    private final HostelRoomRepository roomRepo;
    private final HostelAllocationRepository allocationRepo;
    private final HostelLeaveRepository leaveRepo;

    public List<HostelRoom> getRooms(String schoolId) { return roomRepo.findBySchoolId(schoolId); }
    public HostelRoom createRoom(String schoolId, HostelRoom room) { room.setSchoolId(schoolId); if (room.getStatus() == null) room.setStatus("ACTIVE"); return roomRepo.save(room); }
    public HostelRoom updateRoom(String schoolId, String id, HostelRoom updated) {
        HostelRoom room = roomRepo.findById(id).filter(r -> r.getSchoolId().equals(schoolId)).orElseThrow();
        room.setRoomNumber(updated.getRoomNumber()); room.setHostelName(updated.getHostelName()); room.setRoomType(updated.getRoomType()); room.setCapacity(updated.getCapacity()); room.setGender(updated.getGender()); room.setFloor(updated.getFloor()); room.setStatus(updated.getStatus());
        return roomRepo.save(room);
    }
    public void deleteRoom(String schoolId, String id) { roomRepo.findById(id).filter(r -> r.getSchoolId().equals(schoolId)).ifPresent(roomRepo::delete); }

    public List<HostelAllocation> getAllocations(String schoolId) { return allocationRepo.findBySchoolId(schoolId); }
    public HostelAllocation allocate(String schoolId, HostelAllocation allocation) {
        allocation.setSchoolId(schoolId);
        if (allocation.getCheckInDate() == null) allocation.setCheckInDate(LocalDate.now());
        allocation.setStatus("ACTIVE");
        HostelRoom room = roomRepo.findById(allocation.getRoomId()).filter(r -> r.getSchoolId().equals(schoolId)).orElse(null);
        if (room != null) { room.setOccupiedBeds(room.getOccupiedBeds() + 1); roomRepo.save(room); }
        return allocationRepo.save(allocation);
    }
    public HostelAllocation vacate(String schoolId, String id) {

        HostelAllocation allocation = allocationRepo.findById(id).filter(a -> a.getSchoolId().equals(schoolId)).orElseThrow();
        allocation.setStatus("VACATED"); allocation.setCheckOutDate(LocalDate.now());
        HostelRoom room = roomRepo.findById(allocation.getRoomId()).filter(r -> r.getSchoolId().equals(schoolId)).orElse(null);
        if (room != null) { room.setOccupiedBeds(Math.max(0, room.getOccupiedBeds() - 1)); roomRepo.save(room); }
        return allocationRepo.save(allocation);
    }

    public List<HostelLeave> getLeaves(String schoolId) { return leaveRepo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public HostelLeave createLeave(String schoolId, HostelLeave leave) { leave.setSchoolId(schoolId); leave.setStatus("PENDING"); return leaveRepo.save(leave); }
    public HostelLeave updateLeaveStatus(String schoolId, String id, String status) {
        HostelLeave leave = leaveRepo.findById(id).filter(l -> l.getSchoolId().equals(schoolId)).orElseThrow();
        leave.setStatus(status);
        return leaveRepo.save(leave);
    }

    public HostelAllocation updateSignInStatus(String schoolId, String id, String status) {
        HostelAllocation allocation = allocationRepo.findById(id).filter(a -> a.getSchoolId().equals(schoolId)).orElseThrow();
        allocation.setSignInStatus(status);
        return allocationRepo.save(allocation);
    }
}
