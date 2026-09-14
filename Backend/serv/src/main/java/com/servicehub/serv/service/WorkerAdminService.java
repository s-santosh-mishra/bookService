package com.servicehub.serv.service;

import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.dto.AdminActionResponseDto;
import com.servicehub.serv.dto.WorkerAdminDetailDto;
import com.servicehub.serv.dto.WorkerAdminListDto;
import com.servicehub.serv.repository.WorkerServiceRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkerAdminService {

    private final WorkerRepository workerRepository;
    private final WorkerServiceRepository workerServiceRepository;

    public WorkerAdminService(
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository) {
        this.workerRepository = workerRepository;
        this.workerServiceRepository = workerServiceRepository;
    }

    // Approve Worker

    @Transactional
    public AdminActionResponseDto approveWorker(UUID workerId) {

        Worker worker = getWorkerEntity(workerId);

        if (worker.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException("Only pending workers can be approved.");
        }

        worker.setVerificationStatus(VerificationStatus.VERIFIED);
        workerRepository.save(worker);

        return new AdminActionResponseDto("Worker approved successfully.");
    }

    // Reject Worker

    @Transactional
    public AdminActionResponseDto rejectWorker(UUID workerId) {

        Worker worker = getWorkerEntity(workerId);

        if (worker.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException("Only pending workers can be rejected.");
        }

        worker.setVerificationStatus(VerificationStatus.REJECTED);
        workerRepository.save(worker);

        return new AdminActionResponseDto("Worker rejected successfully.");
    }

    // Suspend Worker

    @Transactional
    public AdminActionResponseDto suspendWorker(UUID workerId) {

        Worker worker = getWorkerEntity(workerId);

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Only verified workers can be suspended.");
        }

        worker.setVerificationStatus(VerificationStatus.SUSPENDED);
        worker.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        workerRepository.save(worker);

        return new AdminActionResponseDto("Worker suspended successfully.");
    }

    // Restore Suspended Worker

    @Transactional
    public AdminActionResponseDto restoreWorker(UUID workerId) {

        Worker worker = getWorkerEntity(workerId);

        if (worker.getVerificationStatus() != VerificationStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended workers can be restored.");
        }

        worker.setVerificationStatus(VerificationStatus.VERIFIED);
        worker.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        workerRepository.save(worker);

        return new AdminActionResponseDto("Worker restored successfully.");
    }

    // Get Worker

    public WorkerAdminDetailDto getWorker(UUID workerId) {

        Worker worker = getWorkerEntity(workerId);
        return toDetailDto(worker);
    }

    public List<WorkerAdminListDto> getWorkersByStatus(
            VerificationStatus verificationStatus) {

        return workerRepository
                .findByVerificationStatus(verificationStatus)
                .stream()
                .map(this::toListDto)
                .toList();
    }

    private Worker getWorkerEntity(UUID workerId) {

        return workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Worker not found."));
    }

    // Convert Worker to List DTO

    private WorkerAdminListDto toListDto(Worker worker) {

        return new WorkerAdminListDto(
                worker.getUserId(),
                worker.getUser().getFullName(),
                worker.getUser().getCredentials().getEmail(),
                worker.getUser().getPhone(),
                worker.getExperienceYears(),
                worker.getQualification(),
                worker.getBio(),
                worker.getVerificationStatus(),
                worker.getAvailabilityStatus(),
                worker.getUser().getCreatedAt());
    }

    // Convert Worker to Detail DTO

    private WorkerAdminDetailDto toDetailDto(Worker worker) {

        List<String> services = workerServiceRepository
                .findByWorker_UserId(worker.getUserId())
                .stream()
                .map(workerService -> workerService.getService().getServiceName())
                .collect(Collectors.toList());

        return new WorkerAdminDetailDto(
                worker.getUserId(),
                worker.getUser().getFullName(),
                worker.getUser().getCredentials().getEmail(),
                worker.getUser().getPhone(),
                worker.getUser().getAge(),
                worker.getUser().getGender(),
                worker.getUser().getAddressLine1(),
                worker.getUser().getAddressLine2(),
                worker.getUser().getLandmark(),
                worker.getUser().getCity(),
                worker.getUser().getState(),
                worker.getUser().getPinCode(),
                worker.getExperienceYears(),
                worker.getQualification(),
                worker.getBio(),
                worker.getVerificationStatus(),
                worker.getAvailabilityStatus(),
                services,
                worker.getUser().getCreatedAt(),
                worker.getUser().getUpdatedAt());
    }
}
