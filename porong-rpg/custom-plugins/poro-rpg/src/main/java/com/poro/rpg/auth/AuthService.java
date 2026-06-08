package com.poro.rpg.auth;

import com.poro.rpg.common.time.TimeProvider;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.Optional;

/**
 * 디스코드 인증 코어 (DL-132 — 인게임 발급 → 봇 검증).
 *
 * <ul>
 *   <li>{@link #issueCode} — 인게임 {@code /인증}이 호출. 로그인된 MC uuid에 바인드된 1회용 코드 발급.
 *       uuid당 활성 코드 1개, 짧은 TTL.</li>
 *   <li>{@link #verify} — 봇 {@code POST /auth/verify}가 호출. 코드 소모 + {@code discord_id ↔ {uuid,name}} 확정.</li>
 * </ul>
 *
 * <p>코드 = 혼동 문자(I,L,O,U,0,1) 제외 30자 알파벳에서 8자리(≈39비트 엔트로피), {@link SecureRandom} 기반.
 */
public final class AuthService {
    /** 혼동되는 글자 제외(I/L/O/U, 0/1). 30자 알파벳. */
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;
    private static final int INSERT_RETRY = 5;

    private final AuthRepository repository;
    private final TimeProvider timeProvider;
    private final SecureRandom random;
    private final long ttlMillis;

    public AuthService(AuthRepository repository, TimeProvider timeProvider, long ttlMillis) {
        this.repository   = Objects.requireNonNull(repository, "repository");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must be > 0");
        }
        this.ttlMillis = ttlMillis;
        this.random    = new SecureRandom();
    }

    /**
     * 인게임 발급. uuid당 활성 코드 1개(기존 미사용 코드 제거 후 재발급).
     *
     * @return 발급된 코드, 저장 실패 시 비어 있음
     */
    public Optional<String> issueCode(String playerUuid, String playerName) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerName, "playerName");

        repository.deletePendingByUuid(playerUuid);

        long now = timeProvider.nowInstant().toEpochMilli();
        long expiresAt = now + ttlMillis;
        for (int attempt = 0; attempt < INSERT_RETRY; attempt++) {
            String code = generateCode();
            if (repository.insertPending(code, playerUuid, playerName, now, expiresAt)) {
                return Optional.of(code);
            }
            // 코드 충돌(PK) 등 — 재시도.
        }
        return Optional.empty();
    }

    /**
     * 봇 검증. 코드 소모 + 링크 확정.
     *
     * @return 성공 시 확정 {uuid, name}; 코드 없음·만료·실패 시 비어 있음(→ 봇에 404).
     */
    public Optional<AuthRepository.LinkedIdentity> verify(String code, String discordId) {
        if (code == null || discordId == null) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        long now = timeProvider.nowInstant().toEpochMilli();
        return repository.consumeAndLink(normalized, discordId, now);
    }

    public long ttlMillis() {
        return ttlMillis;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
