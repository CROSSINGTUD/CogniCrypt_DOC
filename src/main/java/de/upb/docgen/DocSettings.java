package de.upb.docgen;

/**
 * @author Sven Feldmann
 */

public class DocSettings {

    // Singleton holder to share parsed settings across the app.
    private static final DocSettings singletonSettings = new DocSettings();

    // Output and optional override paths.
    private String reportDirectory = null;
    private String rulesetPathDir = null;
    private String ftlTemplatesPath = null;
    private String langTemplatesPath = null;
    // booleans to modify FTL templates
    private boolean booleanA = true;
    private boolean booleanB = true;
    private boolean booleanC = true;
    private boolean booleanD = true;
    private boolean booleanE = true;
    private boolean booleanF = true;
    private boolean booleanG = true;



    // LLM feature toggles (default enabled).
    private boolean genLllmExplanations = true;
    private boolean genLlmExamples = true;

    // LLM backend selection (default: OpenAI).
    private String llmBackend = "openai"; // default

    private DocSettings() {

    }

    public boolean isGenLlmExamples() {
        return genLlmExamples;
    }

    public boolean isGenLllmExplanations() {
        return genLllmExplanations;
    }

    public static DocSettings getInstance() {
        return singletonSettings;
    }

    public String getFtlTemplatesPath() {
        return ftlTemplatesPath;
    }

    public void setFTLTemplatesPath(String ftlTemplatesPath) {
        this.ftlTemplatesPath = ftlTemplatesPath;
    }

    public String getRulesetPathDir() {
        return rulesetPathDir;
    }

    public void setRulesetPathDir(String rulesetPathDir) {
        this.rulesetPathDir = rulesetPathDir;
    }

    public String getReportDirectory() {
        return reportDirectory;
    }

    public void setReportDirectory(String reportDirectory) {
        this.reportDirectory = reportDirectory;
    }

    public boolean isBooleanA() {
        return booleanA;
    }

    public void setBooleanA(boolean booleanA) {
        this.booleanA = booleanA;
    }

    public boolean isBooleanB() {
        return booleanB;
    }

    public void setBooleanB(boolean booleanB) {
        this.booleanB = booleanB;
    }

    public boolean isBooleanC() {
        return booleanC;
    }

    public void setBooleanC(boolean booleanC) {
        this.booleanC = booleanC;
    }

    public boolean isBooleanD() {
        return booleanD;
    }

    public void setBooleanD(boolean booleanD) {
        this.booleanD = booleanD;
    }

    public boolean isBooleanE() {
        return booleanE;
    }

    public void setBooleanE(boolean booleanE) {
        this.booleanE = booleanE;
    }

    public boolean isBooleanG() {
        return booleanG;
    }

    public void setBooleanG(boolean booleanG) {
        this.booleanG = booleanG;
    }

    public String getLlmBackend() {
        return llmBackend;
    }

    // Strictly parses an on/off toggle value; anything else is a usage error rather
    // than being silently treated as "on".
    private static boolean parseToggleValue(String arg, String prefix) {
        String v = arg.substring(prefix.length()).trim().toLowerCase();
        if (v.equals("on") || v.equals("true") || v.equals("1")) {
            return true;
        }
        if (v.equals("off") || v.equals("false") || v.equals("0")) {
            return false;
        }
        showErrorMessage(arg);
        System.exit(255);
        return false; // unreachable
    }

    // Enforces value presence for flags that require an argument.
    private static String requireValue(String[] settings, int i, String flagName) {
    if (i + 1 >= settings.length) {
        showErrorMessage("Missing value for " + flagName);
        System.exit(255);
    }
    String v = settings[i + 1];
    if (v == null || v.trim().isEmpty() || v.trim().startsWith("--")) {
        showErrorMessage("Invalid value for " + flagName + ": " + v);
        System.exit(255);
    }
    return v;
}


