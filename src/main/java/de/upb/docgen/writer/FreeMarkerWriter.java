package de.upb.docgen.writer;

import de.upb.docgen.*;
import de.upb.docgen.utils.TemplateAbsolutePathLoader;
import de.upb.docgen.utils.TreeNode;
import de.upb.docgen.utils.Utils;
import freemarker.template.*;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;

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
        input.put("rules", composedRuleList);
        Template template = cfg.getTemplate(Utils.pathForTemplates(DocSettings.getInstance().getFtlTemplatesPath() + "/"+ "sidebar.ftl"));
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
     * @throws IOException
     * @throws TemplateException
     */
    public static void createSinglePage(List<ComposedRule> composedRuleList, Configuration cfg , Map<String, TreeNode<String>> reqToEns, Map<String, TreeNode<String>> ensToReq, boolean a, boolean b, boolean c , boolean d , boolean e) throws IOException, TemplateException {
        for (ComposedRule rule : composedRuleList) {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("title", "class");
            input.put("rule", rule);
            TreeNode<String> rootReq = reqToEns.get(rule.getComposedClassName());
            input.put("requires", rootReq); //requires tree parsed by the template
            TreeNode<String> rootEns = ensToReq.get(rule.getComposedClassName());
            input.put("ensures", rootEns); //ensures tree parsed by the template

            //necessary input for the template to load abslote path from crysl rule which can be displayed
            input.put("pathToRules", Utils.pathForTemplates("file://"+DocSettings.getInstance().getRulesetPathDir()));
            //Set flags
            input.put("booleanA", a); //To show StateMachineGraph
            input.put("booleanB", b); //To show Help Button
            input.put("booleanC", c);
            input.put("booleanD", d);
            input.put("booleanE", e);

            // 2.2. Get the template
            Template template = cfg.getTemplate(Utils.pathForTemplates(DocSettings.getInstance().getFtlTemplatesPath() + "/"+"singlepage.ftl"));

            //create composedRules directory where single pages are stored
            new File(DocSettings.getInstance().getReportDirectory()+"/"+"composedRules/").mkdir();
            //create the page
            try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory()+"/"+"composedRules/"+rule.getComposedClassName()+".html"))) {
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
        Template headerTemplate = cfg.getTemplate(Utils.pathForTemplates(DocSettings.getInstance().getFtlTemplatesPath() + "/"+ "header.ftl"));
        try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"header.html"))) {
            headerTemplate.process(input, fileWriter);
        }
        Template frontpageTemplate = cfg.getTemplate(Utils.pathForTemplates(DocSettings.getInstance().getFtlTemplatesPath() + "/"+ "frontpage.ftl"));
        try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"frontpage.html"))) {
            frontpageTemplate.process(input, fileWriter);
        }
        Template rootpageTemplate = cfg.getTemplate(Utils.pathForTemplates(DocSettings.getInstance().getFtlTemplatesPath() + "/"+ "rootpage.ftl"));
        try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"rootpage.html"))) {
            rootpageTemplate.process(input, fileWriter);
        }
    }
}

