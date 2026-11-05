package com.hotel.channelmanager.channels.connector;

import com.hotel.channelmanager.channels.domain.ChannelType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ChannelConnectorRegistry {

    private final Map<ChannelType, ChannelConnector> connectors = new EnumMap<>(ChannelType.class);

    public ChannelConnectorRegistry(List<ChannelConnector> connectorList) {
        for (ChannelConnector connector : connectorList) {
            connectors.put(connector.getChannelType(), connector);
        }
    }

    public Optional<ChannelConnector> getConnector(ChannelType channelType) {
        return Optional.ofNullable(connectors.get(channelType));
    }

    public ChannelConnector getRequiredConnector(ChannelType channelType) {
        ChannelConnector connector = connectors.get(channelType);
        if (connector == null) {
            throw new IllegalArgumentException("No connector implementation registered for channel type: " + channelType);
        }
        return connector;
    }
}

