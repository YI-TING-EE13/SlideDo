package com.klotski.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Versioned, owner-controlled backup and full-replacement restore for the
 * Desktop personal-data namespace.
 *
 * <p>This is deliberately a Desktop format. Android's preference archive is
 * not reused because the Desktop store has independent files, legacy sources,
 * assisted sidecars, reset masks, and transaction-recovery requirements.</p>
 *
 * <p>Only the registered logical file names are archived. Temporary atomic
 * siblings, generated packages, IDE files, and every other file in the data
 * directory are outside the archive and are never copied into it.</p>
 */
public final class DesktopPersonalDataArchive {
    /** Stable archive format identifier. */
    public static final String FORMAT = "slidedo-desktop-personal-data";

    /** Current Desktop archive schema version. */
    public static final int FORMAT_VERSION = 1;

    /** Maximum UTF-8 archive size accepted by the codec. */
    public static final int MAX_ARCHIVE_BYTES = 8 * 1024 * 1024;

    /** Maximum decoded bytes for one logical entry. */
    public static final int MAX_ENTRY_BYTES = 1024 * 1024;

    /** Maximum decoded bytes across all logical entries. */
    public static final int MAX_TOTAL_ENTRY_BYTES = 4 * 1024 * 1024;

    /** Maximum number of logical entries in one archive. */
    public static final int MAX_ENTRIES = 512;

    /** Maximum UTF-16 code units allowed in one logical entry identifier. */
    public static final int MAX_ENTRY_ID_LENGTH = 128;

    private static final String SAVE_PREFIX = "klotski_save_";
    private static final String SAVE_SUFFIX = ".json";
    private static final String LEGACY_JSON = "klotski_save.json";
    private static final String LEGACY_SERIALIZED = "klotski_save.dat";
    private static final String RECORDS = "klotski_records.json";
    private static final String SCOPED_RECORDS = "klotski_records_v2.json";
    private static final String RECORDS_RESET = "klotski_records_reset.marker";
    private static final String SAVED_GAMES_RESET = "klotski_saved_games_reset.marker";
    private static final String STATISTICS = "klotski_statistics.json";
    private static final String DAILY_PREFIX = "klotski_daily_";
    private static final String DAILY_PROGRESS = "klotski_daily_progress.json";
    private static final String FAVORITES = "klotski_favorites.json";
    private static final String FAVORITE_PREFIX = "klotski_favorite_";
    private static final String PREFERENCES = "klotski_personal_preferences.json";
    private static final String CONTINUOUS_META = "klotski_continuous_meta.json";
    private static final String CONTINUOUS_CURRENT = "klotski_continuous_current.json";
    private static final String[] LEGACY_FALLBACK_NAMES = {
            "klotski_save_3.json", "klotski_save_4.json", "klotski_save_5.json",
            LEGACY_JSON, LEGACY_SERIALIZED, RECORDS, SCOPED_RECORDS
    };

    private static final Pattern SAVE_PATTERN = Pattern.compile(
            "klotski_save_[345]\\.json");
    private static final Pattern DAILY_PATTERN = Pattern.compile(
            "klotski_daily_\\d{4}-\\d{2}-\\d{2}(?:\\.json|\\.assisted)");
    private static final Pattern FAVORITE_RUN_PATTERN = Pattern.compile(
            "klotski_favorite_[0-9a-f]{64}(?:\\.json|\\.assisted)");
    private static final Pattern TEMP_PATTERN = Pattern.compile(".+\\.(?:tmp|bak)");

    private static volatile FailureInjector failureInjector;

    private DesktopPersonalDataArchive() {
    }

    /** Hook used only by deterministic package tests to fail bounded restore operations. */
    @FunctionalInterface
    interface FailureInjector {
        /** Invoked immediately before a candidate entry is written. */
        void beforeWrite(String entryName) throws IOException;

        /** Invoked immediately before a managed destination entry is deleted. */
        default void beforeDelete(String entryName) throws IOException {
        }

        /** Invoked immediately before final post-write validation. */
        default void beforeValidation() throws IOException {
        }

        /** Invoked immediately before a rollback delete or write. */
        default void beforeRollback(String entryName) throws IOException {
        }

        /** Invoked immediately before transaction cleanup. */
        default void beforeCleanup() throws IOException {
        }
    }

    static void setFailureInjectorForTests(FailureInjector injector) {
        failureInjector = injector;
    }

    /** Raised when the old state could not be restored and recovery material was retained. */
    public static final class RestoreRecoveryRequiredException extends IOException {
        private static final long serialVersionUID = 1L;
        /** Retained transaction directory. */
        private final File recoveryDirectory;
        /** Retained pre-restore managed snapshot directory. */
        private final File previousSnapshotDirectory;

        private RestoreRecoveryRequiredException(String message, Path recoveryDirectory,
                Path previousSnapshotDirectory, IOException originalFailure,
                IOException rollbackFailure) {
            super(message, originalFailure);
            this.recoveryDirectory = recoveryDirectory.toFile();
            this.previousSnapshotDirectory = previousSnapshotDirectory.toFile();
            addSuppressed(rollbackFailure);
        }

        /**
         * Returns the retained transaction directory containing recovery material.
         *
         * @return retained transaction directory
         */
        public File getRecoveryDirectory() {
            return recoveryDirectory;
        }

        /**
         * Returns the retained snapshot of the pre-restore managed state.
         *
         * @return retained previous snapshot directory
         */
        public File getPreviousSnapshotDirectory() {
            return previousSnapshotDirectory;
        }
    }

