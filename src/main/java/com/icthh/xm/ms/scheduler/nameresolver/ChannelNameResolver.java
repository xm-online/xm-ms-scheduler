package com.icthh.xm.ms.scheduler.nameresolver;

import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.springframework.stereotype.Component;

@Component
public interface ChannelNameResolver {

    String resolve(TaskDTO task);

}
