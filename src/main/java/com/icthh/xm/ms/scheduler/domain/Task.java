package com.icthh.xm.ms.scheduler.domain;

import com.icthh.xm.ms.scheduler.domain.enumeration.ChannelType;
import com.icthh.xm.ms.scheduler.domain.enumeration.ScheduleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A Task.
 */
@Entity
@Table(name = "task")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "jhi_key")
    private String key;

    @Column(name = "name")
    private String name;

    @NotNull
    @Column(name = "type_key", nullable = false)
    private String typeKey;

    @Column(name = "state_key")
    private String stateKey;

    @NotNull
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type")
    private ScheduleType scheduleType;

    @Column(name = "delay")
    private Long delay;

    @Column(name = "ttl")
    private Integer ttl;

    @Column(name = "target_ms")
    private String targetMs;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type")
    private ChannelType channelType;

    @Column(name = "description")
    private String description;

    @Column(name = "data")
    private String data;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public Task key(String key) {
        this.key = key;
        return this;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public Task name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public Task typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getStateKey() {
        return stateKey;
    }

    public Task stateKey(String stateKey) {
        this.stateKey = stateKey;
        return this;
    }

    public void setStateKey(String stateKey) {
        this.stateKey = stateKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Task createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Task startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public Task endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public Task scheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
        return this;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public Long getDelay() {
        return delay;
    }

    public Task delay(Long delay) {
        this.delay = delay;
        return this;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public String getTargetMs() {
        return targetMs;
    }

    public void setTargetMs(String targetMs) {
        this.targetMs = targetMs;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public Task cronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
        return this;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public Task channelType(ChannelType channelType) {
        this.channelType = channelType;
        return this;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public String getDescription() {
        return description;
    }

    public Task description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getData() {
        return data;
    }

    public Task data(String data) {
        this.data = data;
        return this;
    }

    public void setData(String data) {
        this.data = data;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

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
        Task task = (Task) o;
        if (task.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), task.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Task{"
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
            + ", targetMs=" + getTargetMs()
            + ", cronExpression='" + getCronExpression() + "'"
            + ", channelType='" + getChannelType() + "'"
            + ", description='" + getDescription() + "'"
            + ", data='" + getData() + "'"
            + ", ttl='" + getTtl() + "'"
            + "}";
    }
}
