package com.servicehub.serv.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

// This is not an entity.
// It is simply the object that represents the composite primary key of WorkerService

@Embeddable
public class WorkerServiceId implements Serializable {

    // it uses workerid+service id as a composite key 
    //to uniquely identify one worker - service relationship
    private UUID workerId;
    private UUID serviceId;

    public WorkerServiceId() {
    }

    public WorkerServiceId(UUID workerId, UUID serviceId) {
        this.workerId = workerId;
        this.serviceId = serviceId;
    }

    public UUID getWorkerId() {
        return workerId;
    }

    public void setWorkerId(UUID workerId) {
        this.workerId = workerId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkerServiceId)) return false;

        WorkerServiceId that = (WorkerServiceId) o;

        return workerId.equals(that.workerId)
                && serviceId.equals(that.serviceId);
    }

    @Override
    public int hashCode() {
        return 31 * workerId.hashCode() + serviceId.hashCode();
    }
}