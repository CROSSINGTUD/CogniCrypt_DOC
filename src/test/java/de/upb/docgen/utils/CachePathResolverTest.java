package de.upb.docgen.utils;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class CachePathResolverTest {

    @Test
    public void resolvesLlmCacheDirUnderReportPath() {
        Path expected = Paths.get("/tmp/doc-output")
                .toAbsolutePath()
                .normalize()
                .resolve("resources")
                .resolve("llm_cache");

        assertEquals(expected, CachePathResolver.resolveLlmCacheDir("/tmp/doc-output"));
    }

    @Test
    public void resolvesCodeCacheDirUnderReportPath() {
        Path expected = Paths.get("/tmp/doc-output")
                .toAbsolutePath()
                .normalize()
                .resolve("resources")
                .resolve("code_cache");

        assertEquals(expected, CachePathResolver.resolveCodeCacheDir("/tmp/doc-output"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullReportPath() {
        CachePathResolver.resolveReportDirectory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankReportPath() {
        CachePathResolver.resolveReportDirectory("   ");
    }
}
