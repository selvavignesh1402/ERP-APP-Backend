package com.riceerp.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirebaseConfigTest {
    @TempDir Path directory;
    GoogleCredentials fakeCredentials() {
        return GoogleCredentials.create(new AccessToken("test-only-no-network", new Date(System.currentTimeMillis() + 60000)));
    }
    @AfterEach void cleanup() { FirebaseApp.getApps().forEach(FirebaseApp::delete); }
    @Test void disabledDoesNotReadCredentialsOrInitialize() throws Exception {
        var config = spy(new FirebaseConfig(false, "missing.json", "test-project"));
        config.initialize(); verify(config, never()).credentials(); assertTrue(FirebaseApp.getApps().isEmpty());
    }
    @Test void enabledInitializesDefaultAppWithConfiguredProject() throws Exception {
        var config = spy(new FirebaseConfig(true, "", "test-project"));
        doReturn(fakeCredentials()).when(config).credentials(); config.initialize();
        assertEquals("test-project", FirebaseApp.getInstance().getOptions().getProjectId());
        config.initialize(); verify(config, times(1)).credentials();
    }
    @Test void namedAppDoesNotPreventDefaultAppInitialization() throws Exception {
        FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(fakeCredentials()).build(), "other-app");
        var config = spy(new FirebaseConfig(true, "", "test-project"));
        doReturn(fakeCredentials()).when(config).credentials(); config.initialize();
        assertNotNull(FirebaseApp.getInstance()); assertEquals(2, FirebaseApp.getApps().size());
    }
    @Test void existingDefaultAppIsPreserved() throws Exception {
        var original = FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(fakeCredentials()).build());
        var config = spy(new FirebaseConfig(true, "missing.json", "")); config.initialize();
        verify(config, never()).credentials(); assertSame(original, FirebaseApp.getInstance());
    }
    @Test void configuredMissingFileStopsEnabledStartup() {
        var config = new FirebaseConfig(true, directory.resolve("missing.json").toString(), "");
        var error = assertThrows(IllegalStateException.class, config::initialize);
        assertTrue(error.getMessage().contains("FIREBASE_CREDENTIALS_PATH")); assertTrue(FirebaseApp.getApps().isEmpty());
    }
    @Test void malformedCredentialFileStopsStartupAndClosesStream() throws Exception {
        var file = directory.resolve("credentials.json"); Files.writeString(file, "{}");
        assertThrows(IllegalStateException.class, () -> new FirebaseConfig(true, file.toString(), "").initialize());
        Files.delete(file); assertFalse(Files.exists(file));
    }
    @Test void blankPathUsesApplicationDefaultCredentials() throws Exception {
        var expected = fakeCredentials();
        try (var google = mockStatic(GoogleCredentials.class)) {
            google.when(GoogleCredentials::getApplicationDefault).thenReturn(expected);
            assertSame(expected, new FirebaseConfig(true, "", "").credentials());
            google.verify(GoogleCredentials::getApplicationDefault);
        }
    }
    @Test void explicitFileDoesNotSilentlyFallBackToOtherCredentials() throws Exception {
        try (var google = mockStatic(GoogleCredentials.class)) {
            assertThrows(IOException.class, () -> new FirebaseConfig(true, directory.resolve("missing").toString(), "").credentials());
            google.verifyNoInteractions();
        }
    }
}
