package org.cardanofoundation.hydrapoc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.hydra.HydraClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "hydra", name = "ws.url")
public class AppcationEventListener {
    private final HydraClient hydraClient;

    @EventListener
    public void initialize(ApplicationStartedEvent applicationStartedEvent) {
        try {
            hydraClient.connect();
        } catch (Exception e) {
            throw new IllegalStateException("Hydra connection error.", e);
        }
    }
}
