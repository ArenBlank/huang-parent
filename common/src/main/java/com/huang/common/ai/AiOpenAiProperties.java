package com.huang.common.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
public class AiOpenAiProperties {

    private String apiKey;

    private String baseUrl;

    private String modelName = "gpt-4o-mini";

    private Duration timeout = Duration.ofSeconds(60);
}
