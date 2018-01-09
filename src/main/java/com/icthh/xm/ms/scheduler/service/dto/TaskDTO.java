package com.icthh.xm.ms.scheduler.service.dto;


import java.time.Instant;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import com.icthh.xm.ms.scheduler.domain.enumeration.Scheduletype;
import com.icthh.xm.ms.scheduler.domain.enumeration.Channeltype;

/**
 * A DTO for the Task entity.
 */
public class TaskDTO implements Serializable {

    private Long id;

    private String key;

    private String name;

    @NotNull
    private String typeKey;

    private String stateKey;

    @NotNull
    private String createdBy;

    private Instant startDate;

    private Instant endDate;

    private Scheduletype scheduletype;

    private Long delay;

    private String clonExpression;

    private Channeltype channelType;

    private String description;

    private String data;

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

    public Scheduletype getScheduletype() {
        return scheduletype;
    }

    public void setScheduletype(Scheduletype scheduletype) {
        this.scheduletype = scheduletype;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public String getClonExpression() {
        return clonExpression;
    }

    public void setClonExpression(String clonExpression) {
        this.clonExpression = clonExpression;
    }

    public Channeltype getChannelType() {
        return channelType;
    }

    public void setChannelType(Channeltype channelType) {
        this.channelType = channelType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TaskDTO taskDTO = (TaskDTO) o;
        if(taskDTO.getId() == null || getId() == null) {
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
        return "TaskDTO{" +
            "id=" + getId() +
            ", key='" + getKey() + "'" +
            ", name='" + getName() + "'" +
            ", typeKey='" + getTypeKey() + "'" +
            ", stateKey='" + getStateKey() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            ", scheduletype='" + getScheduletype() + "'" +
            ", delay=" + getDelay() +
            ", clonExpression='" + getClonExpression() + "'" +
            ", channelType='" + getChannelType() + "'" +
            ", description='" + getDescription() + "'" +
            ", data='" + getData() + "'" +
            "}";
    }
}
