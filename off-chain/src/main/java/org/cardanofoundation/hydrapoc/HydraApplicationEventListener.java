package org.cardanofoundation.hydrapoc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.hydra.HydraClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "hydra", name = "ws.url")
public class HydraApplicationEventListener {

    private final HydraClient hydraClient;

    @EventListener
    public void initialize(ApplicationStartedEvent applicationStartedEvent) {
        try {
            hydraClient.connect().block(Duration.ofMinutes(10));
        } catch (Exception e) {
            throw new IllegalStateException("Hydra connection error.", e);
        }
    }

    @EventListener
    public void dispose(ContextStoppedEvent contextStoppedEvent) {
        log.info("Context stopped, closing hydra client...");
        hydraClient.disconnect();
    }

}
