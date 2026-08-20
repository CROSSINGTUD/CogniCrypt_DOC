package de.upb.docgen.llm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import de.upb.docgen.DocSettings;
import de.upb.docgen.utils.CachePathResolver;
import de.upb.docgen.utils.Utils;

/**
 * Author: Roshan Samantaray
 **/

public class LLMService {

    // Resolve project-relative paths for the LLM sidecar scripts and caches.
    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path VENV_PY_UNIX = PROJECT_ROOT.resolve(Paths.get(".venv", "bin", "python"));
    private static final Path VENV_PY_WIN  = PROJECT_ROOT.resolve(Paths.get(".venv", "Scripts", "python.exe"));

    /**
     * Detect whether the current OS is Windows for Python fallback selection.
     */
    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    /**
     * Resolve the Python executable, preferring the project venv if present.
     */
    private static String resolvePythonExecutable() {
        if (Files.isExecutable(VENV_PY_UNIX)) {
            return VENV_PY_UNIX.toString();
        }
        if (Files.isExecutable(VENV_PY_WIN)) {
            return VENV_PY_WIN.toString();
        }
        return isWindows() ? "python" : "python3";
    }

    /**
     * Build the javac classpath used to validate generated examples.
     *
     * <p>Empty by default, so examples are checked against the JDK alone. They are meant to
     * use only the JCA/JCE surface, and a reader of the documentation will not have the
     * generator's own dependency tree (Xtext, Soot, Guava, ...) on their classpath —
     * compiling against it made the gate accept code the reader cannot build, and could
     * never catch an accidental dependency on one of those libraries.
     *
     * <p>Set {@code CRYSLDOC_COMPILE_CLASSPATH} to supply entries for rules that genuinely
     * need a non-JDK type on the classpath — {@code javax.servlet.http.Cookie} is the one
     * bundled rule in that position. Set {@code CRYSLDOC_COMPILE_USE_RUNTIME_CLASSPATH=1}
     * to restore the previous behaviour wholesale.
     */
    private static String buildCompileClasspath() {
        LinkedHashSet<String> entries = new LinkedHashSet<>();

        String extra = System.getenv("CRYSLDOC_COMPILE_CLASSPATH");
        if (extra != null && !extra.isBlank()) {
            for (String part : extra.split(Pattern.quote(File.pathSeparator))) {
                String trimmed = part == null ? "" : part.trim();
                if (!trimmed.isEmpty()) {
                    entries.add(trimmed);
                }
            }
        }

        boolean useRuntimeClasspath = "1".equals(System.getenv("CRYSLDOC_COMPILE_USE_RUNTIME_CLASSPATH"));
        if (useRuntimeClasspath) {
            String cp = System.getProperty("java.class.path", "");
            if (cp != null && !cp.isBlank()) {
                for (String part : cp.split(Pattern.quote(File.pathSeparator))) {
                    String trimmed = part == null ? "" : part.trim();
                    if (!trimmed.isEmpty()) {
                        entries.add(trimmed);
                    }
                }
            }

            Path targetClasses = PROJECT_ROOT.resolve(Paths.get("target", "classes"));
            if (Files.exists(targetClasses)) {
                entries.add(targetClasses.toAbsolutePath().toString());
            }

            Path libDir = PROJECT_ROOT.resolve(Paths.get("target", "lib"));
            if (Files.isDirectory(libDir)) {
                try {
                    try (var stream = Files.list(libDir)) {
                        stream
                            .filter(p -> p.getFileName().toString().endsWith(".jar"))
                            .sorted()
                            .forEach(p -> entries.add(p.toAbsolutePath().toString()));
                    }
                } catch (IOException ignored) {
                    // Best-effort classpath enrichment; keep existing entries.
                }
            }
        }

        return String.join(File.pathSeparator, entries);
    }

