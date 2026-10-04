package com.ieltsaitutor.speaking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SpeakingTranscriptBoundaryTest {

    private SpeechToTextProvider sttProvider;
    private SpeakingTranscriptService transcriptService;

    @BeforeEach
    void setUp() {
        sttProvider = mock(SpeechToTextProvider.class);
        transcriptService = new SpeakingTranscriptService(sttProvider);
    }

    @Test
    @DisplayName("Empty audio and empty manual text returns UNAVAILABLE without calling STT")
    void emptyAudioAndEmptyManualReturnsUnavailable() {
        SpeechToTextResult result = transcriptService.resolveTranscript(null, null);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.source()).isEqualTo(TranscriptSource.UNAVAILABLE);
        assertThat(result.transcript()).isNull();
        verifyNoInteractions(sttProvider);
    }

    @Test
    @DisplayName("Blank manual text with blank audio returns UNAVAILABLE")
    void blankManualTextWithBlankAudioReturnsUnavailable() {
        SpeechToTextResult result = transcriptService.resolveTranscript("   ", "   ");

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.source()).isEqualTo(TranscriptSource.UNAVAILABLE);
        assertThat(result.transcript()).isNull();
        verifyNoInteractions(sttProvider);
    }

    @Test
    @DisplayName("Manual transcript takes precedence and is labeled MANUAL")
    void manualTranscriptLabeledManual() {
        SpeechToTextResult result = transcriptService.resolveTranscript("I love studying in quiet libraries.", "audio-key-123");

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.source()).isEqualTo(TranscriptSource.MANUAL);
        assertThat(result.transcript()).isEqualTo("I love studying in quiet libraries.");
        verifyNoInteractions(sttProvider);
    }

    @Test
    @DisplayName("Unavailable STT provider returns explicit unavailable status without inventing transcript")
    void unavailableSttReturnsExplicitUnavailable() {
        SpeakingTranscriptService defaultService = new SpeakingTranscriptService(new UnavailableSpeechToTextProvider());
        SpeechToTextResult result = defaultService.resolveTranscript(null, "some-audio-key.webm");

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.source()).isEqualTo(TranscriptSource.UNAVAILABLE);
        assertThat(result.transcript()).isNull();
    }

    @Test
    @DisplayName("Real STT provider returns SPEECH_TO_TEXT result with metadata")
    void realSttProviderReturnsRecognizedTranscript() {
        when(sttProvider.process("valid-key.webm")).thenReturn(
                SpeechToTextResult.recognized("This is transcribed speech.", "whisper-test", "large-v3", 0.95)
        );

        SpeechToTextResult result = transcriptService.resolveTranscript(null, "valid-key.webm");

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.source()).isEqualTo(TranscriptSource.SPEECH_TO_TEXT);
        assertThat(result.transcript()).isEqualTo("This is transcribed speech.");
        assertThat(result.providerName()).isEqualTo("whisper-test");
        assertThat(result.modelVersion()).isEqualTo("large-v3");
        assertThat(result.confidence()).isEqualTo(0.95);
    }
}
