package com.icthh.xm.ms.scheduler.cucumber.stepdefs;

import com.icthh.xm.ms.scheduler.SchedulerApp;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;

import org.springframework.boot.test.context.SpringBootTest;

@WebAppConfiguration
@SpringBootTest
@ContextConfiguration(classes = SchedulerApp.class)
public abstract class StepDefs {

    protected ResultActions actions;

}
