package de.upb.docgen;

/**
 * @author Sven Feldmann
 */

public class DocSettings {

    private static final DocSettings singletonSettings = new DocSettings();

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

    private DocSettings() {

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

    /**
     * Basic parsing functions see showErrorMessage method for flag explanations.
     * Sets paths and booleans for templates.
     * 
     * @param settings flags provided developer on the CLI
     */
    public void parseSettingsFromCLI(String[] settings) {
        int mandatorySettings = 0;
        if (settings == null) {
            showErrorMessage();
            System.exit(255);
        }
        for (int i = 0; i < settings.length; i++) {
            switch (settings[i].toLowerCase()) {
                case "--rulesdir":
                    setRulesetPathDir(settings[i + 1]);
                    i++;
                    mandatorySettings++;
                    break;
                case "--reportpath":
                    setReportDirectory(settings[i + 1]);
                    i++;
                    mandatorySettings++;
                    break;
                case "--ftltemplatespath":
                    setFTLTemplatesPath(settings[i + 1]);
                    i++;
                    mandatorySettings++;
                    break;
                case "--langtemplatespath":
                    setLangTemplatesPath(settings[i + 1]);
                    i++;
                    mandatorySettings++;
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
                default:
                    showErrorMessage(settings[i]);
                    System.exit(255);
            }
        }
        if (mandatorySettings != 4) {
            showErrorMessage();
            System.exit(255);
        }
    }

    private static void showErrorMessage() {
        String errorMessage = "An error occurred while trying to parse the CLI arguments.\n"
                + "The default command for running CogniCryptDOC is: \n" +
                "java -jar <jar_location_of_CogniCryptDOC> \\\r\n" +
                " 		--rulesDir <absolute_path_to_CrySL_rules> \\\r\n" +
                " 		--FTLtemplatesPath <absolute_path_to_ftl_templates> \\\r\n" +
                " 		--LANGtemplatesPath <absolute_path_to_lang_templates> \\\r\n" +
                "       --reportPath <absolute_path_to_generate_documentation>\n";
        System.out.println(errorMessage);
    }

    private static void showErrorMessage(String arg) {
        String errorMessage = "An error occurred while trying to parse the CLI argument: " + arg + ".\n"
                + "The default command for running CogniCryptDOC is: \n" +
                "java -jar <jar_location_of_CogniCryptDOC> \\\r\n" +
                " 		--rulesDir <absolute_path_to_CrySL_rules> \\\r\n" +
                " 		--templatesPath <absolute_path_to_ftl_templates> \\\r\n" +
                " 		--LANGtemplatesPath <absolute_path_to_lang_templates> \\\r\n" +
                "       --reportPath <absolute_path_to_generate_documentation>\n"
                + "\nAdditional arguments that can be used are:\n"
                + "--booleanA <To hide state machine graph>\n"
                + "--booleanB <To hide help>\n"
                + "--booleanC <To hide dependency trees>\n"
                + "--booleanD <To hide CrySL rule\n"
                + "--booleanE <To turn of graphviz generation\n"
                + "--booleanF <To copy CrySL rules into documentation folder>\n" // Relative Paths for FTL templates if
                                                                                 // distributed
                + "--booleanG <To turn on fully qualified name in state machine graph>\n";
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
