package com.icthh.xm.ms.scheduler.manager;

import com.icthh.xm.ms.scheduler.nameresolver.ChannelNameResolver;
import com.icthh.xm.ms.scheduler.service.dto.TaskDTO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class TestChannelNameResolver implements ChannelNameResolver {

    private Set<String> resolvedChannels = new HashSet<>();

    @Autowired
    ChannelNameResolver channelNameResolver;

    @Override
    public String resolve(final TaskDTO task) {
        String channelName = channelNameResolver.resolve(task);
        resolvedChannels.add(channelName);
        return channelName;
    }

    public Set<String> getResolvedChannels() {
        return resolvedChannels;
    }
}
