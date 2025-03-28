package com.icthh.xm.ms.scheduler.web.rest;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import com.icthh.xm.commons.logging.web.rest.LogsResource;
import com.icthh.xm.ms.scheduler.AbstractSpringBootTest;
import com.icthh.xm.ms.scheduler.web.rest.vm.LoggerVM;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the LogsResource REST controller.
 *
 * @see LogsResource
 */
public class LogsResourceIntTest extends AbstractSpringBootTest {

    private MockMvc restLogsMockMvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        LogsResource logsResource = new LogsResource();
        this.restLogsMockMvc = MockMvcBuilders
            .standaloneSetup(logsResource)
            .build();
    }

    @Test
    public void getAllLogs() throws Exception {
        restLogsMockMvc.perform(get("/management/logs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    public void changeLogs() throws Exception {
        LoggerVM logger = new LoggerVM();
        logger.setLevel("INFO");
        logger.setName("ROOT");

        restLogsMockMvc.perform(put("/management/logs")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(logger)))
            .andExpect(status().isNoContent());
    }

    @Ignore("Test fails when run from Idea")
    @Test
    public void testLogstashAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertThat(context.getLogger("ROOT").getAppender("ASYNC_LOGSTASH")).isInstanceOf(AsyncAppender.class);
    }
}
