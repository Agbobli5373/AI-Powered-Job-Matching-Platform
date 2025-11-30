package com.isaac.job_matching.shared.config;

import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.MistralAiEmbeddingModel;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.MistralAiEmbeddingOptions;
import org.springframework.ai.document.MetadataMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MistralAiConfig {

    @Value("${spring.ai.mistralai.api-key}")
    private String apiKey;

    @Bean
    public MistralAiApi mistralAiApi() {
        return new MistralAiApi(apiKey);
    }

    @Bean
    public MistralAiChatModel mistralAiChatModel(MistralAiApi mistralAiApi) {
        return MistralAiChatModel.builder()
                .mistralAiApi(mistralAiApi)
                .defaultOptions(MistralAiChatOptions.builder().model("mistral-large-latest").build())
                .build();
    }

    @Bean
    public MistralAiEmbeddingModel mistralAiEmbeddingModel(MistralAiApi mistralAiApi) {
        return new MistralAiEmbeddingModel(mistralAiApi,
                MistralAiEmbeddingOptions.builder()
                        .withModel("mistral-embed")
                        .build());
    }
}
