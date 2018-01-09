package com.icthh.xm.ms.scheduler.service.dto;

import java.io.Serializable;
import com.icthh.xm.ms.scheduler.domain.enumeration.Scheduletype;
import com.icthh.xm.ms.scheduler.domain.enumeration.Channeltype;
import io.github.jhipster.service.filter.BooleanFilter;
import io.github.jhipster.service.filter.DoubleFilter;
import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.FloatFilter;
import io.github.jhipster.service.filter.IntegerFilter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;

import io.github.jhipster.service.filter.InstantFilter;




/**
 * Criteria class for the Task entity. This class is used in TaskResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * <code> /tasks?id.greaterThan=5&amp;attr1.contains=something&amp;attr2.specified=false</code>
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
public class TaskCriteria implements Serializable {
    /**
     * Class for filtering Scheduletype
     */
    public static class ScheduletypeFilter extends Filter<Scheduletype> {
    }

    /**
     * Class for filtering Channeltype
     */
    public static class ChanneltypeFilter extends Filter<Channeltype> {
    }

    private static final long serialVersionUID = 1L;


    private LongFilter id;

    private StringFilter key;

    private StringFilter name;

    private StringFilter typeKey;

    private StringFilter stateKey;

    private StringFilter createdBy;

    private InstantFilter startDate;

    private InstantFilter endDate;

    private ScheduletypeFilter scheduletype;

    private LongFilter delay;

    private StringFilter clonExpression;

    private ChanneltypeFilter channelType;

    private StringFilter description;

    private StringFilter data;

    public TaskCriteria() {
    }

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getKey() {
        return key;
    }

    public void setKey(StringFilter key) {
        this.key = key;
    }

    public StringFilter getName() {
        return name;
    }

    public void setName(StringFilter name) {
        this.name = name;
    }

    public StringFilter getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(StringFilter typeKey) {
        this.typeKey = typeKey;
    }

    public StringFilter getStateKey() {
        return stateKey;
    }

    public void setStateKey(StringFilter stateKey) {
        this.stateKey = stateKey;
    }

    public StringFilter getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(StringFilter createdBy) {
        this.createdBy = createdBy;
    }

    public InstantFilter getStartDate() {
        return startDate;
    }

    public void setStartDate(InstantFilter startDate) {
        this.startDate = startDate;
    }

    public InstantFilter getEndDate() {
        return endDate;
    }

    public void setEndDate(InstantFilter endDate) {
        this.endDate = endDate;
    }

    public ScheduletypeFilter getScheduletype() {
        return scheduletype;
    }

    public void setScheduletype(ScheduletypeFilter scheduletype) {
        this.scheduletype = scheduletype;
    }

    public LongFilter getDelay() {
        return delay;
    }

    public void setDelay(LongFilter delay) {
        this.delay = delay;
    }

    public StringFilter getClonExpression() {
        return clonExpression;
    }

    public void setClonExpression(StringFilter clonExpression) {
        this.clonExpression = clonExpression;
    }

    public ChanneltypeFilter getChannelType() {
        return channelType;
    }

    public void setChannelType(ChanneltypeFilter channelType) {
        this.channelType = channelType;
    }

    public StringFilter getDescription() {
        return description;
    }

    public void setDescription(StringFilter description) {
        this.description = description;
    }

    public StringFilter getData() {
        return data;
    }

    public void setData(StringFilter data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "TaskCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (key != null ? "key=" + key + ", " : "") +
                (name != null ? "name=" + name + ", " : "") +
                (typeKey != null ? "typeKey=" + typeKey + ", " : "") +
                (stateKey != null ? "stateKey=" + stateKey + ", " : "") +
                (createdBy != null ? "createdBy=" + createdBy + ", " : "") +
                (startDate != null ? "startDate=" + startDate + ", " : "") +
                (endDate != null ? "endDate=" + endDate + ", " : "") +
                (scheduletype != null ? "scheduletype=" + scheduletype + ", " : "") +
                (delay != null ? "delay=" + delay + ", " : "") +
                (clonExpression != null ? "clonExpression=" + clonExpression + ", " : "") +
                (channelType != null ? "channelType=" + channelType + ", " : "") +
                (description != null ? "description=" + description + ", " : "") +
                (data != null ? "data=" + data + ", " : "") +
            "}";
    }

}