    /** Raised after a good restore when transaction cleanup could not be completed. */
    public static final class RestoreCleanupWarningException extends IOException {
        private static final long serialVersionUID = 1L;
        /** Retained transaction directory. */
        private final File recoveryDirectory;

        private RestoreCleanupWarningException(String message, Path recoveryDirectory,
                IOException cleanupFailure) {
            super(message, cleanupFailure);
            this.recoveryDirectory = recoveryDirectory.toFile();
        }

        /**
         * Returns the retained transaction directory that may be removed after inspection.
         *
         * @return retained transaction directory
         */
        public File getRecoveryDirectory() {
            return recoveryDirectory;
        }
    }

    /**
     * Exports the configured Desktop data directory using the current clock.
     * Historical project-root normal-save and records fallback files are
     * included when they are the only valid source for a logical entry; the
     * source files are read but never migrated or rewritten.
     *
     * @return deterministic-entry JSON archive
     * @throws IOException when the source changes during the snapshot or is
     *         outside the archive bounds
     */
    public static String exportArchive() throws IOException {
        return exportArchive(SaveManager.getDataDirectory(), new File("."),
                System.currentTimeMillis());
    }

    /**
     * Exports one data directory using the current clock.
     *
     * @param dataDirectory source personal-data directory
     * @return JSON archive
     * @throws IOException when the source cannot be read or validated
     */
    public static String exportArchive(File dataDirectory) throws IOException {
        return exportArchive(dataDirectory, System.currentTimeMillis());
    }

    /**
     * Testable export overload with an explicit archive timestamp.
     *
     * @param dataDirectory source personal-data directory
     * @param createdAt archive creation time in epoch milliseconds
     * @return JSON archive
     * @throws IOException when the source cannot be read or validated
     */
    public static String exportArchive(File dataDirectory, long createdAt) throws IOException {
        return exportArchive(dataDirectory, null, createdAt);
    }

    /**
     * Testable export overload that also models the historical project-root
     * fallback directory used by {@link SaveManager}. The no-argument
     * production entry point supplies the real process root; explicit data
     * directory overloads intentionally omit it so isolated tests cannot
     * accidentally capture the developer's legacy files.
     *
     * @param dataDirectory canonical Desktop data directory
     * @param legacyFallbackDirectory historical fallback directory, or null
     * @param createdAt archive creation time in epoch milliseconds
     * @return deterministic JSON archive
     * @throws IOException when a source cannot be read or validated
     */
    static String exportArchive(File dataDirectory, File legacyFallbackDirectory,
            long createdAt) throws IOException {
        if (dataDirectory == null) {
            throw new IOException("Desktop data directory is missing.");
        }
        if (dataDirectory.exists() && !dataDirectory.isDirectory()) {
            throw new IOException("Desktop data path is not a directory.");
        }
        Map<String, byte[]> entries = resolveLogicalEntries(dataDirectory,
                legacyFallbackDirectory);
        validateResolvedEntries(entries);
        return encode(entries, createdAt);
    }

    /**
     * Resolves the same logical canonical names that SaveManager loaders use.
     * A valid primary wins; an invalid or absent primary falls back to .tmp and
     * then .bak. Recovery siblings are never emitted as physical archive IDs.
     */
    private static Map<String, byte[]> resolveLogicalEntries(File dataDirectory,
            File fallbackDirectory) throws IOException {
        Map<String, List<SourceCandidate>> candidates = collectSourceCandidates(
                dataDirectory, fallbackDirectory);
        Map<String, byte[]> entries = new TreeMap<>();

        resolveContinuousEntries(candidates, entries);
        boolean progressed;
        do {
            progressed = false;
            for (Map.Entry<String, List<SourceCandidate>> entry : candidates.entrySet()) {
                String logicalName = entry.getKey();
                if (isContinuousName(logicalName) || entries.containsKey(logicalName)) {
                    continue;
                }
                for (SourceCandidate candidate : entry.getValue()) {
                    byte[] bytes;
                    try {
                        bytes = readStableSource(candidate.file, candidate.file.getName());
                    } catch (IOException exception) {
                        continue;
                    }
                    if (isValidCandidateEntries(entries, logicalName, bytes)) {
                        entries.put(logicalName, bytes);
                        progressed = true;
                        break;
                    }
                }
            }
        } while (progressed);

        for (Map.Entry<String, List<SourceCandidate>> entry : candidates.entrySet()) {
            if (!entries.containsKey(entry.getKey())) {
                throw new IOException("No valid recovery candidate for managed entry: "
                        + entry.getKey());
            }
        }
        return entries;
    }

    private static Map<String, List<SourceCandidate>> collectSourceCandidates(
            File dataDirectory, File fallbackDirectory) throws IOException {
        Map<String, List<SourceCandidate>> candidates = new TreeMap<>();
        collectSourceCandidates(candidates, dataDirectory, null, 0);
        if (fallbackDirectory != null && !sameFile(dataDirectory, fallbackDirectory)) {
            collectSourceCandidates(candidates, fallbackDirectory,
                    Set.of(LEGACY_FALLBACK_NAMES), 100);
        }
        for (List<SourceCandidate> values : candidates.values()) {
            values.sort(Comparator.comparingInt((SourceCandidate value) -> value.priority)
                    .thenComparing(value -> value.file.getName()));
        }
        return candidates;
    }