    /**
     * Basic parsing functions see showErrorMessage method for flag explanations.
     * Sets paths and booleans for templates.
     * 
     * @param settings flags provided developer on the CLI
     */
    public void parseSettingsFromCLI(String[] settings) {
    
        if (settings == null || settings.length == 0) {
            showErrorMessage();
            System.exit(255);
            return;
        }

        for (int i = 0; i < settings.length; i++) {
            if (settings[i] == null) {
                showErrorMessage("null argument at position " + i);
                System.exit(255);
            }
            switch (settings[i].toLowerCase()) {
                case "--rulesdir":
                    setRulesetPathDir(requireValue(settings, i, "--rulesDir"));
                    i++;
                    break;

                case "--reportpath":
                    setReportDirectory(requireValue(settings, i, "--reportPath"));
                    i++;
                    break;

                case "--ftltemplatespath":
                    setFTLTemplatesPath(requireValue(settings, i, "--ftlTemplatesPath"));
                    i++;
                    break;

                case "--langtemplatespath":
                    setLangTemplatesPath(requireValue(settings, i, "--langTemplatesPath"));
                    i++;
                    break;

                case "--booleana":
                    setBooleanA(false);
                    break;
                case "--booleanb":
                    setBooleanB(false);
                    break;
                case "--booleanc":
                    setBooleanC(false);
                    break;
                case "--booleand":
                    setBooleanD(false);
                    break;
                case "--booleane":
                    setBooleanE(false);
                    break;
                case "--booleanf":
                    setBooleanF(false);
                    break;
                case "--booleang":
                    setBooleanG(false);
                    break;
                case "--disable-llm-explanations":
                    genLllmExplanations = false;
                    break;
                case "--disable-llm-examples":
                    genLlmExamples = false;
                    break;
                default:
                    if (settings[i].toLowerCase().startsWith("--llm=")) {
                        boolean master = parseToggleValue(settings[i], "--llm=");
                        // Master applies unless overridden later by specific flags (order of args matters)
                        genLllmExplanations = master;
                        genLlmExamples = master;
                        break;
                    }
                    if (settings[i].toLowerCase().startsWith("--llm-explanations=")) {
                        genLllmExplanations = parseToggleValue(settings[i], "--llm-explanations=");
                        break;
                    }
                    if (settings[i].toLowerCase().startsWith("--llm-examples=")) {
                        genLlmExamples = parseToggleValue(settings[i], "--llm-examples=");
                        break;
                    }
                    if (settings[i].toLowerCase().startsWith("--llm-backend=")) {
                        String v = settings[i].substring("--llm-backend=".length()).trim().toLowerCase();
                        if (v.equals("openai") || v.equals("gateway")) {
                            llmBackend = v;
                        } else {
                            showErrorMessage(settings[i]);
                            System.exit(255);
                        }
                        break;
                    }
                    showErrorMessage(settings[i]);
                    System.exit(255);
            }
        }
        // Sven feature: only --reportPath is mandatory now
        if (reportDirectory == null || reportDirectory.trim().isEmpty()) {
            showErrorMessage("--reportPath is required");
            System.exit(255);
        }

    }

    private static void showErrorMessage() {
        String errorMessage =
            "An error occurred while trying to parse the CLI arguments.\n\n" +

            "Minimal command (recommended):\n" +
            "  java -jar <CogniCryptDOC.jar> --reportPath <output_dir>\n\n" +

            "Optional overrides (only if you want to override bundled defaults):\n" +
            "  --rulesDir <path_to_CrySL_rules>\n" +
            "  --ftlTemplatesPath <path_to_ftl_templates>\n" +
            "  --langTemplatesPath <path_to_lang_templates>\n\n" +

            "Additional flags:\n" +
            "  --booleanA <hide state machine graph>\n" +
            "  --booleanB <hide help>\n" +
            "  --booleanC <hide dependency trees>\n" +
            "  --booleanD <hide CrySL rule>\n" +
            "  --booleanE <turn off graphviz generation>\n" +
            "  --booleanF <copy CrySL rules into documentation folder>\n" +
            "  --booleanG <use fully qualified name in state machine graph>\n\n" +

            "LLM flags:\n" +
            "  --disable-llm-explanations\n" +
            "  --disable-llm-examples\n" +
            "  --llm=<on|off|true|false|1|0>\n" +
            "  --llm-explanations=<on|off|true|false|1|0>\n" +
            "  --llm-examples=<on|off|true|false|1|0>\n" +
            "  --llm-backend=<openai|gateway>\n";

        System.out.println(errorMessage);
    }


    private static void showErrorMessage(String arg) {
        String errorMessage =
            "An error occurred while trying to parse the CLI argument: " + arg + "\n\n" +

            "Minimal command (recommended):\n" +
            "  java -jar <CogniCryptDOC.jar> --reportPath <output_dir>\n\n" +

            "Optional overrides (only if you want to override bundled defaults):\n" +
            "  --rulesDir <path_to_CrySL_rules>\n" +
            "  --ftlTemplatesPath <path_to_ftl_templates>\n" +
            "  --langTemplatesPath <path_to_lang_templates>\n\n" +

            "Additional flags:\n" +
            "  --booleanA <hide state machine graph>\n" +
            "  --booleanB <hide help>\n" +
            "  --booleanC <hide dependency trees>\n" +
            "  --booleanD <hide CrySL rule>\n" +
            "  --booleanE <turn off graphviz generation>\n" +
            "  --booleanF <copy CrySL rules into documentation folder>\n" +
            "  --booleanG <use fully qualified name in state machine graph>\n\n" +

            "LLM flags:\n" +
            "  --disable-llm-explanations\n" +
            "  --disable-llm-examples\n" +
            "  --llm=<on|off|true|false|1|0>\n" +
            "  --llm-explanations=<on|off|true|false|1|0>\n" +
            "  --llm-examples=<on|off|true|false|1|0>\n" +
            "  --llm-backend=<openai|gateway>\n";

        System.out.println(errorMessage);
    }


    public String getLangTemplatesPath() {
        return langTemplatesPath;
    }

    public void setLangTemplatesPath(String langTemplatesPath) {
        this.langTemplatesPath = langTemplatesPath;
    }

    public boolean isBooleanF() {
        return booleanF;
    }

    public void setBooleanF(boolean booleanF) {
        this.booleanF = booleanF;
    }
}
