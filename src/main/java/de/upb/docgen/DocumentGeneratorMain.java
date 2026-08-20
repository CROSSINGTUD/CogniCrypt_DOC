package de.upb.docgen;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import crypto.exceptions.CryptoAnalysisException;
import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLRule;
import crypto.rules.CrySLRuleReader;
import de.upb.docgen.crysl.CrySLReader;
import de.upb.docgen.llm.CrySLToLLMGenerator;
import de.upb.docgen.utils.*;
import de.upb.docgen.writer.FreeMarkerWriter;
import freemarker.template.*;
import org.apache.commons.io.FileUtils;

import static de.upb.docgen.llm.CrySLToLLMGenerator.cleanLLMCodeBlock;

/**
 * @author Ritika Singh
 * @author Sven Feldmann
 */

public class DocumentGeneratorMain {

	// Shared reader for filesystem-based CrySL rules.
	private static final CrySLRuleReader ruleReader = new CrySLRuleReader();
    private static final List<String> CRITICAL_STARTUP_CLASSES = List.of(
            "de.upb.docgen.writer.FreeMarkerWriter",
            "freemarker.template.Configuration",
            "de.upb.docgen.llm.LLMService"
    );

    /**
     * Fail fast if critical runtime classes are missing on the current classpath.
     */
    private static void runStartupPreflight() {
        ClassLoader cl = DocumentGeneratorMain.class.getClassLoader();
        for (String className : CRITICAL_STARTUP_CLASSES) {
            try {
                Class.forName(className, false, cl);
            } catch (ClassNotFoundException e) {
                String cp = System.getProperty("java.class.path", "");
                throw new IllegalStateException(
                        "Startup preflight failed. Missing class: " + className + ". " +
                                "Please reload Maven project and rebuild. Active classpath: " + cp,
                        e
                );
            }
        }
    }

    private static boolean isRetryablePlaceholder(String content, boolean secure) {
        if (content == null) return false;
        String txt = content.trim();
        String unavailable = secure ? "// LLM secure example unavailable." : "// LLM insecure example unavailable.";
        String disabled = secure ? "// LLM secure example disabled by flag." : "// LLM insecure example disabled by flag.";
        String failedPrefix = secure ? "// LLM secure example failed:" : "// LLM insecure example failed:";
        return txt.equals(unavailable) || txt.equals(disabled) || txt.startsWith(failedPrefix);
    }

    /**
     * True if a cached explanation is a placeholder rather than a real answer.
     *
     * <p>The explanation cache is keyed only by class and language, so without this a
     * "disabled by flag" or "no explanation generated" placeholder written by an earlier
     * run is served forever - even once explanations are enabled and a real one exists.
     * The example cache has had an equivalent check all along; this is its counterpart.
     */
    private static boolean isRetryableExplanationPlaceholder(String content) {
        return Utils.isRetryableExplanationPlaceholder(content);
    }

    private static boolean isFailurePlaceholder(String content, boolean secure) {
        return isRetryablePlaceholder(content, secure);
    }

    private static String failureReason(String content, boolean secure) {
        if (content == null) return "unknown";
        String txt = content.trim();
        String failedPrefix = secure ? "// LLM secure example failed:" : "// LLM insecure example failed:";
        if (txt.startsWith(failedPrefix)) {
            String reason = txt.substring(failedPrefix.length()).trim();
            return reason.isEmpty() ? "generation failed" : reason;
        }
        return "example unavailable";
    }

