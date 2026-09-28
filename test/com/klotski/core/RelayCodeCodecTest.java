package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.zip.CRC32;

import org.junit.jupiter.api.Test;

class RelayCodeCodecTest {
    private static final String FIXTURE_3X3 =
            "SLD-R1-AwIABQECAwQFBgcACA-A1947A39";
    private static final String FIXTURE_4X4 =
            "SLD-R1-BAEACQECAwQFBgcICQoLDA0OAA8-180BAECF";
    private static final String FIXTURE_5X5 =
            "SLD-R1-BQMADQECAwQFBgcICQoLDA0ODxAREhMUFRYXABg-40B4F94E";

    @Test
    void frozenFixturesDecodeToExactCrossPlatformContracts() {
        assertFixture(FIXTURE_3X3, PuzzleDifficulty.CLASSIC, 5,
                new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 0, 8}});
        assertFixture(FIXTURE_4X4, PuzzleDifficulty.RELAXED, 9,
                new int[][] {{1, 2, 3, 4}, {5, 6, 7, 8},
                        {9, 10, 11, 12}, {13, 14, 0, 15}});
        assertFixture(FIXTURE_5X5, PuzzleDifficulty.CHALLENGE, 13,
                new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10},
                        {11, 12, 13, 14, 15}, {16, 17, 18, 19, 20},
                        {21, 22, 23, 0, 24}});
    }

    @Test
    void encodingIsDeterministicAndUsesExplicitDifficultyWireIds() {
        assertEquals(FIXTURE_3X3, RelayCodeCodec.encode(spec(
                3, PuzzleDifficulty.CLASSIC, 5,
                new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 0, 8}})));
        assertEquals(FIXTURE_4X4, RelayCodeCodec.encode(spec(
                4, PuzzleDifficulty.RELAXED, 9,
                new int[][] {{1, 2, 3, 4}, {5, 6, 7, 8},
                        {9, 10, 11, 12}, {13, 14, 0, 15}})));
        assertEquals(FIXTURE_5X5, RelayCodeCodec.encode(spec(
                5, PuzzleDifficulty.CHALLENGE, 13,
                new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10},
                        {11, 12, 13, 14, 15}, {16, 17, 18, 19, 20},
                        {21, 22, 23, 0, 24}})));
    }

    @Test
    void decoderTrimsSurroundingWhitespaceButRejectsUnknownVersions() {
        assertEquals(RelayCodeCodec.decode(FIXTURE_3X3),
                RelayCodeCodec.decode(" \r\n" + FIXTURE_3X3 + "\t "));
        RelayCodeCodec.RelayCodeException error = assertThrows(
                RelayCodeCodec.RelayCodeException.class,
                () -> RelayCodeCodec.decode(FIXTURE_3X3.replace("SLD-R1-", "SLD-R2-")));
        assertEquals(RelayCodeCodec.Error.UNSUPPORTED_VERSION, error.getError());
    }

    @Test
    void rejectsBadChecksumAndTruncatedOrTrailingPayloads() {
        assertError(FIXTURE_3X3.substring(0, FIXTURE_3X3.length() - 1) + "0",
                RelayCodeCodec.Error.CHECKSUM_MISMATCH);
        assertThrows(RelayCodeCodec.RelayCodeException.class,
                () -> RelayCodeCodec.decode("SLD-R1-AwIABQECAwQFBgcAC-A1947A39"));
        assertThrows(RelayCodeCodec.RelayCodeException.class,
                () -> RelayCodeCodec.decode(FIXTURE_3X3 + "-extra"));
        assertError(code(payload(3, 2, 5,
                        1, 2, 3, 4, 5, 6, 7, 0, 8, 99)),
                RelayCodeCodec.Error.INVALID_PAYLOAD);
    }

    @Test
    void rejectsInvalidSizesDifficultyTargetsAndBoardsAfterChecksumValidation() {
        assertError(code(payload(2, 2, 5, 1, 2, 3, 0)), RelayCodeCodec.Error.INVALID_SIZE);
        assertError(code(payload(3, 99, 5, 1, 2, 3, 4, 5, 6, 7, 0, 8)),
                RelayCodeCodec.Error.UNKNOWN_DIFFICULTY);
        assertError(code(payload(3, 2, 0, 1, 2, 3, 4, 5, 6, 7, 0, 8)),
                RelayCodeCodec.Error.INVALID_TARGET);
        assertError(code(payload(3, 2, 10_000, 1, 2, 3, 4, 5, 6, 7, 0, 8)),
                RelayCodeCodec.Error.INVALID_TARGET);
        assertError(code(payload(3, 2, 5, 1, 2, 3, 4, 5, 6, 7, 7, 8)),
                RelayCodeCodec.Error.INVALID_BOARD);
        assertError(code(payload(3, 2, 5, 1, 2, 3, 4, 5, 6, 7, 0, 9)),
                RelayCodeCodec.Error.INVALID_BOARD);
        assertError(code(payload(3, 2, 5, 1, 2, 3, 4, 5, 6, 8, 0, 7)),
                RelayCodeCodec.Error.INVALID_BOARD);
    }

    @Test
    void canonicalReencodingMatchesFrozenCodeAndSpecHasStructuralEquality() {
        RelayChallengeSpec decoded = RelayCodeCodec.decode(FIXTURE_5X5);
        assertEquals(FIXTURE_5X5, RelayCodeCodec.encode(decoded));
        assertEquals(decoded, RelayCodeCodec.decode(FIXTURE_5X5));
        assertEquals(decoded.hashCode(), RelayCodeCodec.decode(FIXTURE_5X5).hashCode());
        assertNotEquals(decoded, RelayCodeCodec.decode(FIXTURE_4X4));
    }

    @Test
    void validPermutationMustAlsoBeSolvable() {
        assertTrue(PuzzleSolvability.isSolvable(
                new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 0}}, 3));
        assertFalse(PuzzleSolvability.isSolvable(
                new int[][] {{1, 2, 3}, {4, 5, 6}, {8, 7, 0}}, 3));
        assertTrue(PuzzleSolvability.isSolvable(
                new int[][] {{1, 2, 3, 4}, {5, 6, 7, 8},
                        {9, 10, 11, 12}, {13, 14, 0, 15}}, 4));
        assertFalse(PuzzleSolvability.isSolvable(
                new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 7, 0}}, 3));
    }

    @Test
    void specificationCopiesGridAndReplaysExactInitialBoard() {
        int[][] source = {{1, 2, 3}, {4, 5, 6}, {7, 0, 8}};
        RelayChallengeSpec spec = spec(3, PuzzleDifficulty.CLASSIC, 5, source);
        source[2][1] = 8;
        int[][] copy = spec.getInitialGridCopy();
        copy[2][1] = 8;
        assertEquals(0, spec.getInitialGridCopy()[2][1]);
        assertEquals(spec.getInitialGridCopy().length, spec.createGame().getSize());
        assertTrue(java.util.Arrays.deepEquals(spec.getInitialGridCopy(),
                spec.createGame().getInitialGridCopy()));
    }

    @Test
    void targetUsesPlayerActionCountAndWholeLineSlideRemainsOneMove() {
        RelayChallengeSpec spec = RelayCodeCodec.decode(FIXTURE_3X3);
        assertTrue(spec.isTargetMet(4));
        assertTrue(spec.isTargetMet(5));
        assertFalse(spec.isTargetMet(6));
        assertFalse(spec.isTargetMet(-1));

        GameModel model = new GameModel(3);
        model.loadState(saveData(new int[][] {{1, 2, 3}, {4, 5, 0}, {7, 8, 6}}));
        assertTrue(model.slideLineTo(1, 0));
        assertEquals(1, model.getMoveCount());
        assertTrue(RelayChallengeSpec.MIN_TARGET_MOVES <= RelayChallengeSpec.MAX_TARGET_MOVES);
    }

    @Test
    void specificationRejectsUnsupportedBoardAndTargetBounds() {
        assertThrows(IllegalArgumentException.class, () -> spec(2,
                PuzzleDifficulty.CLASSIC, 1, new int[][] {{1, 0}, {2, 3}}));
        assertThrows(IllegalArgumentException.class, () -> spec(3,
                PuzzleDifficulty.CLASSIC, 0, new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 0, 8}}));
        assertThrows(IllegalArgumentException.class, () -> spec(3,
                PuzzleDifficulty.CLASSIC, 10_000,
                new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 0, 8}}));
    }

    private static void assertFixture(String code, PuzzleDifficulty difficulty,
            int target, int[][] grid) {
        RelayChallengeSpec actual = RelayCodeCodec.decode(code);
        assertEquals(1, actual.getFormatVersion());
        assertEquals(grid.length, actual.getSize());
        assertEquals(difficulty, actual.getDifficulty());
        assertEquals(target, actual.getTargetMoves());
        assertTrue(java.util.Arrays.deepEquals(grid, actual.getInitialGridCopy()));
    }

    private static RelayChallengeSpec spec(int size, PuzzleDifficulty difficulty,
            int target, int[][] grid) {
        return new RelayChallengeSpec(size, difficulty, grid, target);
    }

    private static void assertError(String code, RelayCodeCodec.Error expected) {
        RelayCodeCodec.RelayCodeException error = assertThrows(
                RelayCodeCodec.RelayCodeException.class, () -> RelayCodeCodec.decode(code));
        assertEquals(expected, error.getError());
    }

    private static String code(byte[] payload) {
        CRC32 crc = new CRC32();
        crc.update(payload);
        return RelayCodeCodec.PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload) + "-" + String.format("%08X", crc.getValue());
    }

    private static byte[] payload(int size, int difficultyId, int target, int... tiles) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + tiles.length);
        buffer.put((byte) size);
        buffer.put((byte) difficultyId);
        buffer.putShort((short) target);
        for (int tile : tiles) {
            buffer.put((byte) tile);
        }
        return buffer.array();
    }

    private static SaveManager.SaveData saveData(int[][] grid) {
        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = grid.length;
        data.grid = grid;
        data.initialGrid = grid;
        data.moveCount = 0;
        data.elapsedTime = 0;
        data.updatedAt = 1;
        data.active = true;
        data.difficulty = PuzzleDifficulty.CLASSIC;
        return data;
    }
}