    private static void collectSourceCandidates(Map<String, List<SourceCandidate>> result,
            File directory, Set<String> allowedNames, int sourcePriority) throws IOException {
        if (directory == null || !directory.exists()) {
            return;
        }
        if (!directory.isDirectory()) {
            throw new IOException("Managed data path is not a directory: " + directory);
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Managed data directory cannot be listed: " + directory);
        }
        for (File file : files) {
            String physicalName = file.getName();
            String logicalName = stripRecoverySuffix(physicalName);
            boolean recovery = !physicalName.equals(logicalName);
            if (!isArchiveName(logicalName)
                    || (allowedNames != null && !allowedNames.contains(logicalName))) {
                continue;
            }
            if (!file.isFile() || file.length() > MAX_ENTRY_BYTES) {
                throw new IOException("Managed archive entry is not a bounded file: "
                        + physicalName);
            }
            int suffixPriority = recovery
                    ? (physicalName.endsWith(".tmp") ? 1 : 2) : 0;
            result.computeIfAbsent(logicalName, ignored -> new ArrayList<>())
                    .add(new SourceCandidate(file, sourcePriority + suffixPriority));
        }
    }

    private static void resolveContinuousEntries(Map<String, List<SourceCandidate>> candidates,
            Map<String, byte[]> entries) throws IOException {
        boolean hasContinuous = candidates.keySet().stream().anyMatch(
                DesktopPersonalDataArchive::isContinuousName);
        if (!hasContinuous) {
            return;
        }
        List<SourceCandidate> meta = candidates.getOrDefault(CONTINUOUS_META,
                Collections.emptyList());
        List<SourceCandidate> current = candidates.getOrDefault(CONTINUOUS_CURRENT,
                Collections.emptyList());
        List<SourceCandidate> assisted = candidates.getOrDefault(
                CONTINUOUS_CURRENT + ".assisted", Collections.emptyList());
        List<SourceCandidate> metaOptions = withAbsentOption(meta);
        List<SourceCandidate> currentOptions = withAbsentOption(current);
        List<SourceCandidate> assistedOptions = withAbsentOption(assisted);
        for (SourceCandidate metaCandidate : metaOptions) {
            for (SourceCandidate currentCandidate : currentOptions) {
                for (SourceCandidate assistedCandidate : assistedOptions) {
                    Map<String, byte[]> trial = new TreeMap<>(entries);
                    addCandidate(trial, CONTINUOUS_META, metaCandidate);
                    addCandidate(trial, CONTINUOUS_CURRENT, currentCandidate);
                    addCandidate(trial, CONTINUOUS_CURRENT + ".assisted", assistedCandidate);
                    if (trial.isEmpty() || !isValidCandidateMap(trial)) {
                        continue;
                    }
                    entries.putAll(trial);
                    return;
                }
            }
        }
        throw new IOException("Continuous Challenge recovery candidates are inconsistent: "
                + meta.size() + "/" + current.size() + "/" + assisted.size());
    }

    private static List<SourceCandidate> withAbsentOption(List<SourceCandidate> candidates) {
        List<SourceCandidate> result = new ArrayList<>();
        if (candidates.isEmpty()) {
            result.add(null);
        } else {
            result.addAll(candidates);
        }
        return result;
    }

    private static void addCandidate(Map<String, byte[]> entries, String logicalName,
            SourceCandidate candidate) throws IOException {
        if (candidate != null) {
            entries.put(logicalName, readStableSource(candidate.file, candidate.file.getName()));
        }
    }

    private static boolean isContinuousName(String name) {
        return CONTINUOUS_META.equals(name) || CONTINUOUS_CURRENT.equals(name)
                || (CONTINUOUS_CURRENT + ".assisted").equals(name);
    }

    private static boolean isValidCandidateMap(Map<String, byte[]> entries) {
        Path temporary = null;
        try {
            temporary = Files.createTempDirectory("slidedo-logical-validate-");
            writeCandidate(temporary, entries, false);
            return SaveManager.validatePersonalDataDirectory(temporary.toFile());
        } catch (IOException | RuntimeException exception) {
            return false;
        } finally {
            deleteTreeQuietly(temporary);
        }
    }

    private static void validateResolvedEntries(Map<String, byte[]> entries) throws IOException {
        if (!isValidCandidateMap(entries)) {
            throw new IOException("Desktop data contains an invalid managed state.");
        }
    }

    private static final class SourceCandidate {
        private final File file;
        private final int priority;

        private SourceCandidate(File file, int priority) {
            this.file = file;
            this.priority = priority;
        }
    }

    private static byte[] readStableSource(File file, String name) throws IOException {
        BasicFileAttributes before = Files.readAttributes(file.toPath(),
                BasicFileAttributes.class);
        byte[] bytes = Files.readAllBytes(file.toPath());
        BasicFileAttributes after = Files.readAttributes(file.toPath(),
                BasicFileAttributes.class);
        if (before.size() != after.size()
                || before.lastModifiedTime().toMillis() != after.lastModifiedTime().toMillis()) {
            throw new IOException("Managed data changed during backup: " + name);
        }
        return bytes;
    }

    private static boolean sameFile(File first, File second) {
        try {
            return first.getCanonicalFile().equals(second.getCanonicalFile());
        } catch (IOException exception) {
            return first.getAbsoluteFile().equals(second.getAbsoluteFile());
        }
    }