    /**
     * Resolve javac --release value from the running JVM, defaulting to 21.
     */
    private static String resolveJavaRelease() {
        String spec = System.getProperty("java.specification.version", "21").trim();
        if (spec.contains(".")) {
            String[] parts = spec.split("\\.");
            return parts[parts.length - 1];
        }
        return spec.isEmpty() ? "21" : spec;
    }

    /**
     * Resolve javac from the running JVM's java.home when possible.
     */
    private static String resolveJavacBinary() {
        Path javaHome = Paths.get(System.getProperty("java.home", ""));
        Path javac = javaHome.resolve(Paths.get("bin", isWindows() ? "javac.exe" : "javac"));
        if (Files.isExecutable(javac)) {
            return javac.toAbsolutePath().toString();
        }
        return "javac";
    }

    // Wall-clock ceilings for the Python sidecar, in seconds. Explanations are a single
    // completion; a secure example can run a compile-and-repair loop of up to
    // CRYSLDOC_MAX_REPAIRS rounds, each with its own completion plus a javac invocation,
    // so it needs a far larger budget. Override via -D or the environment when using a
    // slow model or a raised repair limit.
    // 480s: comfortably above the Python sidecar's own worst-case retry budget
    // ((1 + LLM_MAX_RETRIES) * LLM_TIMEOUT_SECONDS, see llm/.env), so a slow-but-working
    // gateway response has room to finish rather than being cut off by this ceiling first.
    private static final long EXPLANATION_TIMEOUT_SECONDS =
            resolveTimeoutSeconds("CRYSLDOC_LLM_EXPLANATION_TIMEOUT_SECONDS", 480);
    private static final long EXAMPLE_TIMEOUT_SECONDS =
            resolveTimeoutSeconds("CRYSLDOC_LLM_EXAMPLE_TIMEOUT_SECONDS", 900);

    private static long resolveTimeoutSeconds(String name, long fallbackSeconds) {
        String raw = System.getProperty(name);
        if (raw == null || raw.trim().isEmpty()) {
            raw = System.getenv(name);
        }
        if (raw == null || raw.trim().isEmpty()) {
            return fallbackSeconds;
        }
        try {
            long parsed = Long.parseLong(raw.trim());
            return parsed > 0 ? parsed : fallbackSeconds;
        } catch (NumberFormatException e) {
            System.err.println("[WARN] Ignoring non-numeric " + name + "=" + raw + "; using " + fallbackSeconds + "s.");
            return fallbackSeconds;
        }
    }

    /**
     * Run the sidecar to completion under a wall-clock limit and return its merged output.
     *
     * <p>The child's stdout is drained on a separate thread. Draining inline (the previous
     * shape) blocks in {@code readLine()} until the child closes the stream, which only
     * happens when it exits - so {@code waitFor(timeout)} was only ever reached after the
     * process had already finished, and a stalled HTTPS call hung the whole run forever.
     * Reading concurrently is also what keeps a chatty child from deadlocking on a full
     * pipe buffer.
     *
     * @throws IOException on timeout (after {@code destroyForcibly()}), non-zero exit, or interruption
     */
    private static String runSidecar(ProcessBuilder pb, long timeoutSeconds, String description) throws IOException {
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        StringBuilder diagnostics = new StringBuilder();

        Thread drain = drainStream(process.getInputStream(), output, "llm-sidecar-stdout");
        Thread drainErr = drainStream(process.getErrorStream(), diagnostics, "llm-sidecar-stderr");

        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                drain.join(5_000);
                drainErr.join(5_000);
                throw new IOException("LLM python process timed out after " + timeoutSeconds
                        + "s for " + description + ". Partial output: " + snapshot(output)
                        + " stderr: " + snapshot(diagnostics));
            }
            // The child has exited; let the drainers finish the tails of both streams.
            drain.join(10_000);
            drainErr.join(10_000);

            int exit = process.exitValue();
            if (exit != 0) {
                throw new IOException("LLM python process exited with code " + exit
                        + " for " + description + ". Output: " + snapshot(output)
                        + " stderr: " + snapshot(diagnostics));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted while waiting for LLM python process for " + description, e);
        }

