package com.poro.rpg.common.registry;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.seed.SeedLoader;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class RegistryBootstrapper {
    private final DomainLogger logger;

    public RegistryBootstrapper(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public Result<Void> bootstrap(List<BootstrapTask<?>> tasks) {
        for (BootstrapTask<?> task : tasks) {
            Result<Integer> taskResult = task.run();
            if (taskResult.isFailure()) {
                String message = "Failed to bootstrap registry task [" + task.name() + "]: " + taskResult.message();
                logger.error(message, taskResult.cause());
                return Result.failure(ErrorCode.REGISTRY_BOOTSTRAP_FAILED, message, taskResult.cause());
            }
            logger.info("Bootstrapped task [" + task.name() + "] with " + taskResult.value() + " entries.");
        }
        return Result.success();
    }

    public static <T> BootstrapTask<T> task(String name, SeedLoader<T> loader, Consumer<List<T>> registrar) {
        return new BootstrapTask<>(name, loader, registrar);
    }

    public static final class BootstrapTask<T> {
        private final String name;
        private final SeedLoader<T> loader;
        private final Consumer<List<T>> registrar;

        private BootstrapTask(String name, SeedLoader<T> loader, Consumer<List<T>> registrar) {
            this.name = Objects.requireNonNull(name, "name");
            this.loader = Objects.requireNonNull(loader, "loader");
            this.registrar = Objects.requireNonNull(registrar, "registrar");
        }

        public String name() {
            return name;
        }

        public Result<Integer> run() {
            Result<List<T>> loaded = loader.load();
            if (loaded.isFailure()) {
                return Result.failure(loaded.errorCode(), loaded.message(), loaded.cause());
            }

            List<T> values = loaded.value();
            try {
                registrar.accept(values);
                return Result.success(values.size());
            } catch (Exception exception) {
                return Result.failure(
                        ErrorCode.REGISTRY_BOOTSTRAP_FAILED,
                        "Failed to register loaded seed values for task: " + name,
                        exception
                );
            }
        }
    }
}
