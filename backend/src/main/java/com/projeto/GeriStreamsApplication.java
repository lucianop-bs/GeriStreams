package com.projeto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class GeriStreamsApplication {

    private static final Logger log = LoggerFactory.getLogger(GeriStreamsApplication.class);

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("GeriStreams iniciado com sucesso! API disponivel em http://localhost:8080");
    }

    public static void main(String[] args) {
        SpringApplication.run(GeriStreamsApplication.class, args);
    }
}
