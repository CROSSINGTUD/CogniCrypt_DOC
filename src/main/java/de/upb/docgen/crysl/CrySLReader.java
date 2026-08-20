package de.upb.docgen.crysl;

import crypto.cryslhandler.CrySLModelReader;
import crypto.exceptions.CryptoAnalysisException;
import crypto.rules.CrySLRule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.security.CodeSource;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;


/**
 * @author Ritika Singh
 */

public class CrySLReader {
	// ---------------------------------------------------------
// Sven features (PR #11/#12): default-from-JAR support
// Implements your GitHub review comments:
// - exact matching (no broad "contains")
// - safe extraction (no Utils.extract / no writing into cwd)
// ---------------------------------------------------------

// Resource roots within the JAR/classpath.
private static final String CRYSL_RULES_DIR = "CrySLRules";
private static final String FTL_TEMPLATES_DIR = "FTLTemplates";
private static final String SYMBOL_PROPERTIES_RESOURCE = "Templates/symbol.properties";

// Reuse one temp dir for extracted resources.
private static Path cachedTempDir = null;

	/**
	 * Read ALL CrySL rules bundled inside the JAR under /CrySLRules.
	 * If running from IDE (resources on disk), read from filesystem folder instead.
	 */
		public static List<CrySLRule> readRulesFromJar() throws IOException {
			CrySLModelReader reader = new CrySLModelReader();
			List<File> ruleFiles = new ArrayList<>();
			Map<File, String> displayNames = new HashMap<>();

			// IDE mode: resources available as real files
			URL dirUrl = CrySLReader.class.getClassLoader().getResource(CRYSL_RULES_DIR);
			if (dirUrl != null && "file".equalsIgnoreCase(dirUrl.getProtocol())) {
				try {
					File dir = Paths.get(dirUrl.toURI()).toFile();
					File[] files = dir.listFiles((d, name) -> name != null && name.endsWith(".crysl"));
					if (files != null) {
						for (File f : files) {
							ruleFiles.add(f);
							displayNames.put(f, f.getName());
						}
					}
					// Deterministic parse order for stable logs/debugging.
					ruleFiles.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
					return parseRulesBatchWithFallback(reader, ruleFiles, displayNames, false);
				} catch (URISyntaxException e) {
					throw new IOException("Failed to resolve /CrySLRules resource directory", e);
				}
			}

			// JAR mode: enumerate entries
			try (JarFile jar = openOwningJar(dirUrl)) {
				Enumeration<JarEntry> entries = jar.entries();
				String prefix = CRYSL_RULES_DIR + "/";
				Path rulesTmpDir = getOrCreateTempDir().resolve(CRYSL_RULES_DIR);
				Files.createDirectories(rulesTmpDir);
				// Registered before the files it will contain, so reverse-registration-order
				// deleteOnExit deletes them first and this (now-empty) directory second -
				// otherwise the directory itself was never scheduled for deletion and was
				// left behind after every clean, successful, packaged-JAR run.
				rulesTmpDir.toFile().deleteOnExit();

				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();

					if (!entry.isDirectory() && name.startsWith(prefix) && name.endsWith(".crysl")) {
						// Keep original simple rule filenames in a dedicated rules temp folder.
						// This mirrors filesystem loading semantics more closely than flattened names.
						String fileName = Paths.get(name).getFileName().toString();
						Path out = rulesTmpDir.resolve(fileName);
						extractIfStale(jar, entry, out);
						File extracted = out.toFile();
						ruleFiles.add(extracted);
						displayNames.put(extracted, name);
					}
				}
			}

