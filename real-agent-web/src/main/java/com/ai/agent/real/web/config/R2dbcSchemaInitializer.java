package com.ai.agent.real.web.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class R2dbcSchemaInitializer {

    @Bean
    public ApplicationRunner schemaRunner(DatabaseClient databaseClient) {
        return args -> {
            try {
                ClassPathResource resource = new ClassPathResource("sql/voice_persistence_schema.sql");
                if (!resource.exists()) {
                    log.info("[schema] no schema resource found, skip init");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                }
                String sql = sb.toString();
                String[] statements = sql.split(";\\s*\n");
                Flux.fromArray(statements)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .concatMap(stmt -> databaseClient.sql(stmt).then())
                        .onErrorResume(e -> {
                            log.warn("[schema] init error: {}", e.toString());
                            return Mono.empty();
                        })
                        .then()
                        .subscribe(v -> {}, err -> log.warn("[schema] init failed: {}", err.toString()), () -> log.info("[schema] init completed"));
            } catch (Exception e) {
                log.warn("[schema] init caught exception: {}", e.toString());
            }
        };
    }
}
