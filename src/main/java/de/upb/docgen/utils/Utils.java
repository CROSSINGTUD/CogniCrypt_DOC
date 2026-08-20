package de.upb.docgen.utils;

import com.google.gson.*;
import crypto.interfaces.ICrySLPredicateParameter;
import crypto.rules.CrySLCondPredicate;
import crypto.rules.CrySLPredicate;
import de.upb.docgen.DocSettings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    // Input size guard for sanitizer.
    private static final int MAX_BYTES = 2_000_000; // 2MB guard
    private static final Pattern TAG_PATTERN = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern CONTROL_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");
    // Pattern to capture tooltip spans: outer label + inner tooltip text
    private static final Pattern TOOLTIP_PATTERN = Pattern.compile(
            "(?is)<span\\s+class=\"tooltip\">(.*?)<span\\s+class=\"tooltiptext\">(.*?)</span>\\s*</span>"
    );

    // Shared JSON serializer for deterministic sanitized outputs.
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    /**
     * Secure sanitizer entry.
     * @param input path to noisy JSON
     * @param output path to cleaned JSON
     * @param baseDir base directory to confine IO (e.g. Paths.get("llm"))
     * @return cleaned JSON string
     * @throws IOException on IO errors
     */
    public static String sanitizeRuleFileSecure(Path input, Path output, Path baseDir) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(baseDir, "baseDir");
        // Ensure reads/writes stay under the provided base directory.
        enforceUnderBase(input, baseDir);
        enforceUnderBase(output, baseDir);

        byte[] rawBytes = Files.readAllBytes(input);
        if (rawBytes.length > MAX_BYTES) {
            throw new IOException("Input exceeds limit: " + rawBytes.length);
        }
        String raw = new String(rawBytes, StandardCharsets.UTF_8);

        // Pre-clean
        String cleaned = preClean(raw);

        JsonObject source = parseLenient(cleaned);

        // Build normalized object
        JsonObject out = new JsonObject();
        out.addProperty("className", cleanScalar(optString(source, "className")));
        out.add("objects", toJsonArray(splitList(optString(source, "objects"))));
        out.add("ensures", toJsonArray(splitSentences(cleanScalar(optString(source, "ensures")))));
        out.add("constraints", toJsonArray(splitSentences(cleanScalar(optString(source, "constraints")))));
        out.add("requires", toJsonArray(autoListOrSentences(cleanScalar(optString(source, "requires")))));
        out.addProperty("order", cleanScalar(optString(source, "order")));
        out.addProperty("events", cleanScalar(optString(source, "events")));
        out.add("forbidden", toJsonArray(autoListOrSentences(cleanScalar(optString(source, "forbidden")))));
        // New: preserve explanationLanguage if present
        out.addProperty("explanationLanguage", cleanScalar(optString(source, "explanationLanguage")));
        // New: dependencies list (optional)
        out.add("dependency", toJsonArray(splitList(optString(source, "dependency"))));

        // De-duplicate arrays
        dedupeArray(out, "objects");
        dedupeArray(out, "ensures");
        dedupeArray(out, "constraints");
        dedupeArray(out, "requires");
        dedupeArray(out, "forbidden");
        dedupeArray(out, "dependency");

        String json = GSON.toJson(out);
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.writeString(output, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return json;
    }

    /**
     * Ensure a path stays within a base directory (prevents path traversal).
     */
    private static void enforceUnderBase(Path path, Path base) throws IOException {
        Path normBase = base.toAbsolutePath().normalize();
        Path norm = path.toAbsolutePath().normalize();
        if (!norm.startsWith(normBase)) {
            throw new IOException("Path escapes base: " + path);
        }
    }

    /**
     * Remove control characters and null bytes before JSON parsing.
     */
    private static String preClean(String s) {
        // Remove null bytes & control chars
        s = s.replace("\u0000", "");
        s = CONTROL_PATTERN.matcher(s).replaceAll("");
        return s;
    }

    /**
     * Parse JSON leniently, falling back to a minimal object if parsing fails.
     */
    private static JsonObject parseLenient(String s) {
        try {
            JsonElement el = JsonParser.parseString(s);
            if (el.isJsonObject()) return el.getAsJsonObject();
        } catch (Exception ignored) {}
        // Fallback: attempt to wrap as single field if raw text
        JsonObject obj = new JsonObject();
        obj.addProperty("ensures", s);
        return obj;
    }

    /**
     * Safely read a string value from a JSON object.
     */
    private static String optString(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return "";
        if (o.get(key).isJsonPrimitive()) return o.get(key).getAsString();
        return o.get(key).toString();
    }

    /**
     * Strip HTML tags while preserving spacing between tokens.
     */
    private static String stripHtml(String in) {
        if (in == null || in.isEmpty()) return "";
        return TAG_PATTERN.matcher(in).replaceAll(" ");
    }

    /**
     * Convert common HTML entities to their literal characters.
     */
    private static String unescapeBasicEntities(String s) {
        if (s == null) return "";
        return s.replace("&lt;","<")
                .replace("&gt;",">")
                .replace("&amp;","&")
                .replace("&quot;","\"")
                .replace("&#39;","'")
                .replace("&apos;","'");
    }

    /**
     * Normalize whitespace to a single-space separator and trim ends.
     */
    private static String normalizeWs(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    /**
     * Expand tooltip markup into readable text before stripping HTML.
     */
    private static String expandTooltips(String s) {
        if (s == null || s.isEmpty()) return "";
        Matcher m = TOOLTIP_PATTERN.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String label = normalizeWs(stripHtml(m.group(1)));
            String tip = normalizeWs(stripHtml(m.group(2)));
            String replacement = label + " {" + tip + "}";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Clean a scalar field by expanding tooltips, stripping HTML, and normalizing whitespace.
     */
    private static String cleanScalar(String s) {
        s = expandTooltips(s); // convert tooltips before stripping remaining HTML
        return normalizeWs(unescapeBasicEntities(stripHtml(s)));
    }

    /**
     * Split comma/semicolon-delimited values into a list of clean tokens.
     */
    private static List<String> splitList(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isBlank()) return out;
        for (String part : s.split("[,;]")) {
            String c = normalizeWs(part);
            if (!c.isEmpty()) out.add(c);
        }
        return out;
    }

    /**
     * Split sentences conservatively on periods for prompt-friendly lists.
     */
    private static List<String> splitSentences(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isBlank()) return out;
        // Conservative sentence split: period followed by space or end
        String[] parts = s.split("(?<=[^.])\\.(?=\\s|$)");
        for (String p : parts) {
            String c = normalizeWs(p);
            if (!c.isEmpty()) {
                if (!c.endsWith(".")) c += ".";
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Heuristic: choose list split or sentence split based on punctuation.
     */
    private static List<String> autoListOrSentences(String s) {
        if (s == null || s.isBlank()) return Collections.emptyList();
        int commas = s.length() - s.replace(",","").length();
        int periods = s.length() - s.replace(".","").length();
        if (commas > 0 && periods <= commas) return splitList(s);
        return splitSentences(s);
    }

    /**
     * Convert a string list into a JSON array, skipping blanks.
     */
    private static JsonArray toJsonArray(List<String> items) {
        JsonArray arr = new JsonArray();
        for (String i : items) {
            if (i != null && !i.isBlank()) arr.add(i);
        }
        return arr;
    }

    /**
     * De-duplicate values in a JSON array field while preserving order.
     */
    private static void dedupeArray(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonArray()) return;
        JsonArray arr = o.getAsJsonArray(key);
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (JsonElement e : arr) if (e.isJsonPrimitive()) seen.add(e.getAsString());
        JsonArray rebuilt = new JsonArray();
        for (String v : seen) rebuilt.add(v);
        o.add(key, rebuilt);
    }




	/**
	 * Extracts predicate dependencies by class name (deduplicated).
	 */
	public static Map<String, Set<String>> toOnlyClassNames(Map<String, List<Map<String, List<String>>>> mappedPredicates) {
		Map<String, Set<String>> onlyClassNamesMap = new HashMap<>();
		for (String classname : mappedPredicates.keySet()) {
			Set<String> setToRemoveDuplicates = new HashSet<>();
			for (Map<String, List<String>> stringListMap : mappedPredicates.get(classname)) {
				List<String> temp = new ArrayList<>();
				for (String predicate : stringListMap.keySet()) {
					List<String> dependingClassNames = new ArrayList<>();
					for (String classnameToAdd : stringListMap.get(predicate)) {
						dependingClassNames.add(classnameToAdd);
					}
					temp.addAll(dependingClassNames);
				}
				setToRemoveDuplicates.addAll(temp);
			}
			onlyClassNamesMap.putIfAbsent(classname,setToRemoveDuplicates);
		}
		return onlyClassNamesMap;
	}



	/**
	 * Construct a map from predicates to the classes that provide them.
	 */
	public static Map<String, List<Map<String, List<String>>>> mapPredicates(Map<String, List<CrySLPredicate>> dependingMap, Map<String, List<CrySLPredicate>> keyMap) {
		Map<String, List<Map<String,List<String>>>> dependingPredicatesMap = new HashMap<>();
		for (String className : keyMap.keySet()) {
			List<Map<String,List<String>>> predicateList = new ArrayList<>();
			for (CrySLPredicate predicate : keyMap.get(className)) {
				if (predicate.isNegated()) continue;
				String predicateName = predicate.getPredName();
				List<ICrySLPredicateParameter> predicateParameters = predicate.getParameters();
				Map<String,List<String>> keyToDependingMap = new HashMap<>();
				for (String dependingClassName: dependingMap.keySet() ) {
					Set<String> dependingClasses = new LinkedHashSet<>();

					for (CrySLPredicate dependingPredicates :  dependingMap.get(dependingClassName)) {
						if (dependingPredicates.getPredName().equals(predicateName) && !(dependingPredicates.isNegated() && !(dependingPredicates instanceof CrySLCondPredicate))) {
							if (dependingPredicates.getParameters().size() == predicateParameters.size()) {
								dependingClasses.add(dependingClassName);

							}
						}
					}

					if (dependingClasses.size() != 0 ) {
						if (keyToDependingMap.containsKey(predicateName)) {
							keyToDependingMap.get(predicateName).addAll(dependingClasses);
						} else {
							List<String> dc = new ArrayList<>(dependingClasses);
							keyToDependingMap.put(predicateName, dc);
						}
					}
				}
				predicateList.add(keyToDependingMap);
			}
			dependingPredicatesMap.put(className,predicateList);
		}
		return dependingPredicatesMap;
	}


    /**
     * Read a template and return its contents as a char array.
     */
	public static char[] getTemplatesText(String templateName) throws IOException {
        return getTemplatesTextString(templateName).toCharArray();
    }


    /**
     * Read a template text from an override path or bundled resources.
     */
	public static String getTemplatesTextString(String templateName) throws IOException {
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("templateName must not be null/blank");
        }

        String langTemplatesPath = DocSettings.getInstance().getLangTemplatesPath();

        // Disk override when --langTemplatesPath is provided
        if (langTemplatesPath != null && !langTemplatesPath.trim().isEmpty()) {
            File file = new File(langTemplatesPath, templateName);
            if (!file.isFile()) {
                throw new FileNotFoundException("Template not found at --langTemplatesPath: " + file.getAbsolutePath());
            }
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }

        // Bundled fallback: resources/Templates/<templateName>
        String resourcePath = "Templates/" + templateName;
        try (InputStream in = Utils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Bundled template not found on classpath: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }


    /**
     * Condense a code-generation failure into one short, path-free line fit for publication.
     *
     * <p>The raw exception carries the sidecar's entire stdout - a Python traceback, the
     * shaped CrySL contract dump, and absolute paths from the build machine. That belongs in
     * the codegen failure report, not embedded in a page where a code example should be.
     */
    public static String summarizeGenerationFailure(String raw) {
        if (raw == null || raw.isBlank()) {
            return "generation failed";
        }
        String flat = stripBuildPaths(raw.replaceAll("\\s+", " ").trim());

        Matcher repairs = Pattern.compile("Still not compilable after (\\d+) repair attempts").matcher(flat);
        if (repairs.find()) {
            String summary = "did not compile after " + repairs.group(1) + " repair attempts";
            String firstError = firstCompilerError(flat);
            return firstError == null ? summary : summary + " - first error: " + firstError;
        }

        Matcher runtimeError = Pattern.compile("RuntimeError: (.+)").matcher(flat);
        if (runtimeError.find()) {
            return capForDisplay(runtimeError.group(1));
        }

        Matcher exitCode = Pattern.compile("exited with code (\\d+)").matcher(flat);
        if (exitCode.find()) {
            return "the generator exited with code " + exitCode.group(1);
        }

        return capForDisplay(flat);
    }

    private static String firstCompilerError(String flat) {
        Matcher m = Pattern.compile("error: (.+?)(?= \\S*\\.java:| Note: | \\d+ errors?\\b|$)").matcher(flat);
        return m.find() ? capForDisplay(m.group(1)) : null;
    }

    /** Remove absolute filesystem paths so generated pages do not expose the build machine. */
    private static String stripBuildPaths(String text) {
        String cleaned = text.replaceAll("(?:/[^\\s:]+)+/([A-Za-z0-9_$.-]+\\.java)", "$1");
        return cleaned.replaceAll("(?:/home|/Users|/tmp|/var/folders)/\\S*", "<path>");
    }

    private static String capForDisplay(String text) {
        String trimmed = text.trim();
        return trimmed.length() <= 160 ? trimmed : trimmed.substring(0, 157).trim() + "...";
    }

    /**
     * True if a cached LLM explanation is a placeholder rather than a real answer.
     *
     * <p>Shared by both cache layers on purpose. {@code LLMService} uses it to decide
     * whether to skip the Python call, and {@code DocumentGeneratorMain} uses it to decide
     * whether to reuse the file. Those two decisions must agree: if only the second checked,
     * a placeholder would still short-circuit the LLM call and then be rewritten with itself.
     */
    public static boolean isRetryableExplanationPlaceholder(String content) {
        if (content == null) {
            return true;
        }
        String txt = content.trim();
        if (txt.isEmpty()) {
            return true;
        }
        return txt.equals("LLM explanations disabled by flag.")
                || (txt.startsWith("No explanation generated for ") && txt.endsWith(")."));
    }

    /**
     * Normalize file paths for FreeMarker template loading.
     */
	public static String pathForTemplates(String path) {
		return path.replaceAll("\\\\","/");
	}

    /**
     * Extract a classpath resource into a temporary file.
     */
	public static File extract(String filePath) {
        try {
            if (filePath == null || filePath.isBlank()) {
                throw new IllegalArgumentException("filePath must not be null/blank");
            }

            // Normalize resource path (ClassLoader expects NO leading '/')
            String resourcePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;

            InputStream classIS = Utils.class.getClassLoader().getResourceAsStream(resourcePath);
            if (classIS == null) {
                throw new FileNotFoundException("Resource not found on classpath: " + resourcePath);
            }

            // Create a SAFE temp file name (no '/', prefix >= 3 chars)
            String baseName = resourcePath;
            int lastSlash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
            if (lastSlash >= 0) baseName = baseName.substring(lastSlash + 1);

            String prefix = baseName;
            String suffix = null;

            int dot = baseName.lastIndexOf('.');
            if (dot > 0 && dot < baseName.length() - 1) {
                prefix = baseName.substring(0, dot);
                suffix = baseName.substring(dot); // includes ".ftl", ".properties", etc.
            }

            prefix = prefix.replaceAll("[^A-Za-z0-9._-]", "_");
            if (prefix.length() < 3) prefix = (prefix + "___").substring(0, 3);

            File f = File.createTempFile(prefix + "-", suffix);
            f.deleteOnExit();

            try (classIS; OutputStream resourceOS = new FileOutputStream(f)) {
                classIS.transferTo(resourceOS);
            }

            return f;

        } catch (Exception e) {
            // Keep your old behavior of not changing the signature,
            // but fail fast (returning null causes NPE later)
            throw new IllegalStateException("Error extracting resource from jar: " + filePath + " -> " + e.getMessage(), e);
        }
    }

    /**
     * Topologically order dependencies from leaf providers to the root consumer.
     */
    public static List<String> leafToRootOrderTopo(String start, Map<String, Set<String>> adj) {
        // 1) Collect reachable nodes from `start` (following adj: class -> providers)
        Deque<String> stack = new ArrayDeque<>();
        Set<String> reachable = new HashSet<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String u = stack.pop();
            if (!reachable.add(u)) continue;
            for (String v : adj.getOrDefault(u, Collections.emptySet())) {
                if (!v.equals(u)) stack.push(v); // ignore self-edge
            }
        }

        // 2) Build reversed graph: provider -> consumers (only within reachable set)
        Map<String, Set<String>> provToConsumers = new HashMap<>();
        Map<String, Integer> indeg = new HashMap<>();  // indegree = number of providers (deps)
        for (String u : reachable) indeg.put(u, 0);

        for (String u : reachable) {
            for (String p : adj.getOrDefault(u, Collections.emptySet())) {
                if (p.equals(u) || !reachable.contains(p)) continue;
                provToConsumers.computeIfAbsent(p, k -> new HashSet<>()).add(u);
                indeg.put(u, indeg.get(u) + 1);   // u depends on p
            }
        }

        // 3) Kahn’s queue seeded with leaves (indegree 0 => no deps)
        //    Use PriorityQueue for deterministic alphabetical order
        Queue<String> q = new PriorityQueue<>();
        for (Map.Entry<String,Integer> e : indeg.entrySet())
            if (e.getValue() == 0) q.add(e.getKey());

        List<String> order = new ArrayList<>(reachable.size());
        while (!q.isEmpty()) {
            String leaf = q.remove();
            order.add(leaf);  // providers before consumers (leaf -> root)

            for (String consumer : provToConsumers.getOrDefault(leaf, Collections.emptySet())) {
                int d = indeg.computeIfPresent(consumer, (k,v) -> v-1);
                if (d == 0) q.add(consumer);
            }
        }

        // 4) Cycle check (rare, but possible in big specs)
        if (order.size() != reachable.size()) {
            // nodes with indegree > 0 are in cycles; log and append them deterministically
            List<String> stuck = new ArrayList<>();
            for (Map.Entry<String,Integer> e : indeg.entrySet())
                if (e.getValue() > 0) stuck.add(e.getKey());
            Collections.sort(stuck);
            System.err.println("WARNING: cycle(s) detected among: " + stuck);
            // Break ties by appending; generation will still proceed.
            order.addAll(stuck);
        }

        return order; // leaf → … → root (start will be last if the graph is acyclic)
    }
}
