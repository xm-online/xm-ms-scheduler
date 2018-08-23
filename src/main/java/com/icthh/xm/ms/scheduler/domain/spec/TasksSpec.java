package com.icthh.xm.ms.scheduler.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "tasks" })
@Data
public class TasksSpec {

    @JsonProperty("tasks")
    private List<TaskDTO> tasks = null;

}