    private static boolean isValidCandidateEntries(Map<String, byte[]> existing,
            String name, byte[] bytes) {
        Path temporary = null;
        try {
            temporary = Files.createTempDirectory("slidedo-legacy-validate-");
            Map<String, byte[]> candidate = new TreeMap<>(existing);
            candidate.put(name, bytes);
            writeCandidate(temporary, candidate, false);
            return SaveManager.validatePersonalDataDirectory(temporary.toFile());
        } catch (IOException | RuntimeException exception) {
            return false;
        } finally {
            deleteTreeQuietly(temporary);
        }
    }

    /**
     * Validates an archive without changing the configured data directory.
     *
     * @param archive candidate archive text
     * @throws IllegalArgumentException when structure, bounds, or inner state
     *         is invalid
     */
    public static void validate(String archive) {
        ArchiveDocument document = parse(archive);
        Path temporary = null;
        try {
            temporary = Files.createTempDirectory("slidedo-archive-validate-");
            writeCandidate(temporary, document.entries, false);
            if (!SaveManager.validatePersonalDataDirectory(temporary.toFile())) {
                throw invalid("Archive contains invalid or inconsistent personal state.");
            }
        } catch (IOException exception) {
            throw invalid("Archive could not be validated.", exception);
        } finally {
            deleteTreeQuietly(temporary);
        }
    }

    /**
     * Replaces the configured Desktop managed namespace after complete
     * validation. Unmanaged files remain untouched.
     *
     * @param archive validated archive text
     * @throws IOException when the replacement fails and the previous state is
     *         restored
     * @throws IllegalArgumentException when the archive is invalid
     */
    public static void restoreArchive(String archive) throws IOException {
        restoreArchive(archive, SaveManager.getDataDirectory());
    }

    /**
     * Testable restore overload for an explicit data directory.
     *
     * @param archive validated archive text
     * @param dataDirectory target personal-data directory
     * @throws IOException when the replacement fails
     * @throws IllegalArgumentException when the archive is invalid
     */
    public static synchronized void restoreArchive(String archive, File dataDirectory)
            throws IOException {
        if (dataDirectory == null) {
            throw new IOException("Desktop data directory is missing.");
        }
        ArchiveDocument document = parse(archive);
        // Validate the complete inner state before creating a transaction
        // beside the user's data directory. Invalid input therefore cannot
        // even create a target parent or touch a managed file.
        validate(archive);
        Path target = dataDirectory.toPath().toAbsolutePath().normalize();
        if (Files.exists(target) && !Files.isDirectory(target)) {
            throw new IOException("Desktop data path is not a directory.");
        }

        Path parent = target.getParent();
        if (parent == null) {
            throw new IOException("Desktop data directory has no recoverable parent.");
        }
        Files.createDirectories(parent);
        Path transaction = parent.resolve(".slidedo-restore-" + UUID.randomUUID());
        Path candidate = transaction.resolve("candidate");
        Path previous = transaction.resolve("previous");
        boolean targetExisted = Files.exists(target);
        boolean snapshotReady = false;
        try {
            Files.createDirectories(candidate);
            Files.createDirectories(previous);
            Map<String, byte[]> desired = new TreeMap<>(document.entries);
            addLegacyMasks(desired);
            writeCandidate(candidate, desired, false);
            if (!SaveManager.validatePersonalDataDirectory(candidate.toFile())) {
                throw invalid("Archive contains invalid or inconsistent personal state.");
            }
            snapshotManaged(target, previous);
            snapshotReady = true;
            replaceManaged(target, desired);
            if (failureInjector != null) {
                failureInjector.beforeValidation();
            }
            if (!SaveManager.validatePersonalDataDirectory(target.toFile())) {
                throw new IOException("Restored personal state failed final validation.");
            }
        } catch (IllegalArgumentException | IOException failure) {
            if (snapshotReady) {
                try {
                    restorePrevious(target, previous);
                    if (!targetExisted && isEmptyDirectory(target)) {
                        Files.deleteIfExists(target);
                    }
                } catch (IOException rollbackFailure) {
                    throw new RestoreRecoveryRequiredException(
                            "Restore failed and rollback is incomplete; manual recovery is "
                                    + "required from " + previous.toAbsolutePath(),
                            transaction, previous, asIOException(failure), rollbackFailure);
                }
            }
            try {
                cleanupTransaction(transaction);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(new RestoreCleanupWarningException(
                        "Restore transaction cleanup failed; inspect "
                                + transaction.toAbsolutePath(), transaction, cleanupFailure));
            }
            if (failure instanceof IllegalArgumentException invalidArchive) {
                throw invalidArchive;
            }
            throw (IOException) failure;
        }
        try {
            cleanupTransaction(transaction);
        } catch (IOException cleanupFailure) {
            throw new RestoreCleanupWarningException(
                    "Restore succeeded but transaction cleanup is incomplete; inspect "
                            + transaction.toAbsolutePath(), transaction, cleanupFailure);
        }
    }

    private static IOException asIOException(Exception failure) {
        if (failure instanceof IOException ioFailure) {
            return ioFailure;
        }
        return new IOException(failure.getMessage(), failure);
    }

    private static void cleanupTransaction(Path transaction) throws IOException {
        if (failureInjector != null) {
            failureInjector.beforeCleanup();
        }
        deleteTreeOrThrow(transaction);
    }

    /**
     * Rejects an Export destination that aliases managed state, a recovery
     * sibling, a project-root legacy fallback, or an active restore transaction.
     * Unmanaged files inside the data directory remain valid backup targets.
     *
     * @param destination selected owner backup file
     * @throws IOException when the path cannot be canonicalized
     * @throws IllegalArgumentException when the destination is unsafe
     */
    public static void validateExportDestination(File destination) throws IOException {
        validateExportDestination(destination, SaveManager.getDataDirectory(), new File("."));
    }

