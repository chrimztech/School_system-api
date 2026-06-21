package com.srms.api.modules.transport.service;

import com.srms.api.modules.transport.entity.TransportEnrolment;
import com.srms.api.modules.transport.entity.TransportRoute;
import com.srms.api.modules.transport.entity.Vehicle;
import com.srms.api.modules.transport.repository.TransportEnrolmentRepository;
import com.srms.api.modules.transport.repository.TransportRouteRepository;
import com.srms.api.modules.transport.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportService {

    private final VehicleRepository vehicleRepository;
    private final TransportRouteRepository routeRepository;
    private final TransportEnrolmentRepository enrolmentRepository;

    // ── Vehicles ──────────────────────────────────────────

    public List<Vehicle> getAllVehicles(String schoolId) {
        return vehicleRepository.findBySchoolId(schoolId);
    }

    public Vehicle createVehicle(String schoolId, Vehicle vehicle) {
        vehicle.setSchoolId(schoolId);
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(String schoolId, String id, Vehicle updated) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .filter(v -> v.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setPlateNumber(updated.getPlateNumber());
        vehicle.setMake(updated.getMake());
        vehicle.setModel(updated.getModel());
        vehicle.setCapacity(updated.getCapacity());
        vehicle.setDriverName(updated.getDriverName());
        vehicle.setDriverPhone(updated.getDriverPhone());
        vehicle.setRouteName(updated.getRouteName());
        vehicle.setStatus(updated.getStatus());
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(String schoolId, String id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .filter(v -> v.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicleRepository.delete(vehicle);
    }

    // ── Routes ────────────────────────────────────────────

    public List<TransportRoute> getAllRoutes(String schoolId) {
        return routeRepository.findBySchoolId(schoolId);
    }

    public TransportRoute createRoute(String schoolId, TransportRoute route) {
        route.setSchoolId(schoolId);
        return routeRepository.save(route);
    }

    // ── Enrolments ────────────────────────────────────────

    public List<TransportEnrolment> getAllEnrolments(String schoolId) {
        return enrolmentRepository.findBySchoolId(schoolId);
    }

    public TransportEnrolment createEnrolment(String schoolId, TransportEnrolment enrolment) {
        enrolment.setSchoolId(schoolId);
        enrolment.setStatus(TransportEnrolment.Status.ACTIVE);
        return enrolmentRepository.save(enrolment);
    }
}
