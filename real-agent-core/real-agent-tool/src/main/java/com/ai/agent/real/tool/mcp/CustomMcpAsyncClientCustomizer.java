package com.ai.agent.real.tool.mcp;

import io.modelcontextprotocol.client.McpClient.*;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.extern.slf4j.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.mcp.customizer.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.*;

@Slf4j
@Component
public class CustomMcpAsyncClientCustomizer implements McpAsyncClientCustomizer {
    private final List<Root> roots;

    private final ChatModel chatModel;

    public CustomMcpAsyncClientCustomizer(ChatModel chatModel) {
        this.roots = List.of(new Root("file://E", "fileDir"));
        this.chatModel = chatModel;
    }


    @Override
    public void customize(String name, AsyncSpec spec) {
        // Customize the request timeout configuration
        spec.requestTimeout(Duration.ofSeconds(30));

        // Sets the root URIs that this client can access.
        spec.roots(roots);

        // Sets a custom sampling handler for processing message creation requests.
        spec.sampling((CreateMessageRequest messageRequest) -> {
            // Handle sampling default
            return Mono.just(CreateMessageResult.builder().build());
        });

        spec.elicitation((ElicitRequest elicitationRequest) -> {
            // Handle elicitation default
            return Mono.just(ElicitResult.builder().build());
        });

        // Adds a consumer to be notified when the available tools change, such as tools
        // being added or removed.
        spec.toolsChangeConsumer((List<McpSchema.Tool> tools) -> {
            // Handle tools change
            return Mono.empty();
        });

        // Adds a consumer to be notified when the available resources change, such as resources
        // being added or removed.
        spec.resourcesChangeConsumer((List<McpSchema.Resource> resources) -> {
            // Handle resources change
            return Mono.empty();
        });

        // Adds a consumer to be notified when the available prompts change, such as prompts
        // being added or removed.
        spec.promptsChangeConsumer((List<Prompt> prompts) -> {
            // Handle prompts change
            return Mono.empty();
        });

        // Adds a consumer to be notified when logging messages are received from the server.
        spec.loggingConsumer((McpSchema.LoggingMessageNotification loggingMessageNotification) -> {
            // Handle log messages
            return Mono.fromRunnable(() -> {
                switch (loggingMessageNotification.level()) {
                    case DEBUG -> log.debug(loggingMessageNotification.toString());
                    case INFO -> log.info(loggingMessageNotification.toString());
                    case NOTICE -> log.warn(loggingMessageNotification.toString());
                    case WARNING -> log.warn(loggingMessageNotification.toString());
                    case ERROR -> log.error(loggingMessageNotification.toString());
                    case CRITICAL -> log.error(loggingMessageNotification.toString());
                    case ALERT -> log.error(loggingMessageNotification.toString());
                    case EMERGENCY -> log.error(loggingMessageNotification.toString());
                    default -> log.info(loggingMessageNotification.toString());
                }
            });
        });



    }
}