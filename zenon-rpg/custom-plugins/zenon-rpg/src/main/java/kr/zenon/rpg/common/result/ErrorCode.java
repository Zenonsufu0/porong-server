package kr.zenon.rpg.common.result;

public enum ErrorCode {
    UNKNOWN("common.unknown", "Unknown error"),
    INVALID_ARGUMENT("common.invalid_argument", "Invalid argument"),
    SEED_LOAD_FAILED("common.seed_load_failed", "Failed to load seed data"),
    REGISTRY_BOOTSTRAP_FAILED("common.registry_bootstrap_failed", "Failed to bootstrap registry"),
    MASTER_SEED_VALIDATION_FAILED("common.master_seed_validation_failed", "Master seed validation failed"),
    DB_CONNECTION_FAILED("common.db_connection_failed", "Failed to connect to database"),
    DB_TRANSACTION_FAILED("common.db_transaction_failed", "Database transaction failed"),
    MIGRATION_NOT_IMPLEMENTED("common.migration_not_implemented", "Migration entry point is not implemented");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
