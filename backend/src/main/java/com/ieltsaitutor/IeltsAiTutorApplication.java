package com.ieltsaitutor;

import com.ieltsaitutor.ai.config.GeminiProperties;
import com.ieltsaitutor.rag.config.RagProperties;
import com.ieltsaitutor.rag.embedding.GeminiEmbeddingProperties;
import com.ieltsaitutor.rag.cli.RagCliProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GeminiProperties.class, GeminiEmbeddingProperties.class, RagProperties.class, RagCliProperties.class})
public class IeltsAiTutorApplication {
    public static void main(String[] args) {
        SpringApplication.run(IeltsAiTutorApplication.class, args);
    }
}
