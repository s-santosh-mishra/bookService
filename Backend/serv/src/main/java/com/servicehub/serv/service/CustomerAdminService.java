package com.servicehub.serv.service;

import com.servicehub.serv.dto.AdminActionResponseDto;
import com.servicehub.serv.dto.CustomerAdminDetailDto;
import com.servicehub.serv.dto.CustomerAdminListDto;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.repository.CustomerRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerAdminService {

    private final CustomerRepository customerRepository;

    public CustomerAdminService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // Get All Customers

    public List<CustomerAdminListDto> getAllCustomers() {

        return customerRepository
                .findAll()
                .stream()
                .map(this::toListDto)
                .toList();
    }

    // Get Customer

    public CustomerAdminDetailDto getCustomer(UUID customerId) {

        Customer customer = getCustomerEntity(customerId);

        return toDetailDto(customer);
    }

    // Deactivate Customer

    @Transactional
    public AdminActionResponseDto deactivateCustomer(UUID customerId) {

        Customer customer = getCustomerEntity(customerId);

        if (!customer.getUser().isActive()) {
            throw new IllegalStateException(
                    "Customer is already inactive.");
        }

        customer.getUser().setActive(false);

        return new AdminActionResponseDto(
                "Customer deactivated successfully.");
    }

    // Activate Customer

    @Transactional
    public AdminActionResponseDto activateCustomer(UUID customerId) {

        Customer customer = getCustomerEntity(customerId);

        if (customer.getUser().isActive()) {
            throw new IllegalStateException(
                    "Customer is already active.");
        }

        customer.getUser().setActive(true);

        return new AdminActionResponseDto(
                "Customer activated successfully.");
    }

    private Customer getCustomerEntity(UUID customerId) {

        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found."));
    }

    // Convert Customer to List DTO

    private CustomerAdminListDto toListDto(Customer customer) {

        return new CustomerAdminListDto(
                customer.getUserId(),
                customer.getUser().getFullName(),
                customer.getUser().getCredentials().getEmail(),
                customer.getUser().getPhone(),
                customer.getUser().getCity(),
                customer.getUser().isActive(),
                customer.getUser().getCreatedAt());
    }

    // Convert Customer to Detail DTO

    private CustomerAdminDetailDto toDetailDto(Customer customer) {

        return new CustomerAdminDetailDto(
                customer.getUserId(),
                customer.getUser().getFullName(),
                customer.getUser().getCredentials().getEmail(),
                customer.getUser().getPhone(),
                customer.getUser().getAge(),
                customer.getUser().getGender(),
                customer.getUser().getAddressLine1(),
                customer.getUser().getAddressLine2(),
                customer.getUser().getLandmark(),
                customer.getUser().getCity(),
                customer.getUser().getState(),
                customer.getUser().getPinCode(),
                customer.getUser().isActive(),
                customer.getUser().isTermsAccepted(),
                customer.getUser().getCreatedAt(),
                customer.getUser().getUpdatedAt());
    }
}