package com.huang.common.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(AiOpenAiProperties.class)
public class AiConfiguration {

    @Bean
    public ChatModel chatLanguageModel(AiOpenAiProperties properties) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .timeout(properties.getTimeout());

        if (StringUtils.hasText(properties.getBaseUrl())) {
            builder.baseUrl(properties.getBaseUrl());
        }

        return builder.build();
    }
}
