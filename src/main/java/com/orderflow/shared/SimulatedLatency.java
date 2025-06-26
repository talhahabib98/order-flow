package com.orderflow.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatedLatency {

    private final long delayMs;

    public SimulatedLatency(@Value("${orderflow.simulation.delay-ms:0}") long delayMs) {
        this.delayMs = delayMs;
    }

    public void pause() {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