    static void validateExportDestination(File destination, File dataDirectory,
            File fallbackDirectory) throws IOException {
        validateBackupPath(destination, dataDirectory, fallbackDirectory, false);
    }

    /**
     * Rejects a Restore source that would be removed or replaced by the target
     * transaction. The source is otherwise read-only and remains unchanged.
     *
     * @param source selected archive file
     * @throws IOException when the path cannot be canonicalized
     * @throws IllegalArgumentException when the source aliases managed state
     */
    public static void validateRestoreSource(File source) throws IOException {
        validateRestoreSource(source, SaveManager.getDataDirectory(), new File("."));
    }

    static void validateRestoreSource(File source, File dataDirectory,
            File fallbackDirectory) throws IOException {
        validateBackupPath(source, dataDirectory, fallbackDirectory, true);
    }

    private static void validateBackupPath(File selected, File dataDirectory,
            File fallbackDirectory, boolean restoreSource) throws IOException {
        if (selected == null) {
            throw new IllegalArgumentException("Backup path is missing.");
        }
        Path selectedPath = canonicalPath(selected);
        if (Files.exists(selectedPath) && Files.isDirectory(selectedPath)) {
            throw new IllegalArgumentException("Backup path must be a file.");
        }
        Path dataPath = canonicalPath(dataDirectory);
        if (selectedPath.getParent() != null
                && samePath(selectedPath.getParent(), dataPath)
                && isManagedName(selectedPath.getFileName().toString())) {
            throw new IllegalArgumentException("Backup path aliases managed Desktop data: "
                    + selectedPath.getFileName());
        }
        if (dataDirectory != null && dataDirectory.exists()) {
            File[] files = dataDirectory.listFiles();
            if (files == null) {
                throw new IOException("Desktop data directory cannot be listed.");
            }
            for (File file : files) {
                if (isManagedName(file.getName())
                        && samePath(selectedPath, canonicalPath(file))) {
                    throw new IllegalArgumentException("Backup path aliases managed Desktop data: "
                            + file.getName());
                }
            }
        }
        if (fallbackDirectory != null && fallbackDirectory.exists()) {
            Path fallbackPath = canonicalPath(fallbackDirectory);
            if (selectedPath.getParent() != null
                    && samePath(selectedPath.getParent(), fallbackPath)
                    && isLegacyFallbackPhysicalName(selectedPath.getFileName().toString())) {
                throw new IllegalArgumentException(
                        "Backup path aliases a project-root legacy fallback: "
                                + selectedPath.getFileName());
            }
            for (String name : LEGACY_FALLBACK_NAMES) {
                for (String suffix : new String[] {"", ".tmp", ".bak"}) {
                    File file = new File(fallbackDirectory, name + suffix);
                    if (file.exists() && samePath(selectedPath, canonicalPath(file))) {
                        throw new IllegalArgumentException(
                                "Backup path aliases a project-root legacy fallback: "
                                        + file.getName());
                    }
                }
            }
        }
        Path parent = dataPath.getParent();
        if (parent != null && Files.isDirectory(parent)) {
            try (var stream = Files.list(parent)) {
                for (Path candidate : stream.collect(Collectors.toList())) {
                    if (candidate.getFileName().toString().startsWith(".slidedo-restore-")) {
                        Path transaction = canonicalPath(candidate.toFile());
                        if (samePath(selectedPath, transaction)
                                || isWithin(selectedPath, transaction)) {
                            throw new IllegalArgumentException(
                                    "Backup path aliases an active restore transaction.");
                        }
                    }
                }
            }
        }
        if (restoreSource && dataDirectory != null && dataDirectory.exists()
                && isWithin(selectedPath, dataPath)) {
            File selectedFile = selectedPath.toFile();
            if (isManagedName(selectedFile.getName())) {
                throw new IllegalArgumentException(
                        "Restore source would be replaced by the restore transaction.");
            }
        }
    }

    /**
     * Writes an owner backup through a sibling temporary file and atomic replace.
     *
     * @param destination external owner backup destination
     * @param archive validated archive text
     * @throws IOException when validation, writing, or replacement fails
     */
    public static void writeArchive(File destination, String archive) throws IOException {
        validateExportDestination(destination);
        validate(archive);
        writeArchive(destination, archive, SaveManager.getDataDirectory(), new File("."));
    }

