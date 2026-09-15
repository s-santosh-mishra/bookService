package com.servicehub.serv.dto;

public class AdminDashboardDto {

    private long totalCustomers;
    private long totalWorkers;
    private long pendingWorkers;
    private long verifiedWorkers;
    private long totalCategories;
    private long totalServices;

    private String backendStatus;
    private String databaseStatus;
    private String authenticationStatus;

    public AdminDashboardDto(
            long totalCustomers,
            long totalWorkers,
            long pendingWorkers,
            long verifiedWorkers,
            long totalCategories,
            long totalServices,
            String backendStatus,
            String databaseStatus,
            String authenticationStatus
    ) {
        this.totalCustomers = totalCustomers;
        this.totalWorkers = totalWorkers;
        this.pendingWorkers = pendingWorkers;
        this.verifiedWorkers = verifiedWorkers;
        this.totalCategories = totalCategories;
        this.totalServices = totalServices;
        this.backendStatus = backendStatus;
        this.databaseStatus = databaseStatus;
        this.authenticationStatus = authenticationStatus;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public long getTotalWorkers() {
        return totalWorkers;
    }

    public long getPendingWorkers() {
        return pendingWorkers;
    }

    public long getVerifiedWorkers() {
        return verifiedWorkers;
    }

    public long getTotalCategories() {
        return totalCategories;
    }

    public long getTotalServices() {
        return totalServices;
    }

    public String getBackendStatus() {
        return backendStatus;
    }

    public String getDatabaseStatus() {
        return databaseStatus;
    }

    public String getAuthenticationStatus() {
        return authenticationStatus;
    }
}