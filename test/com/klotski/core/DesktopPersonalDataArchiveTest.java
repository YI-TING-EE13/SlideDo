package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopPersonalDataArchiveTest {
    @TempDir
    File tempDir;

    @AfterEach
    void clearFailureHook() {
        DesktopPersonalDataArchive.setFailureInjectorForTests(null);
        System.clearProperty(SaveManager.DATA_DIR_PROPERTY);
    }

    @Test
    void emptyAndPopulatedArchivesUseDeterministicEntriesAndPreserveUnmanagedFiles() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        Files.writeString(new File(source, "notes.txt").toPath(), "keep me", StandardCharsets.UTF_8);
        Files.writeString(new File(target, "notes.txt").toPath(), "do not replace", StandardCharsets.UTF_8);
        Files.writeString(new File(target, "klotski_save_3.json").toPath(), "stale", StandardCharsets.UTF_8);

        String empty = DesktopPersonalDataArchive.exportArchive(source, 123L);
        assertEquals(empty, DesktopPersonalDataArchive.exportArchive(source, 123L));
        DesktopPersonalDataArchive.validate(empty);
        DesktopPersonalDataArchive.restoreArchive(empty, target);

        assertEquals("do not replace", Files.readString(new File(target, "notes.txt").toPath()));
        assertFalse(new File(target, "klotski_save_3.json").exists());
        assertTrue(new File(target, "klotski_saved_games_reset.marker").isFile());
        assertTrue(new File(target, "klotski_records_reset.marker").isFile());
    }

    @Test
    void populatedMultiNamespaceRoundTripIncludesLegacyAndAssistedState() throws Exception {
        String old = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, source.getAbsolutePath());
        try {
            GameModel normal = new GameModel(3);
            normal.scramble(PuzzleDifficulty.RELAXED, 12L);
            assertTrue(SaveManager.saveGame(normal, true));
            LocalDate date = LocalDate.now().minusDays(2);
            assertTrue(SaveManager.saveDailyGame(date.toString(),
                    DailyChallenge.forDate(date).createGame(), true));
            SaveManager.FavoritePuzzle favorite = SaveManager.saveFavorite(normal, "Archive", 12L);
            assertTrue(SaveManager.saveFavoriteRun(favorite.id, favorite.createGame(), true));
            ContinuousChallenge challenge = ContinuousChallenge.start(3).completePuzzle(2, 40L, true);
            assertTrue(SaveManager.saveContinuousGame(normal, challenge, true));
            assertTrue(SaveManager.setDesktopTheme("ocean"));
            assertTrue(SaveManager.setDesktopLanguageTag("zh-TW"));
            assertTrue(SaveManager.setTrendSize(5));
            assertTrue(SaveManager.setTrendDifficulty(PuzzleDifficulty.CHALLENGE));
            assertTrue(SaveManager.setWeeklyGoalTarget(7));
            assertTrue(SaveManager.setReducedMotionEnabled(true));
            assertTrue(SaveManager.setSoundEnabled(true));
            assertTrue(SaveManager.markOnboardingSeen());
            assertTrue(SaveManager.recordCompletion("archive-player", 3,
                    PuzzleDifficulty.RELAXED, 3, 500L, false));
            assertTrue(SaveManager.recordCompletion("archive-assisted", 3,
                    PuzzleDifficulty.RELAXED, 4, 700L, true));
            Files.writeString(new File(source, "klotski_save.json").toPath(),
                    "{\"version\":1,\"size\":3,\"grid\":[[1,2,3],[4,5,6],[7,0,8]]}");
            String archive = DesktopPersonalDataArchive.exportArchive(source, 321L);
            DesktopPersonalDataArchive.validate(archive);

            System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
            DesktopPersonalDataArchive.restoreArchive(archive, target);
            assertEquals("ocean", SaveManager.getDesktopTheme());
            assertEquals("zh-TW", SaveManager.getDesktopLanguageTag());
            assertEquals(5, SaveManager.getTrendSize());
            assertEquals(PuzzleDifficulty.CHALLENGE, SaveManager.getTrendDifficulty());
            assertEquals(7, SaveManager.getWeeklyGoalTarget());
            assertTrue(SaveManager.isReducedMotionEnabled());
            assertTrue(SaveManager.isSoundEnabled());
            assertTrue(SaveManager.isOnboardingSeen());
            assertTrue(SaveManager.loadGame(3).assisted);
            assertTrue(SaveManager.isDailyGameAssisted(date.toString()));
            assertTrue(SaveManager.isFavoriteRunAssisted(favorite.id));
            assertTrue(SaveManager.loadContinuousGame().assisted);
            assertEquals(2, SaveManager.getCompletionHistory().length);
            assertTrue(new File(target, "klotski_save.json").isFile());
        } finally {
            if (old == null) {
                System.clearProperty(SaveManager.DATA_DIR_PROPERTY);
            } else {
                System.setProperty(SaveManager.DATA_DIR_PROPERTY, old);
            }
        }
    }

    @Test
    void absentNormalAndRecordNamespacesMaskStaleLegacyFallback() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel stale = new GameModel(3);
        stale.scramble(PuzzleDifficulty.CLASSIC, 101L);
        assertTrue(SaveManager.saveGame(stale, new File(target, "klotski_save.json")));
        Files.writeString(new File(target, "klotski_records.json").toPath(),
                "{\"3\":{\"moves\":1,\"timeMs\":2}}", StandardCharsets.UTF_8);
        assertTrue(new File(target, "klotski_save.json").isFile());
        assertTrue(new File(target, "klotski_records.json").isFile());

        String archive = DesktopPersonalDataArchive.exportArchive(source, 11L);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
        DesktopPersonalDataArchive.restoreArchive(archive, target);

        assertNull(SaveManager.loadGame());
        assertNull(SaveManager.getBestRecord(3));
        assertTrue(new File(target, "klotski_saved_games_reset.marker").isFile());
        assertTrue(new File(target, "klotski_records_reset.marker").isFile());
        assertFalse(new File(target, "klotski_save.json").exists());
        assertFalse(new File(target, "klotski_records.json").exists());
    }

    @Test
    void partialNormalRestoreMasksFallbackForMissingSizes() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel imported = new GameModel(3);
        imported.scramble(PuzzleDifficulty.CLASSIC, 111L);
        assertTrue(SaveManager.saveGame(imported, new File(source, "klotski_save_3.json")));
        String archive = DesktopPersonalDataArchive.exportArchive(source, 31L);
        DesktopPersonalDataArchive.restoreArchive(archive, target);

        System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
        assertNotNull(SaveManager.loadGame(3));
        assertNull(SaveManager.loadGame(4));
        assertTrue(new File(target, "klotski_saved_games_reset.marker").isFile());
    }

    @Test
    void durableLegacySuppressionSurvivesLaterNormalSaveAndCanonicalOverwriteWins() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel imported = new GameModel(3);
        imported.scramble(PuzzleDifficulty.CLASSIC, 112L);
        assertTrue(SaveManager.saveGame(imported, new File(source, "klotski_save_3.json")));
        String archive = DesktopPersonalDataArchive.exportArchive(source, 32L);
        DesktopPersonalDataArchive.restoreArchive(archive, target);

        System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
        GameModel later = new GameModel(3);
        later.scramble(PuzzleDifficulty.CLASSIC, 113L);
        assertTrue(SaveManager.saveGame(later, false));
        assertTrue(new File(target, "klotski_saved_games_reset.marker").isFile());
        assertNotNull(SaveManager.loadGame(3));
        assertNull(SaveManager.loadGame(4));
    }

    @Test
    void legacyOnlyProfileSurvivesWithoutDestructiveMigration() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        SaveManager.SaveData legacy = new SaveManager.SaveData();
        legacy.size = 3;
        legacy.grid = new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 0, 8}};
        legacy.initialGrid = new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 0, 8}};
        legacy.active = true;
        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(new File(source, "klotski_save.dat")))) {
            output.writeObject(legacy);
        }
        String archive = DesktopPersonalDataArchive.exportArchive(source, 7L);
        DesktopPersonalDataArchive.validate(archive);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
        DesktopPersonalDataArchive.restoreArchive(archive, target);
        SaveManager.SaveData restored = SaveManager.loadGame(3);
        assertTrue(restored != null);
        assertEquals(3, restored.size);
        assertArrayEquals(legacy.grid, restored.grid);
        assertTrue(new File(target, "klotski_save.dat").isFile());
        assertFalse(new File(target, "klotski_saved_games_reset.marker").exists());
    }

    @Test
    void projectRootLegacyFallbackIsIncludedWithoutRewritingTheSource() throws Exception {
        File source = new File(tempDir, "source");
        File fallback = new File(tempDir, "fallback");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(fallback.mkdirs());
        assertTrue(target.mkdirs());
        SaveManager.SaveData legacy = new SaveManager.SaveData();
        legacy.size = 3;
        legacy.grid = new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 0, 8}};
        legacy.initialGrid = new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 0, 8}};
        File fallbackFile = new File(fallback, "klotski_save.dat");
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(fallbackFile))) {
            output.writeObject(legacy);
        }
        byte[] before = Files.readAllBytes(fallbackFile.toPath());

        String archive = DesktopPersonalDataArchive.exportArchive(source, fallback, 8L);
        assertTrue(archive.contains("klotski_save.dat"));
        assertArrayEquals(before, Files.readAllBytes(fallbackFile.toPath()));
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
        DesktopPersonalDataArchive.restoreArchive(archive, target);

        SaveManager.SaveData restored = SaveManager.loadGame(3);
        assertTrue(restored != null);
        assertEquals(3, restored.size);
        assertTrue(new File(target, "klotski_save.dat").isFile());
    }

    @Test
    void validRecoverySiblingIsResolvedToItsLogicalEntry() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel model = new GameModel(3);
        model.scramble(PuzzleDifficulty.CLASSIC, 303L);
        File canonical = new File(source, "klotski_save_3.json");
        assertTrue(SaveManager.saveGame(model, canonical));
        File recovery = new File(source, "klotski_save_3.json.tmp");
        Files.move(canonical.toPath(), recovery.toPath());

        String archive = DesktopPersonalDataArchive.exportArchive(source, 12L);
        assertTrue(archive.contains("\"id\": \"klotski_save_3.json\""));
        assertFalse(archive.contains("klotski_save_3.json.tmp"));
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
        DesktopPersonalDataArchive.restoreArchive(archive, target);
        assertTrue(SaveManager.loadGame(3) != null);
        assertTrue(new File(source, "klotski_save_3.json.tmp").isFile());
    }

    @Test
    void recoveryResolutionFollowsCanonicalTmpBakPrecedence() throws Exception {
        File source = new File(tempDir, "source");
        assertTrue(source.mkdirs());
        GameModel first = new GameModel(3);
        first.scramble(PuzzleDifficulty.CLASSIC, 401L);
        File canonical = new File(source, "klotski_save_3.json");
        assertTrue(SaveManager.saveGame(first, canonical));
        byte[] canonicalBytes = Files.readAllBytes(canonical.toPath());

        Files.move(canonical.toPath(), new File(source, "klotski_save_3.json.tmp").toPath());
        String tmpArchive = DesktopPersonalDataArchive.exportArchive(source, 21L);
        assertTrue(tmpArchive.contains("\"id\": \"klotski_save_3.json\""));

        Files.writeString(canonical.toPath(), "corrupt", StandardCharsets.UTF_8);
        String corruptPrimaryArchive = DesktopPersonalDataArchive.exportArchive(source, 22L);
        assertTrue(corruptPrimaryArchive.contains("\"id\": \"klotski_save_3.json\""));

        Files.writeString(new File(source, "klotski_save_3.json.tmp").toPath(),
                "corrupt", StandardCharsets.UTF_8);
        Files.write(new File(source, "klotski_save_3.json.bak").toPath(), canonicalBytes);
        String bakArchive = DesktopPersonalDataArchive.exportArchive(source, 23L);
        assertTrue(bakArchive.contains("\"id\": \"klotski_save_3.json\""));

        GameModel differing = new GameModel(3);
        differing.scramble(PuzzleDifficulty.CLASSIC, 402L);
        File different = new File(tempDir, "different.json");
        assertTrue(SaveManager.saveGame(differing, different));
        Files.write(new File(source, "klotski_save_3.json").toPath(), canonicalBytes);
        Files.copy(different.toPath(), new File(source, "klotski_save_3.json.bak").toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        String canonicalWinsArchive = DesktopPersonalDataArchive.exportArchive(source, 24L);
        assertTrue(canonicalWinsArchive.contains("\"id\": \"klotski_save_3.json\""));
        File canonicalTarget = new File(tempDir, "canonical-target");
        assertTrue(canonicalTarget.mkdirs());
        DesktopPersonalDataArchive.restoreArchive(canonicalWinsArchive, canonicalTarget);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, canonicalTarget.getAbsolutePath());
        assertArrayEquals(first.getInitialGridCopy(), SaveManager.loadGame(3).initialGrid);

        Files.writeString(canonical.toPath(), "bad", StandardCharsets.UTF_8);
        Files.writeString(new File(source, "klotski_save_3.json.tmp").toPath(),
                "bad", StandardCharsets.UTF_8);
        Files.writeString(new File(source, "klotski_save_3.json.bak").toPath(),
                "bad", StandardCharsets.UTF_8);
        byte[] badCanonical = Files.readAllBytes(canonical.toPath());
        byte[] badTemporary = Files.readAllBytes(new File(source,
                "klotski_save_3.json.tmp").toPath());
        assertThrows(java.io.IOException.class,
                () -> DesktopPersonalDataArchive.exportArchive(source, 25L));
        assertArrayEquals(badCanonical, Files.readAllBytes(canonical.toPath()));
        assertArrayEquals(badTemporary, Files.readAllBytes(new File(source,
                "klotski_save_3.json.tmp").toPath()));
    }

    @Test
    void preferenceRecoveryUsesTheSameLogicalPrecedence() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, source.getAbsolutePath());
        assertTrue(SaveManager.setDesktopLanguageTag("ja-JP"));
        File preferences = new File(source, "klotski_personal_preferences.json");
        Files.copy(preferences.toPath(), new File(source,
                "klotski_personal_preferences.json.tmp").toPath());
        Files.writeString(preferences.toPath(), "corrupt", StandardCharsets.UTF_8);

        String archive = DesktopPersonalDataArchive.exportArchive(source, 27L);
        assertTrue(archive.contains("klotski_personal_preferences.json"));
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
        DesktopPersonalDataArchive.restoreArchive(archive, target);
        assertEquals("ja-JP", SaveManager.getDesktopLanguageTag());
    }

    @Test
    void assistedNormalStateSurvivesDurableLegacySuppression() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, source.getAbsolutePath());
        GameModel model = new GameModel(3);
        model.scramble(PuzzleDifficulty.RELAXED, 404L);
        assertTrue(SaveManager.saveGame(model, true));
        String archive = DesktopPersonalDataArchive.exportArchive(source, 28L);
        DesktopPersonalDataArchive.restoreArchive(archive, target);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
        assertTrue(SaveManager.loadGame(3).assisted);
        assertTrue(new File(target, "klotski_saved_games_reset.marker").isFile());
    }

    @Test
    void continuousValidationRequiresBidirectionalMetadataOwnership() throws Exception {
        File source = new File(tempDir, "continuous-source");
        assertTrue(source.mkdirs());
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, source.getAbsolutePath());
        GameModel model = new GameModel(3);
        ContinuousChallenge challenge = ContinuousChallenge.start(3).completePuzzle(2, 40L, true);
        assertTrue(SaveManager.saveContinuousGame(model, challenge, true));
        byte[] meta = Files.readAllBytes(new File(source,
                "klotski_continuous_meta.json").toPath());
        byte[] current = Files.readAllBytes(new File(source,
                "klotski_continuous_current.json").toPath());
        byte[] assisted = Files.readAllBytes(new File(source,
                "klotski_continuous_current.json.assisted").toPath());

        File metaOnly = new File(tempDir, "meta-only");
        assertTrue(metaOnly.mkdirs());
        Files.write(new File(metaOnly, "klotski_continuous_meta.json").toPath(), meta);
        assertFalse(SaveManager.validatePersonalDataDirectory(metaOnly));

        File currentOnly = new File(tempDir, "current-only");
        assertTrue(currentOnly.mkdirs());
        Files.write(new File(currentOnly, "klotski_continuous_current.json").toPath(), current);
        assertFalse(SaveManager.validatePersonalDataDirectory(currentOnly));

        File assistedOnly = new File(tempDir, "assisted-only");
        assertTrue(assistedOnly.mkdirs());
        Files.write(new File(assistedOnly, "klotski_continuous_current.json.assisted").toPath(),
                assisted);
        assertFalse(SaveManager.validatePersonalDataDirectory(assistedOnly));

        File mismatch = new File(tempDir, "mismatch");
        assertTrue(mismatch.mkdirs());
        String mismatchedMeta = new String(meta, StandardCharsets.UTF_8)
                .replace("\"size\": 3", "\"size\": 4");
        Files.writeString(new File(mismatch, "klotski_continuous_meta.json").toPath(),
                mismatchedMeta, StandardCharsets.UTF_8);
        Files.write(new File(mismatch, "klotski_continuous_current.json").toPath(), current);
        Files.write(new File(mismatch, "klotski_continuous_current.json.assisted").toPath(), assisted);
        assertFalse(SaveManager.validatePersonalDataDirectory(mismatch));
    }

    @Test
    void backupPathPolicyAllowsUnmanagedTargetsAndRejectsCanonicalAliases() throws Exception {
        File data = new File(tempDir, "data");
        File fallback = new File(tempDir, "fallback");
        assertTrue(data.mkdirs());
        assertTrue(fallback.mkdirs());
        File ordinary = new File(tempDir, "owner-backup.json");
        File inside = new File(data, "owner-backup.json");
        assertDoesNotThrow(() -> DesktopPersonalDataArchive.validateExportDestination(
                ordinary, data, fallback));
        assertDoesNotThrow(() -> DesktopPersonalDataArchive.validateExportDestination(
                inside, data, fallback));

        String[] managed = {
            "klotski_save_3.json", "klotski_records_v2.json",
            "klotski_save_3.json.tmp", "klotski_save_3.json.bak"
        };
        for (String name : managed) {
            File file = new File(data, name);
            Files.writeString(file.toPath(), "managed", StandardCharsets.UTF_8);
            assertThrows(IllegalArgumentException.class,
                    () -> DesktopPersonalDataArchive.validateExportDestination(
                            new File(data, "nested/../" + name), data, fallback));
        }
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validateExportDestination(
                        new File(data, "klotski_save_5.json"), data, fallback));
        String[] rootLegacy = {"klotski_save.json", "klotski_save.dat", "klotski_records.json"};
        for (String name : rootLegacy) {
            File file = new File(fallback, name);
            Files.writeString(file.toPath(), "legacy", StandardCharsets.UTF_8);
            assertThrows(IllegalArgumentException.class,
                    () -> DesktopPersonalDataArchive.validateExportDestination(file, data, fallback));
        }

        File restoreSource = new File(data, "klotski_save_4.json");
        Files.writeString(restoreSource.toPath(), "managed", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validateRestoreSource(restoreSource, data, fallback));

        File transaction = new File(tempDir, ".slidedo-restore-active/candidate");
        assertTrue(transaction.mkdirs());
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validateExportDestination(
                        new File(transaction, "owner-backup.json"), data, fallback));
    }

    @Test
    void unmanagedBackupInsideDataDirectorySurvivesFullReplacement() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel model = new GameModel(3);
        model.scramble(PuzzleDifficulty.CLASSIC, 403L);
        assertTrue(SaveManager.saveGame(model, new File(source, "klotski_save_3.json")));
        String archive = DesktopPersonalDataArchive.exportArchive(source, 26L);
        File safeBackup = new File(target, "owner-backup.json");
        DesktopPersonalDataArchive.writeArchive(safeBackup, archive, target, new File(tempDir, "fallback"));
        DesktopPersonalDataArchive.restoreArchive(archive, target);
        assertTrue(safeBackup.isFile());
        assertTrue(new File(target, "klotski_save_3.json").isFile());
    }

    @Test
    void malformedUnknownTraversalAndOversizedEntriesAreRejectedWithoutMutation() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel validModel = new GameModel(3);
        validModel.scramble(PuzzleDifficulty.CLASSIC, 44L);
        assertTrue(SaveManager.saveGame(validModel, new File(source, "klotski_save_3.json")));
        File old = new File(target, "klotski_save_3.json");
        Files.writeString(old.toPath(), "old", StandardCharsets.UTF_8);
        String archive = DesktopPersonalDataArchive.exportArchive(source, 9L);

        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validate(archive.replace("\"version\": 1",
                        "\"version\": 2")));
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validate(archive.replace("slidedo-desktop-personal-data",
                        "wrong-format")));
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validate(archive.replace("klotski_save_3.json",
                        "unmanaged.json")));
        int entryStart = archive.indexOf("    {\"id\"");
        int entryEnd = archive.indexOf('\n', entryStart);
        String entry = archive.substring(entryStart, entryEnd);
        String duplicateEntry = archive.replace(entry, entry + ",\n" + entry);
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validate(duplicateEntry));
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validate(archive.replace("\"size\": ",
                        "\"size\": 1048577, \"unused\": 0, \"size2\": ")));
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validate(archive.replace("klotski_save_3.json",
                        "../escape")));
        String oversizedId = "klotski_save_" + "x".repeat(120) + ".json";
        assertTrue(oversizedId.length() > DesktopPersonalDataArchive.MAX_ENTRY_ID_LENGTH);
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validate(archive.replace("klotski_save_3.json",
                        oversizedId)));
        assertThrows(IllegalArgumentException.class,
                () -> DesktopPersonalDataArchive.validate(archive.substring(0, archive.length() - 4)));
        assertEquals("old", Files.readString(old.toPath()));
    }

    @Test
    void failedReplacementRollsBackAndLeavesNoTransactionDirectory() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, source.getAbsolutePath());
        GameModel model = new GameModel(3);
        model.scramble(PuzzleDifficulty.CLASSIC, 88L);
        assertTrue(SaveManager.saveGame(model));
        String archive = DesktopPersonalDataArchive.exportArchive(source, 10L);
        Files.writeString(new File(target, "unmanaged.txt").toPath(), "keep", StandardCharsets.UTF_8);
        Files.writeString(new File(target, "klotski_save_3.json").toPath(), "previous", StandardCharsets.UTF_8);

        AtomicBoolean failed = new AtomicBoolean();
        DesktopPersonalDataArchive.setFailureInjectorForTests(name -> {
            if (failed.compareAndSet(false, true)) {
                throw new java.io.IOException("injected write failure");
            }
        });
        assertThrows(java.io.IOException.class,
                () -> DesktopPersonalDataArchive.restoreArchive(archive, target));
        assertEquals("previous", Files.readString(new File(target, "klotski_save_3.json").toPath()));
        assertEquals("keep", Files.readString(new File(target, "unmanaged.txt").toPath()));
        assertEquals(0, Files.list(tempDir.toPath()).filter(path ->
                path.getFileName().toString().startsWith(".slidedo-restore-")).count());
    }

    @Test
    void managedDeleteFailureRollsBackAndLeavesNoTransactionDirectory() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel model = new GameModel(3);
        model.scramble(PuzzleDifficulty.CLASSIC, 89L);
        assertTrue(SaveManager.saveGame(model, new File(source, "klotski_save_3.json")));
        String archive = DesktopPersonalDataArchive.exportArchive(source, 11L);
        File previous = new File(target, "klotski_save_3.json");
        Files.writeString(previous.toPath(), "previous", StandardCharsets.UTF_8);

        DesktopPersonalDataArchive.setFailureInjectorForTests(new DesktopPersonalDataArchive.FailureInjector() {
            @Override
            public void beforeWrite(String entryName) {
            }

            @Override
            public void beforeDelete(String entryName) throws java.io.IOException {
                throw new java.io.IOException("injected managed delete failure");
            }
        });
        assertThrows(java.io.IOException.class,
                () -> DesktopPersonalDataArchive.restoreArchive(archive, target));
        assertEquals("previous", Files.readString(previous.toPath()));
        assertEquals(0, Files.list(tempDir.toPath()).filter(path ->
                path.getFileName().toString().startsWith(".slidedo-restore-")).count());
    }

    @Test
    void finalValidationFailureRollsBackAndLeavesNoTransactionDirectory() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel model = new GameModel(3);
        model.scramble(PuzzleDifficulty.CLASSIC, 90L);
        assertTrue(SaveManager.saveGame(model, new File(source, "klotski_save_3.json")));
        String archive = DesktopPersonalDataArchive.exportArchive(source, 12L);
        File previous = new File(target, "klotski_save_3.json");
        Files.writeString(previous.toPath(), "previous", StandardCharsets.UTF_8);

        DesktopPersonalDataArchive.setFailureInjectorForTests(new DesktopPersonalDataArchive.FailureInjector() {
            @Override
            public void beforeWrite(String entryName) {
            }

            @Override
            public void beforeValidation() throws java.io.IOException {
                throw new java.io.IOException("injected final validation failure");
            }
        });
        assertThrows(java.io.IOException.class,
                () -> DesktopPersonalDataArchive.restoreArchive(archive, target));
        assertEquals("previous", Files.readString(previous.toPath()));
        assertEquals(0, Files.list(tempDir.toPath()).filter(path ->
                path.getFileName().toString().startsWith(".slidedo-restore-")).count());
    }

    @Test
    void rollbackFailureRetainsRecoverySnapshotAndExplainsManualRecovery() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel model = new GameModel(3);
        model.scramble(PuzzleDifficulty.CLASSIC, 91L);
        assertTrue(SaveManager.saveGame(model, new File(source, "klotski_save_3.json")));
        String archive = DesktopPersonalDataArchive.exportArchive(source, 13L);
        File previous = new File(target, "klotski_save_3.json");
        Files.writeString(previous.toPath(), "previous", StandardCharsets.UTF_8);
        AtomicBoolean replacementFailed = new AtomicBoolean();
        DesktopPersonalDataArchive.setFailureInjectorForTests(new DesktopPersonalDataArchive.FailureInjector() {
            @Override
            public void beforeWrite(String entryName) throws java.io.IOException {
                if (replacementFailed.compareAndSet(false, true)) {
                    throw new java.io.IOException("injected replacement write failure");
                }
            }

            @Override
            public void beforeRollback(String entryName) throws java.io.IOException {
                throw new java.io.IOException("injected rollback failure");
            }
        });

        DesktopPersonalDataArchive.RestoreRecoveryRequiredException failure = assertThrows(
                DesktopPersonalDataArchive.RestoreRecoveryRequiredException.class,
                () -> DesktopPersonalDataArchive.restoreArchive(archive, target));
        assertTrue(failure.getMessage().contains("manual recovery"));
        assertTrue(failure.getRecoveryDirectory().isDirectory());
        File retained = new File(failure.getPreviousSnapshotDirectory(), "klotski_save_3.json");
        assertEquals("previous", Files.readString(retained.toPath()));
        assertTrue(failure.getRecoveryDirectory().getName().startsWith(".slidedo-restore-"));
    }

    @Test
    void successfulRestoreWithCleanupWarningKeepsTheGoodTargetAndRecoveryPath() throws Exception {
        File source = new File(tempDir, "source");
        File target = new File(tempDir, "target");
        assertTrue(source.mkdirs());
        assertTrue(target.mkdirs());
        GameModel model = new GameModel(3);
        model.scramble(PuzzleDifficulty.CLASSIC, 92L);
        assertTrue(SaveManager.saveGame(model, new File(source, "klotski_save_3.json")));
        String archive = DesktopPersonalDataArchive.exportArchive(source, 14L);
        DesktopPersonalDataArchive.setFailureInjectorForTests(new DesktopPersonalDataArchive.FailureInjector() {
            @Override
            public void beforeWrite(String entryName) {
            }

            @Override
            public void beforeCleanup() throws java.io.IOException {
                throw new java.io.IOException("injected cleanup failure");
            }
        });

        DesktopPersonalDataArchive.RestoreCleanupWarningException warning = assertThrows(
                DesktopPersonalDataArchive.RestoreCleanupWarningException.class,
                () -> DesktopPersonalDataArchive.restoreArchive(archive, target));
        assertTrue(warning.getRecoveryDirectory().isDirectory());
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, target.getAbsolutePath());
        assertTrue(SaveManager.loadGame(3) != null);
    }
}
