package com.businessos.core.metrics;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsStreamController {

    private final Random random = new Random();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Streams simulated live traffic metrics every 500ms using Server-Sent Events (SSE).
     * Used by the frontend WebGL to control 3D particle simulation speed.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTrafficMetrics() {
        // Set timeout to 10 minutes (or -1L for infinite, but keeping it bound is safer)
        SseEmitter emitter = new SseEmitter(600000L);
        
        executor.execute(() -> {
            try {
                // Keep streaming while the emitter is open
                for (int i = 0; i < 1000; i++) {
                    // Simulated traffic volume (e.g. active connections/requests)
                    int simulatedTraffic = 1000 + random.nextInt(4000); // 1000 to 5000
                    
                    // Send event data
                    emitter.send(SseEmitter.event()
                            .data(String.valueOf(simulatedTraffic)));
                    
                    Thread.sleep(500); // Send every 500ms
                }
                emitter.complete();
            } catch (IOException | InterruptedException ex) {
                emitter.completeWithError(ex);
            }
        });
        
        return emitter;
    }
}
