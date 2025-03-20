package com.icthh.xm.ms.scheduler.service.dto;

import com.icthh.xm.ms.scheduler.domain.enumeration.ChannelType;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * A DTO for the Task entity.
 */
public class TaskDTO implements Serializable {

    private Long id;

    private String key;

    private String tenant;

    private String name;

    @NotNull
    private String typeKey;

    private String stateKey;

    @NotNull
    private String createdBy;

    private Instant startDate;

    private Instant endDate;

    private ScheduleType scheduleType;

    private Long delay;

    private String cronExpression;

    @Getter @Setter
    private String cronTriggerTimeZoneId;

    private ChannelType channelType;

    private String targetMs;

    private String description;

    private String data;

    private Integer ttl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getStateKey() {
        return stateKey;
    }

    public void setStateKey(String stateKey) {
        this.stateKey = stateKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public String getTargetMs() {
        return targetMs;
    }

    public void setTargetMs(String targetMs) {
        this.targetMs = targetMs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TaskDTO taskDTO = (TaskDTO) o;
        if (taskDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), taskDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "TaskDTO{"
            + "id=" + getId()
            + ", key='" + getKey() + "'"
            + ", name='" + getName() + "'"
            + ", typeKey='" + getTypeKey() + "'"
            + ", stateKey='" + getStateKey() + "'"
            + ", createdBy='" + getCreatedBy() + "'"
            + ", startDate='" + getStartDate() + "'"
            + ", endDate='" + getEndDate() + "'"
            + ", scheduleType='" + getScheduleType() + "'"
            + ", delay=" + getDelay()
            + ", cronExpression='" + getCronExpression() + "'"
            + ", cronTriggerTimeZoneId='" + getCronTriggerTimeZoneId() + "'"
            + ", channelType='" + getChannelType() + "'"
            + ", description='" + getDescription() + "'"
            + ", ttl='" + getTtl() + "'"
            + ", data.size='" + StringUtils.length(getData()) + "'"
            + "}";
    }
}
