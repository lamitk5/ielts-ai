package com.ieltsaitutor.speaking;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpeakingAudioValidatorTest {

    private SpeakingAudioValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SpeakingAudioValidator();
    }

    @Test
    @DisplayName("Valid audio types (webm, ogg, mp4, wav, mpeg) pass validation")
    void validAudioTypesPass() {
        byte[] sampleBytes = new byte[] { 0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0x01, 0x02 }; // webm-like header
        assertDoesNotThrow(() -> validator.validate("audio/webm", "recording.webm", sampleBytes.length, sampleBytes));
        assertDoesNotThrow(() -> validator.validate("audio/ogg", "recording.ogg", 1024, new byte[1024]));
        assertDoesNotThrow(() -> validator.validate("audio/mp4", "recording.mp4", 2048, new byte[2048]));
        assertDoesNotThrow(() -> validator.validate("audio/wav", "recording.wav", 4096, new byte[4096]));
    }

    @Test
    @DisplayName("Unsafe MIME types (e.g. text/html, application/x-sh, application/javascript) are rejected")
    void unsafeMimeTypesRejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate("text/html", "exploit.html", 100, new byte[100]));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("application/javascript", "script.js", 100, new byte[100]));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("application/octet-stream", "binary.exe", 100, new byte[100]));
    }

    @Test
    @DisplayName("Files exceeding size limit (> 25MB) are rejected")
    void oversizedFilesRejected() {
        long limitPlusOne = 25 * 1024 * 1024 + 1;
        assertThrows(IllegalArgumentException.class, () -> validator.validate("audio/webm", "huge.webm", limitPlusOne, new byte[10]));
    }

    @Test
    @DisplayName("Empty or zero-byte audio is rejected")
    void emptyAudioRejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate("audio/webm", "empty.webm", 0, new byte[0]));
    }
}