    private static void writeCodegenFailureReport(File codeCacheDir, List<String> failures,
            List<String> fullDetails) throws IOException {
        File report = new File(codeCacheDir, "llm_codegen_failures.txt");
        StringBuilder sb = new StringBuilder();
        sb.append("LLM code generation failure report").append(System.lineSeparator());
        sb.append("Generated at: ").append(new Date()).append(System.lineSeparator());
        sb.append(System.lineSeparator());
        if (failures == null || failures.isEmpty()) {
            sb.append("No LLM code generation failures.").append(System.lineSeparator());
        } else {
            for (String failure : failures) {
                sb.append("- ").append(failure).append(System.lineSeparator());
            }
        }

        // The generated pages carry only a one-line summary, so the unabridged output -
        // traceback, compiler diagnostics, the shaped CrySL contract - is kept here.
        if (fullDetails != null && !fullDetails.isEmpty()) {
            sb.append(System.lineSeparator());
            sb.append("Full diagnostics").append(System.lineSeparator());
            sb.append("================").append(System.lineSeparator());
            for (String detail : fullDetails) {
                sb.append(System.lineSeparator()).append(detail).append(System.lineSeparator());
            }
        }

        Files.writeString(report.toPath(), sb.toString());
        System.out.println("Codegen failure report: " + report.getPath());
    }

