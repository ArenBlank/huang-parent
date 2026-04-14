package com.huang.web.app.custom.config;

import com.huang.web.app.ai.PersonalTrainerAi;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiTrainerConfiguration {

    @Bean
    public PersonalTrainerAi personalTrainerAi(ChatModel chatLanguageModel) {
        return AiServices.builder(PersonalTrainerAi.class)
                .chatModel(chatLanguageModel)
                .build();
    }
}
