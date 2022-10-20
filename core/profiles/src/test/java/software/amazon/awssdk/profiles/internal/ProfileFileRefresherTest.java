/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.profiles.internal;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAmount;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.StringInputStream;

public class ProfileFileRefresherTest {

    private static FileSystem jimfs;
    private static Path testDirectory;

    @BeforeAll
    public static void setup() {
        jimfs = Jimfs.newFileSystem();
        testDirectory = jimfs.getPath("test");
    }

    @AfterAll
    public static void tearDown() {
        try {
            jimfs.close();
        } catch (IOException e) {
            // no-op
        }
    }

    @Test
    void refreshIfStale_profileModifiedNoRefreshIntervalRequestWithinJitterPeriod_doesNotReloadProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        try (ProfileFileRefresher refresher = refresherWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .build()) {
            Duration intervalWithinJitter = Duration.ofMillis(100);

            ProfileFile file1 = refresher.refreshIfStale();

            generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

            clock.tickForward(intervalWithinJitter);
            ProfileFile file2 = refresher.refreshIfStale();

            Assertions.assertThat(file2).isSameAs(file1);
        }
    }

    @Test
    void refreshIfStale_profileModifiedNoRefreshIntervalRequestOutsideJitterPeriod_doesNotReloadProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        try (ProfileFileRefresher refresher = refresherWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .build()) {
            Duration intervalOutsideJitter = Duration.ofMinutes(10);

            ProfileFile file1 = refresher.refreshIfStale();

            generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

            clock.tickForward(intervalOutsideJitter);
            ProfileFile file2 = refresher.refreshIfStale();

            Assertions.assertThat(file2).isSameAs(file1);
        }
    }

    @Test
    void refreshIfStale_profileModifiedBeforeRefreshIntervalExpires_doesNotReloadProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(10);
        try (ProfileFileRefresher refresher = refresherWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .refreshDuration(refreshInterval)
            .pollingDuration(refreshInterval)
            .build()) {
            ProfileFile file1 = refresher.refreshIfStale();

            generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

            clock.tickForward(refreshInterval.dividedBy(2));
            ProfileFile file2 = refresher.refreshIfStale();

            Assertions.assertThat(file2).isSameAs(file1);
        }
    }

    @Test
    void refreshIfStale_profileModifiedAfterRefreshIntervalExpires_reloadsProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(15);
        try (ProfileFileRefresher refresher = refresherWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .refreshDuration(refreshInterval)
            .pollingDuration(refreshInterval)
            .build()) {
            ProfileFile file1 = refresher.refreshIfStale();

            generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
            updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

            clock.tickForward(refreshInterval.plusSeconds(10));
            ProfileFile file2 = refresher.refreshIfStale();

            Assertions.assertThat(file2).isNotSameAs(file1);
        }
    }

    @Test
    void refreshIfStale_profileModifiedBeforeRefreshIntervalExpiresButRefreshedMultipleTimes_reloadsProfileFileOnce() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(15);
        try (ProfileFileRefresher refresher = refresherWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .refreshDuration(refreshInterval)
            .pollingDuration(Duration.ofSeconds(1))
            .build()) {
            ProfileFile file1 = refresher.refreshIfStale();

            generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
            updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

            clock.tickForward(refreshInterval.minusSeconds(1));
            ProfileFile file2 = refresher.refreshIfStale();

            clock.tickForward(Duration.ofSeconds(4));
            ProfileFile file3 = refresher.refreshIfStale();

            Assertions.assertThat(file2).isSameAs(file1);
            Assertions.assertThat(file3).isNotSameAs(file2);
        }
    }

    @Test
    void refreshIfStale_profileModifiedAfterRefreshIntervalExpiresButRefreshedMultipleTimes_reloadsProfileFileOnce() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(15);
        try (ProfileFileRefresher refresher = refresherWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .refreshDuration(refreshInterval)
            .pollingDuration(Duration.ofSeconds(1))
            .build()) {
            ProfileFile file1 = refresher.refreshIfStale();

            clock.tickForward(refreshInterval.plusSeconds(1));
            ProfileFile file2 = refresher.refreshIfStale();

            generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
            updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

            clock.tickForward(Duration.ofSeconds(4));
            ProfileFile file3 = refresher.refreshIfStale();

            Assertions.assertThat(file2).isSameAs(file1);
            Assertions.assertThat(file3).isNotSameAs(file2);
        }
    }

    @Test
    void refreshIfStale_profileModifiedMultipleTimes_reloadsProfileFileWhenCacheIsStale() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(15);
        try (ProfileFileRefresher refresher = refresherWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .refreshDuration(refreshInterval)
            .pollingDuration(Duration.ofSeconds(1))
            .build()) {
            ProfileFile file1 = refresher.refreshIfStale();

            clock.tickForward(refreshInterval.plusSeconds(1));
            ProfileFile file2 = refresher.refreshIfStale();

            generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
            updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

            clock.tickForward(Duration.ofSeconds(4));
            ProfileFile file3 = refresher.refreshIfStale();

            generateTestCredentialsFile("updatedAccessKey", "updatedSecretAccessKey");
            updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

            clock.tickForward(Duration.ofSeconds(4));
            ProfileFile file4 = refresher.refreshIfStale();

            clock.tickForward(refreshInterval.plusSeconds(1));
            ProfileFile file5 = refresher.refreshIfStale();

            Assertions.assertThat(file2).isSameAs(file1);
            Assertions.assertThat(file3).isNotSameAs(file2);
            Assertions.assertThat(file4).isSameAs(file3);
            Assertions.assertThat(file5).isNotSameAs(file4);
        }
    }

    @Test
    void refreshIfStale_profileDeleted_returnsProfileFileFromExceptionHandler() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");
        ProfileFile fallbackProfile = credentialFile("[test]\nx = y");

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(15);
        try (ProfileFileRefresher refresher = refresherWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .refreshDuration(refreshInterval)
            .pollingDuration(refreshInterval)
            .exceptionHandler(e -> fallbackProfile)
            .build()) {

            Files.deleteIfExists(credentialsFilePath);
            ProfileFile file1 = refresher.refreshIfStale();

            Assertions.assertThat(file1).isSameAs(fallbackProfile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ProfileFile credentialFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CREDENTIALS)
                          .build();
    }

    private Path generateTestFile(String contents, String filename) {
        try {
            Files.createDirectories(testDirectory);
            return Files.write(testDirectory.resolve(filename), contents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path generateTestCredentialsFile(String accessKeyId, String secretAccessKey) {
        String contents = String.format("[default]\naws_access_key_id = %s\naws_secret_access_key = %s\n",
                                        accessKeyId, secretAccessKey);
        return generateTestFile(contents, "credentials.txt");
    }

    private void updateModificationTime(Path path, Instant instant) {
        try {
            Files.setLastModifiedTime(path, FileTime.from(instant));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ProfileFile profileFile(Path path) {
        return ProfileFile.builder().content(path).type(ProfileFile.Type.CREDENTIALS).build();
    }

    private ProfileFileRefresher.Builder refresherWithClock(Clock clock) {
        return ProfileFileRefresher.builder()
                                   .clock(clock);
    }

    private static final class AdjustableClock extends Clock {
        private Instant time;

        private AdjustableClock() {
            this.time = Instant.now();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return time;
        }

        public void tickForward(TemporalAmount amount) {
            time = time.plus(amount);
        }

        public void tickBackward(TemporalAmount amount) {
            time = time.minus(amount);
        }
    }
}