	/**
	 * Entry point: loads CrySL rules, builds composed docs, optionally runs LLM steps,
	 * and renders HTML outputs into the report directory.
	 */
	public static void main(String[] args) throws IOException, TemplateException, CryptoAnalysisException {
		// Track total execution time across the full pipeline.
        final long start = System.nanoTime();
        try {
            // Supported natural-language outputs for LLM explanations.
            List<String> LANGUAGES = List.of("English", "Portuguese", "German", "French");
            DocSettings docSettings = DocSettings.getInstance();
            System.out.println("Parsing CLI Flags");
            docSettings.parseSettingsFromCLI(args);
            runStartupPreflight();

            // Load CrySL rules either from user-provided folder or bundled JAR resources.
            System.out.println("Reading CrySL Rules");
            List<CrySLRule> rules;
            if (docSettings.getRulesetPathDir() != null && !docSettings.getRulesetPathDir().trim().isEmpty()) {
                rules = ruleReader.readFromDirectory(new File(docSettings.getRulesetPathDir()));
            } else {
                rules = CrySLReader.readRulesFromJar();
            }


            System.out.println("Reading CrySL Rules Done");

            // Fail loudly instead of writing a fully-formed but empty documentation site.
            if (rules.isEmpty()) {
                String where = (docSettings.getRulesetPathDir() != null && !docSettings.getRulesetPathDir().trim().isEmpty())
                        ? docSettings.getRulesetPathDir()
                        : "the bundled CrySLRules resources";
                throw new IOException("No CrySL rules were loaded from " + where
                        + ". Check --rulesDir: nothing would be documented.");
            }

            // Two rule files describing the same class would silently overwrite each
            // other's page and appear twice in the sidebar; keep the first, skip the rest.
            Set<String> seenClassNames = new HashSet<>();
            List<CrySLRule> uniqueRules = new ArrayList<>(rules.size());
            for (CrySLRule ruleEntry : rules) {
                if (seenClassNames.add(ruleEntry.getClassName())) {
                    uniqueRules.add(ruleEntry);
                } else {
                    System.err.println("[WARN] Duplicate CrySL rule for " + ruleEntry.getClassName()
                            + " - keeping the first definition and skipping this one.");
                }
            }
            rules = uniqueRules;
            // Helpers to build composed documentation sections from CrySL rules.
            ClassEventForb cef = new ClassEventForb();
            ConstraintsVc valueconstraint = new ConstraintsVc();
            ConstraintsPred predicateconstraint = new ConstraintsPred();
            ConstraintsComparison comp = new ConstraintsComparison();
            ConstraintCrySLVC cryslvc = new ConstraintCrySLVC();
            ConstraintCryslnocallto nocall = new ConstraintCryslnocallto();
            ConstraintCrySLInstanceof instance = new ConstraintCrySLInstanceof();
            ConstraintCrySLandencmode enc = new ConstraintCrySLandencmode();
            Order or = new Order();
            Ensures en = new Ensures();
            EnsuresCaseTwo entwo = new EnsuresCaseTwo();
            Negates neg = new Negates();
            List<ComposedRule> composedRuleList = new ArrayList<>();
            Map<String, List<CrySLPredicate>> mapEnsures = new HashMap<>();
            Map<String, List<CrySLPredicate>> mapRequires = new HashMap<>();

            // Build class -> predicates maps to resolve cross-rule dependencies.
            for (CrySLRule ruleEntry : rules) {
                CrySLRule rule = ruleEntry;
                mapEnsures.put(rule.getClassName(), rule.getPredicates());
                mapRequires.put(rule.getClassName(), rule.getRequiredPredicates());
            }

            // Create composed rules (all sections assembled for FreeMarker).
            List<CrySLRule> cryslRuleList = new ArrayList<>();
            List<String> compositionFailures = new ArrayList<>();
            for (CrySLRule ruleEntry : rules) {
                // Per-rule isolation: one malformed rule must not abort the run and
                // leave a half-populated output directory. A rule that throws is
                // skipped entirely - it is never added to either list, so the two stay
                // aligned and no partial page is written for it.
                try {
                    ComposedRule composedRule = new ComposedRule();
                    CrySLRule rule = ruleEntry;
                    // CrySL to .txt format (supports both: rules from disk OR bundled rules from JAR)
                    String fullClassName = rule.getClassName();
                    String simpleName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);

                    File cryslFile;
                    if (docSettings.getRulesetPathDir() != null && !docSettings.getRulesetPathDir().trim().isEmpty()) {
                        // if user provided --rulesDir, read the .crysl from that directory
                        cryslFile = new File(docSettings.getRulesetPathDir(), simpleName + ".crysl");
                    } else {
                        // otherwise read bundled .crysl from the JAR
                        cryslFile = CrySLReader.readRuleFromJarFile(simpleName);
                    }

                    if (cryslFile != null && cryslFile.exists()) {
                        String ruleText = Files.readString(cryslFile.toPath());
                        composedRule.setCryslRuleText("\n" + ruleText);
                    } else {
                        composedRule.setCryslRuleText("// CrySL file not found for: " + simpleName);
                    }


                    // Overview section
                    String classname = rule.getClassName();
                    // fully qualified name
                    composedRule.setComposedClassName(classname);
                    // Only rule name necessary for ftl Template
                    composedRule.setOnlyRuleName(classname.substring(classname.lastIndexOf(".") + 1));
                    // Set classname sentence
                    composedRule.setComposedFullClass(cef.getFullClassName(rule));
                    // Link to corresponding JavaDoc
                    composedRule.setComposedLink(cef.getLink(rule));
                    composedRule.setOnlyLink(cef.getLinkOnly(rule));
                    composedRule.setJavaDocUrl(cef.getJavaDocUrl(rule));
                    composedRule.setNumberOfMethods(cef.getEventNumbers(rule));

                    // Order section
                    composedRule.setOrder(or.runOrder(rule));

                    //
                    composedRule.setValueConstraints(valueconstraint.getConstraintsVc(rule));
                    // create necessary Data structure to link required predicates of current crysl
                    // rule
                    Map<String, List<Map<String, List<String>>>> singleRuleEnsuresMap = Utils.mapPredicates(mapEnsures,
                            mapRequires);
                    // Pairing Dependency only by class name
                    Map<String, Set<String>> singleReqToEns = Utils.toOnlyClassNames(singleRuleEnsuresMap);
                    Set<String> ensuresForThisRule = singleReqToEns.get(composedRule.getComposedClassName());
                    composedRule.setConstrainedPredicates(
                            predicateconstraint.getConstraintsPred(rule, ensuresForThisRule, singleRuleEnsuresMap));
                    // ConstraintsSection
                    composedRule.setComparsionConstraints(comp.getConstriantsComp(rule));
                    composedRule.setConstrainedValueConstraints(cryslvc.getConCryslVC(rule));
                    composedRule.setNoCallToConstraints(nocall.getnoCalltoConstraint(rule));
                    composedRule.setInstanceOfConstraints(instance.getInstanceof(rule));
                    composedRule.setConstraintAndEncConstraints(enc.getConCryslandenc(rule));
                    composedRule.setForbiddenMethods(cef.getForb(rule));
                    //
                    List<String> allConstraints = new ArrayList<>(composedRule.getComparsionConstraints());
                    allConstraints.addAll(composedRule.getValueConstraints());
                    allConstraints.addAll(composedRule.getConstrainedPredicates());
                    allConstraints.addAll(composedRule.getConstrainedValueConstraints());
                    allConstraints.addAll(composedRule.getNoCallToConstraints());
                    allConstraints.addAll(composedRule.getInstanceOfConstraints());
                    allConstraints.addAll(composedRule.getConstraintAndEncConstraints());
                    allConstraints.addAll(composedRule.getForbiddenMethods());
                    composedRule.setAllConstraints(allConstraints);

                    // Predicates Section
                    composedRule
                            .setEnsuresThisPredicates(en.getEnsuresThis(rule, Utils.mapPredicates(mapRequires, mapEnsures)));
                    composedRule.setEnsuresPredicates(entwo.getEnsures(rule, Utils.mapPredicates(mapRequires, mapEnsures)));
                    composedRule.setNegatesPredicates(neg.getNegates(rule));
                    composedRuleList.add(composedRule);

                    cryslRuleList.add(rule);
                } catch (Exception ruleFailure) {
                    String failedClass = ruleEntry.getClassName();
                    compositionFailures.add(failedClass + ": " + ruleFailure);
                    System.err.println("[WARN] Skipping rule " + failedClass
                            + " - composition failed: " + ruleFailure);
                }
            }

            if (!compositionFailures.isEmpty()) {
                System.err.println("[WARN] " + compositionFailures.size() + " of " + rules.size()
                        + " rules were skipped because composition failed:");
                for (String failure : compositionFailures) {
                    System.err.println("       - " + failure);
                }
            }

            // Build dependency trees for requires/ensures rendering.
            Map<String, List<Map<String, List<String>>>> ensuresToRequiresMap = Utils.mapPredicates(mapRequires,
                    mapEnsures);
            Map<String, List<Map<String, List<String>>>> requiresToEnsuresMap = Utils.mapPredicates(mapEnsures,
                    mapRequires);

            Map<String, Set<String>> onlyClassnamesReqToEns = Utils.toOnlyClassNames(ensuresToRequiresMap);
            Map<String, Set<String>> onlyClassnamesEnsToReq = Utils.toOnlyClassNames(requiresToEnsuresMap);

            Map<String, TreeNode<String>> reqToEns = PredicateTreeGenerator.buildDependencyTreeMap(onlyClassnamesReqToEns);
            Map<String, TreeNode<String>> ensToReq = PredicateTreeGenerator.buildDependencyTreeMap(onlyClassnamesEnsToReq);


//        System.out.println("Iv deps  : " + onlyClassnamesEnsToReq.get("javax.crypto.spec.IvParameterSpec"));
//        System.out.println("GCM deps : " + onlyClassnamesEnsToReq.get("javax.crypto.spec.GCMParameterSpec"));
//        System.out.println("SR ensures randomized? (sanity) class present: " +
//                onlyClassnamesEnsToReq.containsKey("java.security.SecureRandom"));

//        List<String> order = Utils.leafToRootOrderTopo("javax.crypto.Mac", onlyClassnamesEnsToReq);
//        Map<String, Set<String>> sanitized =
//                GraphSanitizer.sanitize(onlyClassnamesEnsToReq, onlyClassnamesReqToEns, "javax.crypto.Cipher");
//        GraphVerification.verifyOrdering("javax.crypto.Cipher" ,sanitized);
//        List<String> order = Utils.leafToRootOrderTopo("javax.crypto.Cipher", sanitized);
//
//        System.out.println("order: " + order);

            // Pair the two lists by class name rather than by position. The loops below
            // consumed composedRuleList.get(i) alongside cryslRuleList.get(i); that is the
            // coupling that put the wrong state-machine diagram on 49 of 51 pages once an
            // in-place sort reordered one of them. Nothing enforced the invariant, so it is
            // removed here rather than relied upon.
            Map<String, ComposedRule> composedByClassName = new HashMap<>();
            for (ComposedRule composed : composedRuleList) {
                composedByClassName.putIfAbsent(composed.getComposedClassName(), composed);
            }

            // Verify dependency ordering and record rule-specific dependency list.
            for (int i = 0; i < cryslRuleList.size(); i++) {
                CrySLRule rule = cryslRuleList.get(i);
                ComposedRule composedRule = composedByClassName.get(rule.getClassName());
                if (composedRule == null) {
                    continue; // rule was skipped during composition
                }
                String ruleName = rule.getClassName();
                Map<String, Set<String>> sanitized =
                        GraphSanitizer.sanitize(onlyClassnamesEnsToReq, onlyClassnamesReqToEns, ruleName);
                GraphVerification.verifyOrdering(ruleName, sanitized);
                List<String> order = Utils.leafToRootOrderTopo(ruleName, sanitized);
                composedRule.setDependency(order);
            }


// LLM Call for Explanation (fixed)

            // LLM cache for explanations (one file per class/language) under reportPath/resources.
            File cacheDir = CachePathResolver.resolveLlmCacheDir(docSettings.getReportDirectory()).toFile();
            Files.createDirectories(cacheDir.toPath());

// 1. Generate all explanations once (adjust if API differs)
            if (docSettings.isGenLllmExplanations()) {
                CrySLToLLMGenerator.generateExplanations(composedRuleList, cryslRuleList, docSettings.getLlmBackend());
            } else {
                System.out.println("LLM explanations: DISABLED by flag");
            }

            // Load (or write) explanation cache and attach to composed rules.
            for (int i = 0; i < cryslRuleList.size(); i++) {
                CrySLRule rule = cryslRuleList.get(i);
                ComposedRule composedRule = composedByClassName.get(rule.getClassName());
                if (composedRule == null) {
                    continue; // rule was skipped during composition
                }

                String ruleName = rule.getClassName();
                String fileSafeName = ruleName.replaceAll("[^a-zA-Z0-9.\\-]", "_");

                Map<String, String> explanationMap = new HashMap<>();

                for (String lang : LANGUAGES) {
                    String fileName = fileSafeName + "_" + lang + ".txt";
                    File cacheFile = new File(cacheDir, fileName);

                    // Retrieve the explanation produced by the generator (adapt getter if different)
                    String explanation;

                    if (docSettings.isGenLllmExplanations()) {
                        explanation = composedRule.getLlmExplanation() != null
                                ? composedRule.getLlmExplanation().get(lang)
                                : null;

                        if (explanation == null || explanation.isBlank()) {
                            explanation = "No explanation generated for " + ruleName + " (" + lang + ").";
                        }
                    } else {
                        explanation = "LLM explanations disabled by flag.";
                    }

                    String cachedExplanation = cacheFile.exists()
                            ? Files.readString(cacheFile.toPath())
                            : null;

                    if (cachedExplanation != null && !isRetryableExplanationPlaceholder(cachedExplanation)) {
                        // A real cached answer: reuse it.
                        explanation = cachedExplanation;
                        System.out.println(fileName + " already exists.");
                    } else {
                        // Absent, or a placeholder from an earlier run - (re)write it so a
                        // stale "disabled"/"not generated" marker cannot outlive its cause.
                        Files.writeString(cacheFile.toPath(), explanation);
                        System.out.println(fileName + (cachedExplanation == null ? " written." : " refreshed (was a placeholder)."));
                    }

                    explanationMap.put(lang, explanation);
                }

                // Ensure composedRule holds the final map (overwrites any prior one)
                composedRule.setLlmExplanation(explanationMap);
            }
            //LLM Call for Secure and Insecure Code Generation

            // LLM cache for code examples (secure/insecure per rule) under reportPath/resources.
            File codeCacheDir = CachePathResolver.resolveCodeCacheDir(docSettings.getReportDirectory()).toFile();
            Files.createDirectories(codeCacheDir.toPath());
            List<String> codegenFailures = new ArrayList<>();
            List<String> codegenFullDetails = new ArrayList<>();

            for (int i = 0; i < cryslRuleList.size(); i++) {
                CrySLRule rule = cryslRuleList.get(i);
                ComposedRule composedRule = composedByClassName.get(rule.getClassName());
                if (composedRule == null) {
                    continue; // rule was skipped during composition
                }
                String ruleName = rule.getClassName().replaceAll("[^a-zA-Z0-9.\\-]", "_");

                File secureFile = new File(codeCacheDir, ruleName + "_secure.txt");
                File insecureFile = new File(codeCacheDir, ruleName + "_insecure.txt");

                String secure;
                String insecure;

                if (DocSettings.getInstance().isGenLlmExamples()) {
                    // ----- LLM examples ENABLED -----
                    String existingSecure = secureFile.exists() ? Files.readString(secureFile.toPath()) : null;
                    String existingInsecure = insecureFile.exists() ? Files.readString(insecureFile.toPath()) : null;
                    boolean secureNeedsGeneration = existingSecure == null || isRetryablePlaceholder(existingSecure, true);
                    boolean insecureNeedsGeneration = existingInsecure == null || isRetryablePlaceholder(existingInsecure, false);

                    String generatedSecure = null;
                    String generatedInsecure = null;
                    String generationError = null;

                    if (secureNeedsGeneration || insecureNeedsGeneration) {
                        if (existingSecure != null && isRetryablePlaceholder(existingSecure, true)) {
                            System.out.println(ruleName + "_secure.txt contains placeholder; regenerating.");
                        }
                        if (existingInsecure != null && isRetryablePlaceholder(existingInsecure, false)) {
                            System.out.println(ruleName + "_insecure.txt contains placeholder; regenerating.");
                        }
                        try {
                            // Only ask for what is actually missing - regenerating both
                            // types burned an API call whose result was then discarded.
                            CrySLToLLMGenerator.generateExample(List.of(composedRule), List.of(rule),
                                    docSettings.getLlmBackend(), secureNeedsGeneration, insecureNeedsGeneration);
                            generatedSecure = cleanLLMCodeBlock(
                                    composedRule.getSecureExample() != null ? composedRule.getSecureExample() : "");
                            generatedInsecure = cleanLLMCodeBlock(
                                    composedRule.getInsecureExample() != null ? composedRule.getInsecureExample() : "");
                        } catch (Exception e) {
                            // Full text goes to the failure report; only a short, path-free
                            // summary is allowed anywhere near a generated page.
                            codegenFullDetails.add(rule.getClassName() + ": " + e.getMessage());
                            generationError = Utils.summarizeGenerationFailure(e.getMessage());
                        }
                    }

                    if (secureNeedsGeneration) {
                        if (generatedSecure != null && !generatedSecure.isBlank()) {
                            secure = generatedSecure;
                        } else if (generationError != null && !generationError.isBlank()) {
                            secure = "// LLM secure example failed: " + generationError;
                        } else {
                            secure = "// LLM secure example unavailable.";
                        }
                        Files.writeString(secureFile.toPath(), secure);
                        System.out.println(ruleName + "_secure.txt " + (existingSecure == null ? "created." : "updated."));
                    } else {
                        secure = existingSecure;
                        System.out.println(ruleName + "_secure.txt exists.");
                    }

                    if (insecureNeedsGeneration) {
                        if (generatedInsecure != null && !generatedInsecure.isBlank()) {
                            insecure = generatedInsecure;
                        } else if (generationError != null && !generationError.isBlank()) {
                            insecure = "// LLM insecure example failed: " + generationError;
                        } else {
                            insecure = "// LLM insecure example unavailable.";
                        }
                        Files.writeString(insecureFile.toPath(), insecure);
                        System.out.println(ruleName + "_insecure.txt " + (existingInsecure == null ? "created." : "updated."));
                    } else {
                        insecure = existingInsecure;
                        System.out.println(ruleName + "_insecure.txt exists.");
                    }
                } else {
                    // ----- LLM examples DISABLED -----
                    // Reuse cached files if present (no API calls), otherwise write placeholders
                    if (secureFile.exists()) {
                        secure = Files.readString(secureFile.toPath());
                        System.out.println(ruleName + "_secure.txt reused (LLM examples disabled).");
                    } else {
                        secure = "// LLM secure example disabled by flag.";
                        Files.writeString(secureFile.toPath(), secure);
                        System.out.println(ruleName + "_secure.txt placeholder created (disabled).");
                    }

                    if (insecureFile.exists()) {
                        insecure = Files.readString(insecureFile.toPath());
                        System.out.println(ruleName + "_insecure.txt reused (LLM examples disabled).");
                    } else {
                        insecure = "// LLM insecure example disabled by flag.";
                        Files.writeString(insecureFile.toPath(), insecure);
                        System.out.println(ruleName + "_insecure.txt placeholder created (disabled).");
                    }
                }

                // Attach code examples to the composed rule for template rendering.
                composedRule.setSecureExample(secure);
                composedRule.setInsecureExample(insecure);

                if (DocSettings.getInstance().isGenLlmExamples()) {
                    if (isFailurePlaceholder(secure, true)) {
                        codegenFailures.add(rule.getClassName() + " [secure]: " + failureReason(secure, true));
                    }
                    if (isFailurePlaceholder(insecure, false)) {
                        codegenFailures.add(rule.getClassName() + " [insecure]: " + failureReason(insecure, false));
                    }
                }
            }
            writeCodegenFailureReport(codeCacheDir, codegenFailures, codegenFullDetails);

            // Freemarker Setup and create cognicryptdoc html pages
            System.out.println("Setup Freemarker");
            Configuration cfg = new Configuration(new Version(2, 3, 20));
            FreeMarkerWriter.setupFreeMarker(cfg);
            FreeMarkerWriter.createCogniCryptLayout(cfg);
            FreeMarkerWriter.createSidebar(composedRuleList, cfg);
            FreeMarkerWriter.createSinglePage(composedRuleList, cfg, ensToReq, reqToEns, docSettings.isBooleanA(),
                    docSettings.isBooleanB(), docSettings.isBooleanC(), docSettings.isBooleanD(), docSettings.isBooleanE(),
                    docSettings.isBooleanF(), cryslRuleList, LANGUAGES);
            // copy CryslRulesFolder into generated Cognicrypt folder
            // specifify this flag to distribute the documentation
            System.out.println("CogniCryptDOC generated to: " + DocSettings.getInstance().getReportDirectory());
            if (!docSettings.isBooleanF()) {
                File dest = new File(docSettings.getReportDirectory() + File.separator + "rules");
                Files.createDirectories(dest.toPath());

                if (docSettings.getRulesetPathDir() != null && !docSettings.getRulesetPathDir().trim().isEmpty()) {
                    // user override: copy rules from the provided folder
                    File source = new File(docSettings.getRulesetPathDir());
                    FileUtils.copyDirectory(source, dest);
                } else {
                    // bundled mode: copy each rule file from the JAR into the report folder
                    for (CrySLRule rule : cryslRuleList) {
                        String fullClassName = rule.getClassName();
                        String simpleName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);

                        File ruleFile = CrySLReader.readRuleFromJarFile(simpleName);
                        if (ruleFile != null && ruleFile.exists()) {
                            File target = new File(dest, simpleName + ".crysl");
                            FileUtils.copyFile(ruleFile, target);
                        }

                    }
                }
            }

        } finally {
            // Emit total execution time as a single metric for the run.
            long end = System.nanoTime();
            double elapsedMs = (end - start) / 1_000_000.0;
            System.out.printf("Total execution time: %.3f ms (%.3f s)%n", elapsedMs, elapsedMs / 1000.0);
        }
	}

}
