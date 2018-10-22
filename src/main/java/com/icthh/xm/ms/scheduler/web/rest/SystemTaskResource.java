package com.icthh.xm.ms.scheduler.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.ms.scheduler.service.SystemTaskService;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import io.github.jhipster.web.util.ResponseUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 *
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Slf4j
public class SystemTaskResource {

    private final SystemTaskService systemTaskService;

    /**
     * GET  /systasks : get all the tasks.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of tasks in body
     */
    @GetMapping("/systasks")
    @Timed
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        List<TaskDTO> list = systemTaskService.getSystemTasks();
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    /**
     * GET  /tasks/:key : get the "key" task.
     *
     * @param key the id of the taskDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the taskDTO, or with status 404 (Not Found)
     */
    @GetMapping("/systasks/{key}")
    @Timed
    public ResponseEntity<TaskDTO> getTask(@PathVariable String key) {
        TaskDTO taskDTO = systemTaskService.findOneSystemTask(key);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(taskDTO));
    }

}
