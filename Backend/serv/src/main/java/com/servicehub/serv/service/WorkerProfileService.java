package com.servicehub.serv.service;

import com.servicehub.serv.dto.UpdateWorkerProfileDto;
import com.servicehub.serv.dto.WorkerProfileDto;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.repository.UsersRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkerProfileService {

    private final UsersRepository usersRepository;
    private final WorkerRepository workerRepository;
    private final WorkerServiceRepository workerServiceRepository;

    public WorkerProfileService(
            UsersRepository usersRepository,
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository) {

        this.usersRepository = usersRepository;
        this.workerRepository = workerRepository;
        this.workerServiceRepository = workerServiceRepository;
    }

    // Get Worker Profile

    public WorkerProfileDto getProfile(UUID workerId) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() ->
                        new RuntimeException("Worker not found"));

        Users user = worker.getUser();

        return toDto(worker, user);
    }

    // Update Worker Profile

    public WorkerProfileDto updateProfile(
            UUID workerId,
            UpdateWorkerProfileDto dto) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() ->
                        new RuntimeException("Worker not found"));

        Users user = worker.getUser();

        // Personal information
        user.setFullName(dto.getFullName());
        user.setAge(dto.getAge());
        user.setGender(dto.getGender());
        user.setPhone(dto.getPhone());

        // Address
        user.setAddressLine1(dto.getAddressLine1());
        user.setAddressLine2(dto.getAddressLine2());
        user.setLandmark(dto.getLandmark());
        user.setCity(dto.getCity());
        user.setState(dto.getState());
        user.setPinCode(dto.getPinCode());

        // Professional information
        worker.setExperienceYears(dto.getExperienceYears());
        worker.setQualification(dto.getQualification());
        worker.setBio(dto.getBio());

        user = usersRepository.save(user);
        worker = workerRepository.save(worker);

        return toDto(worker, user);
    }

    // Entity → DTO

    private WorkerProfileDto toDto(
            Worker worker,
            Users user) {

        List<String> services = workerServiceRepository
                .findByWorker_UserId(worker.getUserId())
                .stream()
                .map(ws -> ws.getService().getServiceName())
                .toList();

        return new WorkerProfileDto(
                user.getFullName(),
                user.getCredentials().getEmail(),
                user.getAge(),
                user.getGender(),
                user.getPhone(),

                user.getAddressLine1(),
                user.getAddressLine2(),
                user.getLandmark(),
                user.getCity(),
                user.getState(),
                user.getPinCode(),

                worker.getExperienceYears(),
                worker.getQualification(),
                worker.getBio(),

                worker.getVerificationStatus(),
        worker.getAvailabilityStatus(),
        services
        );
    }
}