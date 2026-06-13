package kr.zenon.rpg.common.seed;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class CsvSeedLoader<T> implements SeedLoader<T> {
    private final String name;
    private final Path sourcePath;
    private final Function<CsvRow, T> mapper;

    public CsvSeedLoader(String name, Path sourcePath, Function<CsvRow, T> mapper) {
        this.name = Objects.requireNonNull(name, "name");
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
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
            List<String> lines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
            List<String> contentLines = lines.stream()
                    .filter(line -> line != null && !line.isBlank())
                    .filter(line -> !line.trim().startsWith("#"))
                    .toList();

            if (contentLines.isEmpty()) {
                return Result.success(List.of());
            }

            List<String> headers = parseCsvLine(contentLines.get(0)).stream()
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .toList();
            if (headers.isEmpty()) {
                return Result.failure(
                        ErrorCode.SEED_LOAD_FAILED,
                        "CSV header is empty: " + sourcePath.toAbsolutePath()
                );
            }

            List<T> rows = new ArrayList<>();
            int logicalLineNo = 1;
            for (int index = 1; index < contentLines.size(); index++) {
                logicalLineNo++;
                String line = contentLines.get(index);
                List<String> values = parseCsvLine(line);
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String key = headers.get(i);
                    String value = i < values.size() ? values.get(i).trim() : "";
                    rowMap.put(key, value);
                }
                CsvRow row = new CsvRow(logicalLineNo, rowMap);
                rows.add(mapper.apply(row));
            }

            return Result.success(List.copyOf(rows));
        } catch (IOException exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to read CSV seed file: " + sourcePath.toAbsolutePath(),
                    exception
            );
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.SEED_LOAD_FAILED,
                    "Failed to parse CSV seed file: " + sourcePath.toAbsolutePath(),
                    exception
            );
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }

        StringBuilder token = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    token.append('"');
                    i++;
                    continue;
                }
                inQuotes = !inQuotes;
                continue;
            }
            if (ch == ',' && !inQuotes) {
                values.add(token.toString());
                token.setLength(0);
                continue;
            }
            token.append(ch);
        }
        values.add(token.toString());
        return values;
    }
}