    static void writeArchive(File destination, String archive, File dataDirectory,
            File fallbackDirectory) throws IOException {
        validateExportDestination(destination, dataDirectory, fallbackDirectory);
        if (archive == null || archive.getBytes(StandardCharsets.UTF_8).length > MAX_ARCHIVE_BYTES) {
            throw new IOException("Backup archive is empty or exceeds the supported size.");
        }
        Path target = canonicalPath(destination);
        Path parent = target.getParent();
        if (parent == null) {
            throw new IOException("Backup destination has no parent directory.");
        }
        Files.createDirectories(parent);
        Path temporary = parent.resolve(".slidedo-backup-" + UUID.randomUUID() + ".tmp");
        try {
            Files.write(temporary, archive.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path canonicalPath(File file) throws IOException {
        if (file == null) {
            throw new IOException("Path is missing.");
        }
        File canonical = file.getCanonicalFile();
        Path path = canonical.toPath().toAbsolutePath().normalize();
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            try {
                return path.toRealPath(LinkOption.NOFOLLOW_LINKS);
            } catch (IOException ignored) {
                return path;
            }
        }
        return path;
    }

    private static boolean samePath(Path first, Path second) {
        try {
            if (Files.exists(first) && Files.exists(second) && Files.isSameFile(first, second)) {
                return true;
            }
        } catch (IOException ignored) {
            // Fall back to canonical component comparison.
        }
        return equalPathComponents(first, second);
    }

    private static boolean isWithin(Path path, Path root) {
        if (samePath(path, root)) {
            return true;
        }
        try {
            Path relative = root.relativize(path);
            if (relative.isAbsolute() || relative.getNameCount() == 0) {
                return false;
            }
            return !relative.startsWith("..");
        } catch (IllegalArgumentException exception) {
            return equalPathComponents(path, root);
        }
    }

    private static boolean equalPathComponents(Path first, Path second) {
        if (first.getNameCount() != second.getNameCount()
                || !first.getRoot().toString().equalsIgnoreCase(second.getRoot().toString())) {
            return false;
        }
        for (int index = 0; index < first.getNameCount(); index++) {
            String left = first.getName(index).toString();
            String right = second.getName(index).toString();
            if (isWindows() ? !left.equalsIgnoreCase(right) : !left.equals(right)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWindows() {
        return File.separatorChar == '\\';
    }

    private static boolean isLegacyFallbackPhysicalName(String name) {
        for (String logical : LEGACY_FALLBACK_NAMES) {
            if (logical.equals(name) || (logical + ".tmp").equals(name)
                    || (logical + ".bak").equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether a file name belongs to the explicitly managed Desktop
     * namespace, including temporary/recovery siblings that must be removed
     * during full replacement.
     *
     * @param name basename to classify
     * @return true for a registered managed name
     */
    public static boolean isManagedName(String name) {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\")
                || name.contains(":") || name.startsWith("..")) {
            return false;
        }
        String canonical = stripRecoverySuffix(name);
        return isCanonicalName(canonical);
    }

    /**
     * Returns whether a file is a durable archive entry rather than a
     * temporary/recovery sibling.
     */
    static boolean isArchiveName(String name) {
        return isManagedName(name) && !TEMP_PATTERN.matcher(name).matches();
    }

    /**
     * Returns the bounded namespace patterns for documentation and tests.
     *
     * @return stable English registry descriptions
     */
    public static String[] managedNamespacePatterns() {
        return new String[] {
                "klotski_save_[345].json",
                "klotski_save.json / klotski_save.dat (legacy)",
                "klotski_records.json / klotski_records_v2.json / reset markers",
                "klotski_statistics.json",
                "klotski_daily_YYYY-MM-DD.json / .assisted / progress",
                "klotski_favorites.json",
                "klotski_favorite_<sha256>.json / .assisted",
                "klotski_personal_preferences.json",
                "klotski_continuous_meta.json / current.json / current.json.assisted",
                "registered names plus .tmp/.bak recovery siblings resolved logically",
                "generated ZIP/app-image/build/IDE/local files are not managed"
        };
    }

    private static String encode(Map<String, byte[]> entries, long createdAt) {
        if (entries.size() > MAX_ENTRIES) {
            throw invalid("Archive contains too many entries.");
        }
        long total = 0;
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"format\": ").append(jsonString(FORMAT))
                .append(",\n  \"version\": ").append(FORMAT_VERSION)
                .append(",\n  \"createdAt\": ").append(Math.max(0L, createdAt))
                .append(",\n  \"entries\": [");
        int index = 0;
        for (Map.Entry<String, byte[]> entry : new TreeMap<>(entries).entrySet()) {
            String name = entry.getKey();
            byte[] bytes = entry.getValue();
            if (!isArchiveName(name) || bytes == null || bytes.length > MAX_ENTRY_BYTES) {
                throw invalid("Archive contains an invalid managed entry.");
            }
            total += bytes.length;
            if (total > MAX_TOTAL_ENTRY_BYTES) {
                throw invalid("Archive exceeds the supported data size.");
            }
            String payload = Base64.getEncoder().encodeToString(bytes);
            if (index++ > 0) {
                json.append(",");
            }
            json.append("\n    {\"id\": ").append(jsonString(name))
                    .append(", \"size\": ").append(bytes.length)
                    .append(", \"sha256\": ").append(jsonString(sha256(bytes)))
                    .append(", \"payload\": ").append(jsonString(payload)).append("}");
        }
        json.append("\n  ]\n}\n");
        byte[] encoded = json.toString().getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_ARCHIVE_BYTES) {
            throw invalid("Archive exceeds the supported size.");
        }
        return json.toString();
    }

    private static ArchiveDocument parse(String archive) {
        if (archive == null || archive.isBlank()
                || archive.getBytes(StandardCharsets.UTF_8).length > MAX_ARCHIVE_BYTES) {
            throw invalid("Archive is empty or exceeds the supported size.");
        }
        Object rootValue;
        try {
            rootValue = new JsonParser(archive).parse();
        } catch (RuntimeException exception) {
            throw invalid("Archive JSON is malformed.", exception);
        }
        if (!(rootValue instanceof Map<?, ?> root)) {
            throw invalid("Archive root must be an object.");
        }
        requireKeys(root, "format", "version", "createdAt", "entries");
        if (!FORMAT.equals(root.get("format")) || !Long.valueOf(FORMAT_VERSION).equals(root.get("version"))) {
            throw invalid("Archive format or version is not supported.");
        }
        Object created = root.get("createdAt");
        if (!(created instanceof Long) || ((Long) created) < 0) {
            throw invalid("Archive creation time is invalid.");
        }
        if (!(root.get("entries") instanceof List<?> entriesValue)
                || entriesValue.size() > MAX_ENTRIES) {
            throw invalid("Archive entries are invalid or exceed the limit.");
        }
        Map<String, byte[]> entries = new TreeMap<>();
        long total = 0;
        for (Object entryValue : entriesValue) {
            if (!(entryValue instanceof Map<?, ?> entry)) {
                throw invalid("Archive entry must be an object.");
            }
            requireKeys(entry, "id", "size", "sha256", "payload");
            Object idValue = entry.get("id");
            if (!(idValue instanceof String id) || !isSafeArchiveId(id)
                    || !isArchiveName(id) || entries.containsKey(id)) {
                throw invalid("Archive contains an invalid, duplicate, or unmanaged entry.");
            }
            Object sizeValue = entry.get("size");
            if (!(sizeValue instanceof Long) || ((Long) sizeValue) < 0
                    || ((Long) sizeValue) > MAX_ENTRY_BYTES) {
                throw invalid("Archive entry size is invalid.");
            }
            Object digestValue = entry.get("sha256");
            Object payloadValue = entry.get("payload");
            if (!(digestValue instanceof String digest)
                    || !digest.matches("[0-9a-f]{64}")
                    || !(payloadValue instanceof String payload)) {
                throw invalid("Archive entry integrity fields are invalid.");
            }
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(payload);
            } catch (IllegalArgumentException exception) {
                throw invalid("Archive entry payload is not valid Base64.", exception);
            }
            if (bytes.length != ((Long) sizeValue).intValue()
                    || !digest.equals(sha256(bytes))) {
                throw invalid("Archive entry integrity check failed.");
            }
            total += bytes.length;
            if (total > MAX_TOTAL_ENTRY_BYTES) {
                throw invalid("Archive exceeds the supported data size.");
            }
            entries.put(id, bytes);
        }
        return new ArchiveDocument((Long) created, entries);
    }

    private static void requireKeys(Map<?, ?> map, String... required) {
        if (map.size() != required.length) {
            throw invalid("Archive contains unknown or missing fields.");
        }
        for (String key : required) {
            if (!map.containsKey(key)) {
                throw invalid("Archive is missing field: " + key);
            }
        }
    }

    private static boolean isSafeArchiveId(String id) {
        return id.length() <= MAX_ENTRY_ID_LENGTH
                && !id.isBlank() && !id.startsWith("/") && !id.startsWith("\\")
                && !id.contains("/") && !id.contains("\\") && !id.contains(":")
                && !id.contains("..") && !id.startsWith(".");
    }

    private static boolean isCanonicalName(String name) {
        if (name.equals(LEGACY_JSON) || name.equals(LEGACY_SERIALIZED)
                || name.equals(RECORDS) || name.equals(SCOPED_RECORDS)
                || name.equals(RECORDS_RESET) || name.equals(SAVED_GAMES_RESET)
                || name.equals(STATISTICS) || name.equals(DAILY_PROGRESS)
                || name.equals(FAVORITES) || name.equals(PREFERENCES)
                || name.equals(CONTINUOUS_META) || name.equals(CONTINUOUS_CURRENT)
                || name.equals(CONTINUOUS_CURRENT + ".assisted")) {
            return true;
        }
        return SAVE_PATTERN.matcher(name).matches()
                || DAILY_PATTERN.matcher(name).matches()
                || FAVORITE_RUN_PATTERN.matcher(name).matches();
    }

    private static String stripRecoverySuffix(String name) {
        if (name.endsWith(".tmp") || name.endsWith(".bak")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private static void addLegacyMasks(Map<String, byte[]> desired) {
        boolean hasLegacy = desired.containsKey(LEGACY_JSON) || desired.containsKey(LEGACY_SERIALIZED);
        // The marker is a durable logical-state boundary: every imported
        // snapshot without an explicit legacy source must continue to mask
        // project-root fallback files, including after later canonical saves.
        if (!hasLegacy) {
            desired.putIfAbsent(SAVED_GAMES_RESET, "reset\n".getBytes(StandardCharsets.UTF_8));
        }
        if (!desired.containsKey(RECORDS)) {
            desired.putIfAbsent(RECORDS_RESET, "reset\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writeCandidate(Path directory, Map<String, byte[]> entries,
            boolean injectFailures) throws IOException {
        Files.createDirectories(directory);
        for (Map.Entry<String, byte[]> entry : new TreeMap<>(entries).entrySet()) {
            if (!isArchiveName(entry.getKey())) {
                throw invalid("Candidate contains an unmanaged entry.");
            }
            if (injectFailures && failureInjector != null) {
                failureInjector.beforeWrite(entry.getKey());
            }
            Path target = directory.resolve(entry.getKey()).normalize();
            if (!target.getParent().equals(directory)) {
                throw invalid("Candidate entry escaped its directory.");
            }
            Files.write(target, entry.getValue(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }
    }

    private static void snapshotManaged(Path target, Path previous) throws IOException {
        if (!Files.isDirectory(target)) {
            return;
        }
        try (var stream = Files.list(target)) {
            for (Path path : stream.collect(Collectors.toList())) {
                if (!isManagedName(path.getFileName().toString())) {
                    continue;
                }
                if (!Files.isRegularFile(path)) {
                    throw new IOException("Managed path is not a regular file: " + path);
                }
                Files.copy(path, previous.resolve(path.getFileName().toString()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void replaceManaged(Path target, Map<String, byte[]> desired) throws IOException {
        Files.createDirectories(target);
        try (var stream = Files.list(target)) {
            for (Path path : stream.collect(Collectors.toList())) {
                if (isManagedName(path.getFileName().toString())) {
                    if (!Files.isRegularFile(path)) {
                        throw new IOException("Managed path is not a regular file: " + path);
                    }
                    if (failureInjector != null) {
                        failureInjector.beforeDelete(path.getFileName().toString());
                    }
                    Files.delete(path);
                }
            }
        }
        for (Map.Entry<String, byte[]> entry : new TreeMap<>(desired).entrySet()) {
            writeRestoreFile(target, entry.getKey(), entry.getValue(), true);
        }
    }

    private static void restorePrevious(Path target, Path previous) throws IOException {
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }
        try (var stream = Files.list(target)) {
            for (Path path : stream.collect(Collectors.toList())) {
                if (isManagedName(path.getFileName().toString())) {
                    if (Files.isRegularFile(path)) {
                        if (failureInjector != null) {
                            failureInjector.beforeRollback(path.getFileName().toString());
                        }
                        Files.delete(path);
                    } else {
                        throw new IOException("Could not remove managed path during rollback: " + path);
                    }
                }
            }
        }
        if (!Files.isDirectory(previous)) {
            return;
        }
        try (var stream = Files.list(previous)) {
            for (Path path : stream.collect(Collectors.toList())) {
                if (failureInjector != null) {
                    failureInjector.beforeRollback(path.getFileName().toString());
                }
                writeRestoreFile(target, path.getFileName().toString(),
                        Files.readAllBytes(path), false);
            }
        }
    }

    private static void writeRestoreFile(Path directory, String name, byte[] bytes,
            boolean injectFailures) throws IOException {
        if (!isManagedName(name) || bytes == null || bytes.length > MAX_ENTRY_BYTES) {
            throw new IOException("Invalid managed restore file: " + name);
        }
        if (injectFailures && failureInjector != null) {
            failureInjector.beforeWrite(name);
        }
        Path target = directory.resolve(name).normalize();
        Path temporary = directory.resolve(name + ".restore-tmp-" + UUID.randomUUID()).normalize();
        Files.write(temporary, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try {
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException invalid(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }

    private static boolean isEmptyDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (var stream = Files.list(directory)) {
            return stream.findAny().isEmpty();
        }
    }

    private static void deleteTreeOrThrow(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exception)
                    throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteTreeQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try {
            deleteTreeOrThrow(directory);
        } catch (IOException ignored) {
            // Temporary validation directories are not user recovery material.
        }
    }

    private static final class ArchiveDocument {
        private final long createdAt;
        private final Map<String, byte[]> entries;

        private ArchiveDocument(long createdAt, Map<String, byte[]> entries) {
            this.createdAt = createdAt;
            this.entries = entries;
        }
    }

    /** Small strict JSON parser used because the Desktop package has no JSON dependency. */
    private static final class JsonParser {
        private final String text;
        private int index;

        private JsonParser(String text) {
            this.text = text;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw new IllegalArgumentException("Trailing JSON data");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw new IllegalArgumentException("Missing JSON value");
            }
            return switch (text.charAt(index)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            while (true) {
                skipWhitespace();
                if (index >= text.length() || text.charAt(index) != '"') {
                    throw new IllegalArgumentException("Object key must be a string");
                }
                String key = parseString();
                if (result.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate JSON key");
                }
                skipWhitespace();
                require(':');
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (consume('}')) {
                    return result;
                }
                require(',');
            }
        }

        private List<Object> parseArray() {
            index++;
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return result;
                }
                require(',');
            }
        }

        private String parseString() {
            require('"');
            StringBuilder result = new StringBuilder();
            while (index < text.length()) {
                char value = text.charAt(index++);
                if (value == '"') {
                    return result.toString();
                }
                if (value < 0x20) {
                    throw new IllegalArgumentException("Control character in JSON string");
                }
                if (value != '\\') {
                    result.append(value);
                    continue;
                }
                if (index >= text.length()) {
                    throw new IllegalArgumentException("Unclosed JSON escape");
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(parseUnicode());
                    default -> throw new IllegalArgumentException("Invalid JSON escape");
                }
            }
            throw new IllegalArgumentException("Unclosed JSON string");
        }

        private char parseUnicode() {
            if (index + 4 > text.length()) {
                throw new IllegalArgumentException("Short unicode escape");
            }
            int value = Integer.parseInt(text.substring(index, index + 4), 16);
            index += 4;
            return (char) value;
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw new IllegalArgumentException("Invalid JSON literal");
            }
            index += literal.length();
            return value;
        }

        private Long parseNumber() {
            int start = index;
            if (index < text.length() && text.charAt(index) == '-') {
                index++;
            }
            int digits = index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (digits == index) {
                throw new IllegalArgumentException("Invalid JSON number");
            }
            if (index < text.length() && (text.charAt(index) == '.'
                    || text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                throw new IllegalArgumentException("Only integer JSON numbers are accepted");
            }
            try {
                return Long.valueOf(text.substring(start, index));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("JSON number is out of range", exception);
            }
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            if (index < text.length() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void require(char expected) {
            if (!consume(expected)) {
                throw new IllegalArgumentException("Expected JSON character: " + expected);
            }
        }
    }
}
