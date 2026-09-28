package com.klotski.core;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Locale;
import java.util.zip.CRC32;

/**
 * Deterministic, platform-neutral encoder and decoder for Relay v1 text codes.
 *
 * <p>The canonical form is {@code SLD-R1-<Base64URL payload>-<CRC32>}. The
 * payload is binary: size byte, explicit stable difficulty wire ID byte,
 * unsigned two-byte big-endian target, and row-major tile bytes. CRC32 detects
 * common transcription corruption; it is not authentication or proof of
 * challenge origin.</p>
 */
public final class RelayCodeCodec {
    /** Canonical prefix for Relay v1 codes. */
    public static final String PREFIX = "SLD-R1-";
    private static final String VERSION_FAMILY_PREFIX = "SLD-R";
    private static final int MAX_INPUT_LENGTH = 256;
    private static final int HEADER_LENGTH = 4;
    private static final int RELAXED_WIRE_ID = 1;
    private static final int CLASSIC_WIRE_ID = 2;
    private static final int CHALLENGE_WIRE_ID = 3;

    private RelayCodeCodec() {
    }

    /**
     * Encodes one Relay specification into its unique canonical text form.
     *
     * @param spec challenge specification to encode
     * @return canonical Relay v1 code
     * @throws NullPointerException when spec is null
     */
    public static String encode(RelayChallengeSpec spec) {
        if (spec == null) {
            throw new NullPointerException("spec");
        }
        byte[] payload = payloadFor(spec);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
                + "-" + checksum(payload);
    }

    /**
     * Decodes and validates a canonical Relay code.
     *
     * @param input code, optionally surrounded by whitespace
     * @return validated immutable Relay specification
     * @throws RelayCodeException when syntax, version, checksum, or payload is invalid
     */
    public static RelayChallengeSpec decode(String input) {
        if (input == null || input.trim().isEmpty() || input.length() > MAX_INPUT_LENGTH) {
            throw new RelayCodeException(Error.INVALID_FORMAT);
        }
        String code = input.trim();
        if (!code.startsWith(PREFIX)) {
            if (code.startsWith(VERSION_FAMILY_PREFIX)) {
                throw new RelayCodeException(Error.UNSUPPORTED_VERSION);
            }
            throw new RelayCodeException(Error.INVALID_FORMAT);
        }
        String[] segments = code.split("-", -1);
        if (segments.length != 4 || !"SLD".equals(segments[0])
                || !"R1".equals(segments[1])
                || !segments[2].matches("[A-Za-z0-9_-]+")
                || !segments[3].matches("[0-9A-F]{8}")) {
            throw new RelayCodeException(Error.INVALID_FORMAT);
        }

        final byte[] payload;
        try {
            payload = Base64.getUrlDecoder().decode(segments[2]);
        } catch (IllegalArgumentException exception) {
            throw new RelayCodeException(Error.INVALID_FORMAT);
        }
        if (!Base64.getUrlEncoder().withoutPadding().encodeToString(payload).equals(segments[2])) {
            throw new RelayCodeException(Error.INVALID_FORMAT);
        }
        if (!checksum(payload).equals(segments[3])) {
            throw new RelayCodeException(Error.CHECKSUM_MISMATCH);
        }
        if (payload.length < HEADER_LENGTH) {
            throw new RelayCodeException(Error.INVALID_PAYLOAD);
        }

        int size = payload[0] & 0xff;
        if (size < RelayChallengeSpec.MIN_SIZE || size > RelayChallengeSpec.MAX_SIZE) {
            throw new RelayCodeException(Error.INVALID_SIZE);
        }
        if (payload.length != HEADER_LENGTH + size * size) {
            throw new RelayCodeException(Error.INVALID_PAYLOAD);
        }
        PuzzleDifficulty difficulty = difficultyForWireId(payload[1] & 0xff);
        if (difficulty == null) {
            throw new RelayCodeException(Error.UNKNOWN_DIFFICULTY);
        }
        int targetMoves = ((payload[2] & 0xff) << 8) | (payload[3] & 0xff);
        if (targetMoves < RelayChallengeSpec.MIN_TARGET_MOVES
                || targetMoves > RelayChallengeSpec.MAX_TARGET_MOVES) {
            throw new RelayCodeException(Error.INVALID_TARGET);
        }
        int[][] grid = new int[size][size];
        for (int index = 0; index < size * size; index++) {
            grid[index / size][index % size] = payload[HEADER_LENGTH + index] & 0xff;
        }
        final RelayChallengeSpec spec;
        try {
            spec = new RelayChallengeSpec(size, difficulty, grid, targetMoves);
        } catch (IllegalArgumentException exception) {
            throw new RelayCodeException(Error.INVALID_BOARD);
        }
        if (!encode(spec).equals(code)) {
            throw new RelayCodeException(Error.INVALID_FORMAT);
        }
        return spec;
    }

    private static byte[] payloadFor(RelayChallengeSpec spec) {
        ByteBuffer payload = ByteBuffer.allocate(HEADER_LENGTH + spec.getSize() * spec.getSize());
        payload.put((byte) spec.getSize());
        payload.put((byte) wireIdFor(spec.getDifficulty()));
        payload.putShort((short) spec.getTargetMoves());
        for (int[] row : spec.getInitialGridCopy()) {
            for (int tile : row) {
                payload.put((byte) tile);
            }
        }
        return payload.array();
    }

    private static int wireIdFor(PuzzleDifficulty difficulty) {
        return switch (difficulty) {
            case RELAXED -> RELAXED_WIRE_ID;
            case CLASSIC -> CLASSIC_WIRE_ID;
            case CHALLENGE -> CHALLENGE_WIRE_ID;
        };
    }

    private static PuzzleDifficulty difficultyForWireId(int wireId) {
        return switch (wireId) {
            case RELAXED_WIRE_ID -> PuzzleDifficulty.RELAXED;
            case CLASSIC_WIRE_ID -> PuzzleDifficulty.CLASSIC;
            case CHALLENGE_WIRE_ID -> PuzzleDifficulty.CHALLENGE;
            default -> null;
        };
    }

    private static String checksum(byte[] payload) {
        CRC32 crc = new CRC32();
        crc.update(payload);
        return String.format(Locale.ROOT, "%08X", crc.getValue());
    }

    /** Stable failure classes used by localized platform UI. */
    public enum Error {
        /** The input does not use the canonical Relay code syntax. */
        INVALID_FORMAT,
        /** The input uses a Relay wire-format version this build does not support. */
        UNSUPPORTED_VERSION,
        /** The payload checksum does not match the supplied checksum. */
        CHECKSUM_MISMATCH,
        /** The encoded payload length or structure is invalid. */
        INVALID_PAYLOAD,
        /** The board size is outside the supported range. */
        INVALID_SIZE,
        /** The difficulty identifier is not recognized. */
        UNKNOWN_DIFFICULTY,
        /** The target move count is outside the supported range. */
        INVALID_TARGET,
        /** The board is not a complete solvable puzzle permutation. */
        INVALID_BOARD
    }

    /** Typed decoder failure without platform-specific error text. */
    public static final class RelayCodeException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        /** Stable failure category for localized presentation. */
        private final Error error;

        private RelayCodeException(Error error) {
            super(error.name());
            this.error = error;
        }

        /**
         * Returns the stable failure class for localized UI.
         *
         * @return stable failure class for localized UI
         */
        public Error getError() {
            return error;
        }
    }
}
