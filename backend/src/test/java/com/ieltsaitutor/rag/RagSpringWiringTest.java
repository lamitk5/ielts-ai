package com.ieltsaitutor.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.rag.chat.DefaultGroundingValidator;
import com.ieltsaitutor.rag.chat.DefaultRagChatService;
import com.ieltsaitutor.rag.chat.RagContextBuilder;
import com.ieltsaitutor.rag.cli.RagCliProperties;
import com.ieltsaitutor.rag.cli.RagCliRunner;
import com.ieltsaitutor.rag.ingestion.DocumentIngestionService;
import com.ieltsaitutor.rag.retrieval.VectorRetrievalService;

class RagSpringWiringTest {
    @Test
    void ragChatServiceCanBeCreatedBySpring() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AiProvider.class, () -> mock(AiProvider.class));
            context.registerBean(VectorRetrievalService.class, () -> mock(VectorRetrievalService.class));
            context.registerBean(RagContextBuilder.class, () -> mock(RagContextBuilder.class));
            context.registerBean(DefaultGroundingValidator.class);
            context.registerBean(DefaultRagChatService.class);
            context.registerBean(RagCliProperties.class);
            context.registerBean(DocumentIngestionService.class, () -> mock(DocumentIngestionService.class));
            context.registerBean(RagCliRunner.class);
            context.refresh();

            assertThat(context.getBean(DefaultRagChatService.class)).isNotNull();
            assertThat(context.getBean(RagCliRunner.class)).isNotNull();
        }
    }

    @Test
    void springBootFlywayAutoConfigurationIsAvailable() throws ClassNotFoundException {
        assertThat(Class.forName("org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")).isNotNull();
    }
}
