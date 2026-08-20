package de.upb.docgen.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves reportPath-scoped cache directories used by LLM features.
 */
public final class CachePathResolver {

    private CachePathResolver() {
    }

    /**
     * Resolve and normalize reportPath from a raw CLI value.
     */
    public static Path resolveReportDirectory(String reportDirectory) {
        if (reportDirectory == null || reportDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("--reportPath is required to resolve LLM cache directories.");
        }
        return Paths.get(reportDirectory).toAbsolutePath().normalize();
    }

    /**
     * Resolve <reportPath>/resources/llm_cache.
     */
    public static Path resolveLlmCacheDir(String reportDirectory) {
        return resolveReportDirectory(reportDirectory).resolve("resources").resolve("llm_cache");
    }

    /**
     * Resolve <reportPath>/resources/code_cache.
     */
    public static Path resolveCodeCacheDir(String reportDirectory) {
        return resolveReportDirectory(reportDirectory).resolve("resources").resolve("code_cache");
    }
}
