package de.upb.docgen.writer;

import crypto.exceptions.CryptoAnalysisException;
import crypto.rules.CrySLRule;
import de.upb.docgen.*;
import de.upb.docgen.crysl.CrySLReader;
import de.upb.docgen.graphviz.StateMachineToGraphviz;
import de.upb.docgen.utils.TemplateAbsolutePathLoader;
import de.upb.docgen.utils.TreeNode;
import de.upb.docgen.utils.Utils;
import freemarker.template.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * @author Sven Feldmann
 */


public class FreeMarkerWriter {

    /**
     * Creates Sidebar for CogniCryptDOC
     * @param composedRuleList the fully qualified name is used from this list later from freemarker
     * @param cfg necessary for freemarker
     * @throws IOException
     * @throws TemplateException
     */
    public static void createSidebar(List<ComposedRule> composedRuleList, Configuration cfg ) throws IOException, TemplateException {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("title", "Sidebar");
        Collections.sort(composedRuleList, new Comparator<ComposedRule>() {
            @Override
            public int compare(ComposedRule o1, ComposedRule o2) {
                return o1.getComposedFullClass().compareTo(o2.getComposedFullClass());
            }
        });
        input.put("rules", composedRuleList);
        Template template = null;
        if (DocSettings.getInstance().getRulesetPathDir() != null) {
            template = cfg.getTemplate(Utils.pathForTemplates(DocSettings.getInstance().getFtlTemplatesPath() + "/" + "sidebar.ftl"));
        } else {
            template = cfg.getTemplate(CrySLReader.readFTLFromJar("sidebar.ftl").getPath());

        }
        // 2.3. Generate the output
        try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"navbar.html"))) {
            template.process(input, fileWriter);
        }
    }

    /**
     * Creates the single page of all rules
     * @param composedRuleList
     * @param cfg
     * @param reqToEns
     * @param ensToReq
     * @param a
     * @param b
     * @param c
     * @param f
     * @throws IOException
     * @throws TemplateException
     */
    public static void createSinglePage(List<ComposedRule> composedRuleList, Configuration cfg, Map<String, TreeNode<String>> reqToEns, Map<String, TreeNode<String>> ensToReq, boolean a, boolean b, boolean c, boolean d, boolean e, boolean f, List<CrySLRule> crySLRules) throws IOException, TemplateException, CryptoAnalysisException {
        for (int i = 0; i < composedRuleList.size(); i++) {
            ComposedRule rule = composedRuleList.get(i);
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("title", "class");
            input.put("rule", rule);
            TreeNode<String> rootReq = reqToEns.get(rule.getComposedClassName());
            input.put("requires", rootReq); // requires tree parsed by the template
            TreeNode<String> rootEns = ensToReq.get(rule.getComposedClassName());
            input.put("ensures", rootEns); // ensures tree parsed by the template

            // necessary input for the template to load absolute path from crysl rule which can be displayed
            if (DocSettings.getInstance().getRulesetPathDir() != null) {
                input.put("pathToRules", Utils.pathForTemplates("file://" + DocSettings.getInstance().getRulesetPathDir()));
            } else {
                input.put("pathToRules", Utils.pathForTemplates("file://" + CrySLReader.readRuleFromJarFile(composedRuleList.get(i).getOnlyRuleName()).getPath()));
            }
                // Set flags
            input.put("booleanA", a); // To show StateMachineGraph
            input.put("booleanB", b); // To show Help Button
            input.put("booleanC", c);
            input.put("booleanD", d);
            input.put("booleanE", e);
            input.put("booleanF", f);

            input.put("stateMachine", StateMachineToGraphviz.toGraphviz(crySLRules.get(i).getUsagePattern()));

            // 2.2. Get the template
            Template template = null;
            if (DocSettings.getInstance().getRulesetPathDir() != null) {
                template = cfg.getTemplate(Utils.pathForTemplates(DocSettings.getInstance().getFtlTemplatesPath() + "/" + "singleclass.ftl"));
            } else {
                template = cfg.getTemplate(CrySLReader.readFTLFromJar("singleclass.ftl").getPath());
            }

            // create composedRules directory where single pages are stored
            new File(DocSettings.getInstance().getReportDirectory() + "/" + "composedRules/").mkdir();
            // create the page
            try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + "/" + "composedRules/" + rule.getComposedClassName() + ".html"))) {
                template.process(input, fileWriter);
            }
        }
    }


    /**
     * sets freemarker settings
     * @param cfg
     */
    public static void setupFreeMarker(Configuration cfg) {
        // setup freemarker to load absolute paths
        cfg.setTemplateLoader(new TemplateAbsolutePathLoader());
        // Some other recommended settings:
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.ENGLISH);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    /**
     * creates core layout pages of cognicrypt doc
     * @param cfg
     * @throws IOException
     * @throws TemplateException
     */
    public static void createCogniCryptLayout(Configuration cfg) throws IOException, TemplateException {
        Map<String, Object> input = new HashMap<String, Object>();

        if (!Files.exists(Paths.get(DocSettings.getInstance().getReportDirectory()))) {
            try {
                // Attempt to create the directory
                Files.createDirectory(Paths.get(DocSettings.getInstance().getReportDirectory()));
                System.out.println("Directory created successfully.");
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + e.getMessage());
            }
        } else {
            System.out.println("Directory already exists.");
        }
        if (DocSettings.getInstance().getRulesetPathDir() != null) {
            Template frontpageTemplate = cfg.getTemplate(Utils.pathForTemplates(DocSettings.getInstance().getFtlTemplatesPath() + "/"+ "frontpage.ftl"));
            try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"frontpage.html"))) {
                frontpageTemplate.process(input, fileWriter);
            }
            Template rootpageTemplate = cfg.getTemplate(Utils.pathForTemplates(DocSettings.getInstance().getFtlTemplatesPath() + "/"+ "rootpage.ftl"));
            try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"rootpage.html"))) {
                rootpageTemplate.process(input, fileWriter);
            }
        } else {
            Template frontpageTemplate = cfg.getTemplate(CrySLReader.readFTLFromJar("frontpage.ftl").getPath());
            try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"frontpage.html"))) {
                frontpageTemplate.process(input, fileWriter);
            }
            Template rootpageTemplate = cfg.getTemplate(CrySLReader.readFTLFromJar("rootpage.ftl").getPath());
            try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"rootpage.html"))) {
                rootpageTemplate.process(input, fileWriter);
            }
        }
    }
}

