package kr.zenon.rpg.common.seed;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class JsonSeedLoader<T> implements SeedLoader<T> {
    private final String name;
    private final Path sourcePath;
    private final Function<String, List<T>> parser;

    public JsonSeedLoader(String name, Path sourcePath, Function<String, List<T>> parser) {
        this.name = Objects.requireNonNull(name, "name");
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Result<List<T>> load() {
        if (!Files.exists(sourcePath)) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Seed file does not exist: " + sourcePath.toAbsolutePath()
            );
        }

        try {
            String json = Files.readString(sourcePath, StandardCharsets.UTF_8);
            List<T> values = parser.apply(json);
            return Result.success(values == null ? List.of() : List.copyOf(values));
        } catch (IOException exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to read seed file: " + sourcePath.toAbsolutePath(),
                    exception
            );
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to parse seed file: " + sourcePath.toAbsolutePath(),
                    exception
            );
        }
    }
}
