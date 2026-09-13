package com.servicehub.serv.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "worker_services",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"worker_id", "service_id"})
    }
)
public class WorkerService {

    @EmbeddedId
    private WorkerServiceId id;

    @ManyToOne
    @MapsId("workerId")
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne
    @MapsId("serviceId")
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    public WorkerService() {
    }

    public WorkerService(Worker worker, Service service) {
        this.worker = worker;
        this.service = service;
    }

    public WorkerServiceId getId() {
        return id;
    }

    public void setId(WorkerServiceId id) {
        this.id = id;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }
}