			// Deterministic parse order by original resource path (not temp filename).
			ruleFiles.sort((a, b) ->
					displayNames.getOrDefault(a, a.getName()).compareToIgnoreCase(displayNames.getOrDefault(b, b.getName()))
			);
			return parseRulesBatchWithFallback(reader, ruleFiles, displayNames, true);
		}

	private static List<CrySLRule> parseRulesBatchWithFallback(
			CrySLModelReader reader,
			List<File> ruleFiles,
			Map<File, String> displayNames,
			boolean jarStyle
	) {
		if (ruleFiles.isEmpty()) {
			return new ArrayList<>();
		}

		// Preferred path: batch parse in one ResourceSet to maximize cross-rule resolution.
		try {
			return reader.readRulesFromFiles(ruleFiles);
		} catch (CryptoAnalysisException batchError) {
			// Fallback path: preserve resilient best-effort behavior from per-file parsing.
			List<CrySLRule> out = new ArrayList<>();
			for (File f : ruleFiles) {
				try {
					out.add(reader.readRule(f));
				} catch (CryptoAnalysisException e) {
					String display = displayNames.getOrDefault(f, f.getName());
					if (jarStyle) {
						System.err.println("Error processing rule: " + display + " - " + e.getMessage());
					} else {
						System.err.println("Error processing rule file: " + display + " - " + e.getMessage());
					}
				}
			}
			return out;
		}
	}

	/**
	 * Extract a SINGLE CrySL rule from resources:
	 * exact match: /CrySLRules/<ruleName>.crysl
	 *
	 * @param ruleName simple class name, e.g. "Cipher"
	 */
	public static File readRuleFromJarFile(String ruleName) throws IOException {
		if (ruleName == null || ruleName.trim().isEmpty()) {
			throw new IllegalArgumentException("ruleName cannot be null/empty");
		}
		ruleName = ruleName.trim();

		String resourcePath = CRYSL_RULES_DIR + "/" + ruleName + ".crysl";

		// IDE mode: file resource
		URL fileUrl = CrySLReader.class.getClassLoader().getResource(resourcePath);
		if (fileUrl != null && "file".equalsIgnoreCase(fileUrl.getProtocol())) {
			try {
				return Paths.get(fileUrl.toURI()).toFile();
			} catch (URISyntaxException e) {
				throw new IOException("Failed to resolve resource URI: " + fileUrl, e);
			}
		}

		// JAR mode: exact entry match (no broad "contains")
		try (JarFile jar = openOwningJar(fileUrl)) {
			JarEntry entry = jar.getJarEntry(resourcePath);
			if (entry == null) {
				// fallback: restricted endsWith match inside CrySLRules/
				entry = findJarEntryEndingWith(jar, "/" + ruleName + ".crysl", CRYSL_RULES_DIR + "/");
			}
			if (entry == null) return null;

			return extractJarEntryToTempFile(jar, entry);
		}
	}

	/**
	 * Extract Templates/symbol.properties (exact match).
	 */
	public static File readSymbolPropertiesFromJar() throws IOException {
		String resourcePath = SYMBOL_PROPERTIES_RESOURCE;

		URL fileUrl = CrySLReader.class.getClassLoader().getResource(resourcePath);
		if (fileUrl != null && "file".equalsIgnoreCase(fileUrl.getProtocol())) {
			try {
				return Paths.get(fileUrl.toURI()).toFile();
			} catch (URISyntaxException e) {
				throw new IOException("Failed to resolve resource URI: " + fileUrl, e);
			}
		}

		try (JarFile jar = openOwningJar(fileUrl)) {
			JarEntry entry = jar.getJarEntry(resourcePath);
			if (entry == null) {
				// fallback: restricted endsWith match inside Templates/
				entry = findJarEntryEndingWith(jar, "/symbol.properties", "Templates/");
			}
			if (entry == null) return null;

			return extractJarEntryToTempFile(jar, entry);
		}
	}

	/**
	 * Extract a bundled FreeMarker template from:
	 * exact match: /FTLTemplates/<ftlName>
	 */
	public static File readFTLFromJar(String ftlName) throws IOException {
		if (ftlName == null || ftlName.trim().isEmpty()) {
			throw new IllegalArgumentException("ftlName cannot be null/empty");
		}
		ftlName = ftlName.trim();

		String resourcePath = FTL_TEMPLATES_DIR + "/" + ftlName;

		// IDE mode: file resource
		URL fileUrl = CrySLReader.class.getClassLoader().getResource(resourcePath);
		if (fileUrl != null && "file".equalsIgnoreCase(fileUrl.getProtocol())) {
			try {
				return Paths.get(fileUrl.toURI()).toFile();
			} catch (URISyntaxException e) {
				throw new IOException("Failed to resolve resource URI: " + fileUrl, e);
			}
		}

		try (JarFile jar = openOwningJar(fileUrl)) {
			JarEntry entry = jar.getJarEntry(resourcePath);
			if (entry == null) {
				// fallback: restricted endsWith match inside FTLTemplates/
				entry = findJarEntryEndingWith(jar, "/" + ftlName, FTL_TEMPLATES_DIR + "/");
			}
			if (entry == null) return null;

			return extractJarEntryToTempFile(jar, entry);
		}
	}

	// ----------------- helpers -----------------

	private static JarFile openOwningJar(URL anyResourceUrl) throws IOException {
		// Best-case: jar:<...>!/path
		if (anyResourceUrl != null && "jar".equalsIgnoreCase(anyResourceUrl.getProtocol())) {
			JarURLConnection conn = (JarURLConnection) anyResourceUrl.openConnection();
			return conn.getJarFile();
		}

		// Fallback: resolve the jar from CodeSource (works when running packaged)
		CodeSource codeSource = CrySLReader.class.getProtectionDomain().getCodeSource();
		if (codeSource == null) {
			throw new IOException("Cannot locate CodeSource for CrySLReader");
		}
		try {
			Path p = Paths.get(codeSource.getLocation().toURI());
			File jarFile = p.toFile();
			if (!jarFile.isFile()) {
				throw new IOException("CodeSource is not a JAR file: " + jarFile);
			}
			return new JarFile(jarFile);
		} catch (URISyntaxException e) {
			throw new IOException("Failed to resolve CodeSource URI", e);
		}
	}

	private static JarEntry findJarEntryEndingWith(JarFile jar, String endsWith, String requiredPrefix) {
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry e = entries.nextElement();
			String name = e.getName();
			if (!e.isDirectory() && name.startsWith(requiredPrefix) && name.endsWith(endsWith)) {
				return e;
			}
		}
		return null;
	}

	private static File extractJarEntryToTempFile(JarFile jar, JarEntry entry) throws IOException {
		String resourcePath = entry.getName();
		Path tmpDir = getOrCreateTempDir();
		String safeName = resourcePath.replace('/', '_').replace('\\', '_');
		Path out = tmpDir.resolve(safeName);
		extractIfStale(jar, entry, out);
		return out.toFile();
	}

	/**
	 * Extract a JAR entry to {@code out}, but only when {@code out} doesn't already hold
	 * an up-to-date copy - reusing an existing file is gated on matching size/CRC against
	 * the JAR entry, not merely on the path existing, so a rebuilt JAR with edited
	 * resource content can't silently keep serving a stale extraction from a prior run.
	 * The write goes through a sibling temp file and an atomic move so a concurrent
	 * reader on the same path never observes a partially-written file.
	 */
	private static void extractIfStale(JarFile jar, JarEntry entry, Path out) throws IOException {
		if (Files.exists(out) && !isStale(out, entry)) {
			return;
		}

		Path tmp = out.resolveSibling(out.getFileName().toString() + "." + UUID.randomUUID() + ".tmp");
		try (InputStream in = jar.getInputStream(entry)) {
			if (in == null) {
				throw new IOException("Resource stream was null for: " + entry.getName());
			}
			Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
		}
		try {
			try {
				Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(tmp);
		}
		out.toFile().deleteOnExit();
	}

	private static boolean isStale(Path existing, JarEntry entry) throws IOException {
		long entrySize = entry.getSize();
		if (entrySize >= 0 && Files.size(existing) != entrySize) {
			return true;
		}
		long entryCrc = entry.getCrc();
		if (entryCrc >= 0 && computeCrc32(existing) != entryCrc) {
			return true;
		}
		return false;
	}

	private static long computeCrc32(Path file) throws IOException {
		CRC32 crc = new CRC32();
		crc.update(Files.readAllBytes(file));
		return crc.getValue();
	}

	private static Path getOrCreateTempDir() throws IOException {
		if (cachedTempDir != null) return cachedTempDir;

		Path base = Paths.get(System.getProperty("java.io.tmpdir"));
		Path dir = base.resolve("cognicryptdoc_resources");
		Files.createDirectories(dir);
		dir.toFile().deleteOnExit();

		cachedTempDir = dir;
		return cachedTempDir;
	}


	public static List<CrySLRule> readRulesFromSourceFilesWithoutFiles(final String folderPath) throws CryptoAnalysisException, MalformedURLException {
		return new ArrayList<>(readRulesFromSourceFiles(folderPath).values());
	}

	public static Map<File, CrySLRule> readRulesFromSourceFiles(final String folderPath) throws CryptoAnalysisException, MalformedURLException {
		if (folderPath == null || folderPath.isEmpty()) {
			throw new IllegalArgumentException("Folder path cannot be null or empty");
		}

		CrySLModelReader cryslModelReader = new CrySLModelReader();
		Map<File, CrySLRule> rules = new HashMap<>();

		try {
			File folder = new File(folderPath);
			if (!folder.isDirectory()) {
				throw new IllegalArgumentException("Invalid folder path: " + folderPath);
			}

			File[] files = folder.listFiles();
			if (files == null) {
				return rules;
			}
			for (File file : files) {
				if (file != null && file.getName().endsWith(".crysl")) {
					rules.put(file, cryslModelReader.readRule(file));
				}
			}

		} catch (CryptoAnalysisException e) {
			// Handle CryptoAnalysisException
			throw e;
		}

		return rules;
	}

}
