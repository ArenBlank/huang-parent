package com.huang.common.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableConfigurationProperties(AiOpenAiProperties.class)
public class AiConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiConfiguration.class);
    private static final Pattern REG_VALUE_PATTERN = Pattern.compile("^\\s*(\\S+)\\s+REG_\\S+\\s+(.+?)\\s*$");

    @Bean
    public ChatModel chatLanguageModel(AiOpenAiProperties properties) {
        String resolvedApiKey = resolveApiKey(properties);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(resolvedApiKey)
                .modelName(properties.getModelName())
                .timeout(properties.getTimeout());

        if (StringUtils.hasText(properties.getBaseUrl())) {
            builder.baseUrl(properties.getBaseUrl());
        }

        if (StringUtils.hasText(resolvedApiKey)) {
            log.info("LLM api key resolved, length={}", resolvedApiKey.length());
        } else {
            log.warn("LLM api key is empty. AI generation will fail until key is configured.");
        }

        return builder.build();
    }

    private String resolveApiKey(AiOpenAiProperties properties) {
        String fromProperties = properties.getApiKey();
        if (isUsableApiKey(fromProperties)) {
            return fromProperties.trim();
        }

        String[] aliases = {"LLM_API_KEY", "DEEPSEEK_API_KEY"};
        for (String alias : aliases) {
            String envValue = System.getenv(alias);
            if (isUsableApiKey(envValue)) {
                log.info("LLM api key loaded from process env: {}", alias);
                return envValue.trim();
            }
        }

        for (String alias : aliases) {
            String propertyValue = System.getProperty(alias);
            if (isUsableApiKey(propertyValue)) {
                log.info("LLM api key loaded from JVM property: {}", alias);
                return propertyValue.trim();
            }
        }

        if (isWindows()) {
            for (String alias : aliases) {
                String userRegistryValue = queryWindowsRegistryEnv("HKCU\\Environment", alias);
                if (isUsableApiKey(userRegistryValue)) {
                    log.info("LLM api key loaded from Windows user registry env: {}", alias);
                    return userRegistryValue.trim();
                }
            }

            for (String alias : aliases) {
                String machineRegistryValue =
                        queryWindowsRegistryEnv("HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment", alias);
                if (isUsableApiKey(machineRegistryValue)) {
                    log.info("LLM api key loaded from Windows machine registry env: {}", alias);
                    return machineRegistryValue.trim();
                }
            }
        }

        return fromProperties;
    }

    private boolean isUsableApiKey(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return !normalized.contains("PLEASE_SET_YOUR_LLM_API_KEY")
                && !normalized.contains("YOUR-API-KEY-PLACEHOLDER")
                && !normalized.contains("YOUR_API_KEY_PLACEHOLDER")
                && !normalized.contains("CHANGE_ME");
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).contains("windows");
    }

    private String queryWindowsRegistryEnv(String registryPath, String variableName) {
        try {
            Process process = new ProcessBuilder("reg", "query", registryPath, "/v", variableName).start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                process.destroyForcibly();
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = REG_VALUE_PATTERN.matcher(line);
                    if (matcher.matches() && variableName.equalsIgnoreCase(matcher.group(1))) {
                        return matcher.group(2);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Read windows registry env failed, path={}, variable={}", registryPath, variableName, ex);
        }
        return null;
    }
}
