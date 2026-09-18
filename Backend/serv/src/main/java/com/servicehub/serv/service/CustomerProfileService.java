package com.servicehub.serv.service;

import com.servicehub.serv.dto.CustomerProfileDto;
import com.servicehub.serv.dto.UpdateCustomerProfileDto;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomerProfileService {

    private final UsersRepository usersRepository;

    public CustomerProfileService(
            UsersRepository usersRepository) {

        this.usersRepository = usersRepository;
    }

    // Get Customer Profile

    public CustomerProfileDto getProfile(UUID userId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return toDto(user);
    }

    // Update Customer Profile

    public CustomerProfileDto updateProfile(
            UUID userId,
            UpdateCustomerProfileDto dto) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setFullName(dto.getFullName());
        user.setAge(dto.getAge());
        user.setGender(dto.getGender());
        user.setPhone(dto.getPhone());
        user.setAddressLine1(dto.getAddressLine1());
        user.setAddressLine2(dto.getAddressLine2());
        user.setLandmark(dto.getLandmark());
        user.setCity(dto.getCity());
        user.setState(dto.getState());
        user.setPinCode(dto.getPinCode());

        Users updatedUser =
                usersRepository.save(user);

        return toDto(updatedUser);
    }

    // Entity → DTO

    private CustomerProfileDto toDto(Users user) {

        return new CustomerProfileDto(
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
                user.getPinCode()
        );
    }
}