        return snapshot(output);
    }

    /** Drain one stream on a daemon thread into the given buffer. */
    private static Thread drainStream(java.io.InputStream stream, StringBuilder sink, String name) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (sink) {
                        sink.append(line).append('\n');
                    }
                }
            } catch (IOException e) {
                // Stream closes when the process is destroyed on timeout; whatever was
                // read before that is still reported by the caller.
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static String snapshot(StringBuilder output) {
        synchronized (output) {
            return output.toString();
        }
    }

    /**
     * Generate multilingual explanations via the Python LLM sidecar with caching.
     */
    public static Map<String, String> getLLMExplanation(Map<String, String> cryslData, List<String> LANGUAGES, String backend) throws IOException {
        Gson gson = new Gson();
        // Choose backend-specific Python script.
        String pythonScriptPath;
        String backendNormalized = backend == null ? "" : backend.trim().toLowerCase(Locale.ROOT);
        if (backendNormalized.equals("openai")) {
            pythonScriptPath = "llm/llm_writer.py";
        } else if (backendNormalized.equals("gateway")) {
            pythonScriptPath = "llm/llm_writer_gateway.py";
        } else {
            throw new IOException("Unsupported LLM backend: " + backend);
        }
        Map<String, String> result = new HashMap<>();

        // Prepare temp/sanitized folders under llm/.
        Path base = PROJECT_ROOT.resolve("llm");
        Path tempFolder = base.resolve("temp_rules");
        Path sanitizedFolder = base.resolve("sanitized_rules");
        Files.createDirectories(base);
        Files.createDirectories(tempFolder);
        Files.createDirectories(sanitizedFolder);

        // Cache folder for explanation outputs under reportPath/resources.
        Path cacheFolder;
        try {
            cacheFolder = CachePathResolver.resolveLlmCacheDir(DocSettings.getInstance().getReportDirectory());
        } catch (IllegalArgumentException e) {
            throw new IOException("Unable to resolve LLM cache directory from --reportPath.", e);
        }
        Files.createDirectories(cacheFolder);

        String pythonPath = resolvePythonExecutable();

        for (String lang: LANGUAGES) {
            // Add language to the payload and create a sanitized JSON input.
            cryslData.put("explanationLanguage", lang);

            String className = cryslData.get("className");
            String classNameSafe = className.replaceAll("[^a-zA-Z0-9.\\-]", "_");
            String json = gson.toJson(cryslData);
            // Written unconditionally: this payload is derived from the composed sentences,
            // so it changes whenever a rule or any sentence generator changes. Skipping the
            // write when the file merely exists meant the model kept being prompted with a
            // previous run's text, with nothing anywhere to invalidate it. Both operations
            // are local and negligible next to the API call that follows.
            Path tempIn = tempFolder.resolve("temp_rule_" + classNameSafe + "_" + lang + ".json");
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(tempIn), StandardCharsets.UTF_8)) {
                writer.write(json);
            }
            Path sanitizedOut = sanitizedFolder.resolve("sanitized_rule_" + classNameSafe + "_" + lang + ".json");
            Utils.sanitizeRuleFileSecure(tempIn, sanitizedOut, base);

            // Use cached explanation if available - but only a REAL one. Testing existence
            // alone meant a "disabled by flag" / "no explanation generated" placeholder left
            // by an earlier run permanently suppressed the LLM call, so the placeholder could
            // never be replaced. This mirrors the retry check the example path already had.
            Path cacheFile = cacheFolder.resolve(classNameSafe + "_" + lang + ".txt");
            if (Files.exists(cacheFile)) {
                String cached = Files.readString(cacheFile, StandardCharsets.UTF_8);
                if (!Utils.isRetryableExplanationPlaceholder(cached)) {
                    result.put(lang, cached.trim());
                    continue; // skip Python process
                }
                System.out.println("Cached explanation for " + className + " / " + lang
                        + " is a placeholder; regenerating.");
            }

            // Spawn the Python process for the selected backend.
            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath,
                    pythonScriptPath,
                    className,
                    lang
            );
            File projectRoot = PROJECT_ROOT.toFile();
            pb.directory(projectRoot);
            // NOT redirectErrorStream: stdout is the content, stderr is diagnostics.
            // Merging them put the sidecar's "[WARN] Missing file: /home/..." lines
            // straight into the published explanation text.
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            // Caught per-language rather than left to propagate: LANGUAGES is processed in a
            // fixed order, so one slow/flaky call (timeout, gateway 5xx, ...) used to abort the
            // whole method and silently skip every language after it in this pass - e.g. German
            // timing out meant French was never even attempted, not just German. The caller's
            // fallback (DocumentGeneratorMain, "No explanation generated for X (lang).") already
            // treats a missing map entry as a retryable placeholder, so skipping just this one
            // language and continuing is enough to make it self-heal on a later run.
            String outStr;
            try {
                outStr = runSidecar(pb, EXPLANATION_TIMEOUT_SECONDS, className + " / " + lang).trim();
            } catch (IOException e) {
                System.err.println("Explanation generation failed for " + className + " / " + lang
                        + ": " + e.getMessage());
                continue;
            }
            result.put(lang, outStr);
            // cache write (optional)
            try (OutputStreamWriter cw = new OutputStreamWriter(Files.newOutputStream(cacheFile), StandardCharsets.UTF_8)) {
                cw.write(outStr);
            } catch (IOException ignored) {
                // non-fatal: caching failure shouldn't break main flow
            }
        }
        return result;
    }

    /**
     * Generate a secure or insecure example via the Python sidecar.
     */
    public static String getLLMExample(Map<String, String> cryslData, String type, String backend) throws IOException {
        // Mark the request type (secure/insecure) and write a temp JSON input.
        cryslData.put("exampleType", type);
        Gson gson = new Gson();
        String json = gson.toJson(cryslData);

        String backendNormalized = backend == null ? "" : backend.trim().toLowerCase(Locale.ROOT);
        if (!backendNormalized.equals("openai") && !backendNormalized.equals("gateway")) {
            throw new IOException("Unsupported LLM backend for examples: " + backend);
        }

        // Keyed by class name as well as type: a type-only name would collide as soon
        // as rule processing stops being strictly sequential.
        String exampleClassName = cryslData.get("className");
        String exampleClassNameSafe = exampleClassName == null
                ? "unknown"
                : exampleClassName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        Path tempFile = PROJECT_ROOT.resolve("llm")
                .resolve("temp_example_" + exampleClassNameSafe + "_" + type + ".json");
        Files.createDirectories(tempFile.getParent());
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(tempFile), StandardCharsets.UTF_8)) {
            writer.write(json);
        }

        // Call the Python generator for the example type.
        String pythonScriptPath = "llm/llm_code_writer_" + type + ".py";
        String pythonPath = resolvePythonExecutable();
        String rulesDir = Paths.get("src", "main", "resources", "CrySLRules")
                .toAbsolutePath()
                .toString();

        List<String> command = new ArrayList<>(List.of(
                pythonPath,
                pythonScriptPath,
                tempFile.toString(),
                "--backend", backendNormalized,
                "--rules-dir", rulesDir
        ));
        if ("secure".equalsIgnoreCase(type)) {
            command.add("--compile-classpath");
            command.add(buildCompileClasspath());
            command.add("--java-release");
            command.add(resolveJavaRelease());
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(PROJECT_ROOT.toFile());
        // See above: stderr is kept separate so it cannot contaminate generated code.
        if ("secure".equalsIgnoreCase(type)) {
            pb.environment().put("JAVAC_BIN", resolveJavacBinary());
        }

        // Enforce a time limit for example generation.
        String description = type + " example for " + (exampleClassName == null ? "unknown class" : exampleClassName);
        return runSidecar(pb, EXAMPLE_TIMEOUT_SECONDS, description).trim();
    }

}
