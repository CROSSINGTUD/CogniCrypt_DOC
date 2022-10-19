package de.upb.docgen.writer;

import de.upb.docgen.*;
import de.upb.docgen.utils.FTLTemplateLoaderFromJar;
import de.upb.docgen.utils.TemplateAbsolutePathLoader;
import de.upb.docgen.utils.TreeNode;
import de.upb.docgen.utils.Utils;
import freemarker.template.*;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
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
        input.put("rules", composedRuleList);
        File sidebarFile;
        String pathToFTLTemplatesFolder;
        if (DocSettings.getInstance().getLangTemplatesPath() == null) {
            sidebarFile = FTLTemplateLoaderFromJar.readFtlTemplateFromJar("sidebar.ftl");
        } else {
            pathToFTLTemplatesFolder = DocSettings.getInstance().getLangTemplatesPath();
            sidebarFile = new File(pathToFTLTemplatesFolder+"/sidebar.ftl");
        }
        Template template = cfg.getTemplate(sidebarFile.getName());
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
    public static void createSinglePage(List<ComposedRule> composedRuleList, Configuration cfg, Map<String, TreeNode<String>> reqToEns, Map<String, TreeNode<String>> ensToReq, boolean a, boolean b, boolean c, boolean d, boolean e, boolean f) throws IOException, TemplateException {
        for (ComposedRule rule : composedRuleList) {
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("title", "class");
            input.put("rule", rule);
            TreeNode<String> rootReq = reqToEns.get(rule.getComposedClassName());
            input.put("requires", rootReq); //requires tree parsed by the template
            TreeNode<String> rootEns = ensToReq.get(rule.getComposedClassName());
            input.put("ensures", rootEns); //ensures tree parsed by the template
            //Set flags
            input.put("booleanA", a); //To show StateMachineGraph
            input.put("booleanB", b); //To show Help Button
            input.put("booleanC", c);
            input.put("booleanD", d);
            input.put("booleanE", e);
            input.put("booleanF", f);

            File singleclassFile;
            String pathToFTLTemplatesFolder;
            if (DocSettings.getInstance().getFtlTemplatesPath() == null) {
                singleclassFile = FTLTemplateLoaderFromJar.readFtlTemplateFromJar("singleclass.ftl");
            } else {
                pathToFTLTemplatesFolder = DocSettings.getInstance().getFtlTemplatesPath();
                singleclassFile = new File(pathToFTLTemplatesFolder+"/singleclass.ftl");
            }
            Template template = cfg.getTemplate(singleclassFile.getName());

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
        // Recommended settings:
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

        File frontpageFile;
        String pathToFTLTemplatesFolder;
        if (DocSettings.getInstance().getFtlTemplatesPath() == null) {
            frontpageFile = FTLTemplateLoaderFromJar.readFtlTemplateFromJar( "frontpage.ftl");
        } else {
            pathToFTLTemplatesFolder = DocSettings.getInstance().getFtlTemplatesPath();
            frontpageFile = new File(pathToFTLTemplatesFolder+"/frontpage.ftl");
        }
        File templateDir = frontpageFile.getParentFile();
        if ( null == templateDir ){
            templateDir = new File("./");
        }
        cfg.setDirectoryForTemplateLoading(templateDir);
        Template frontpageTemplate = cfg.getTemplate(frontpageFile.getName());
        try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"frontpage.html"))) {
            frontpageTemplate.process(input, fileWriter);
        }
        File rootpageFile;
        if (DocSettings.getInstance().getFtlTemplatesPath() == null) {
            rootpageFile = FTLTemplateLoaderFromJar.readFtlTemplateFromJar("rootpage.ftl");
        } else {
            pathToFTLTemplatesFolder = DocSettings.getInstance().getFtlTemplatesPath();
            rootpageFile = new File(pathToFTLTemplatesFolder+"/rootpage.ftl");
        }
        Template rootpageTemplate = cfg.getTemplate(rootpageFile.getName());
        try (Writer fileWriter = new FileWriter(new File(DocSettings.getInstance().getReportDirectory() + File.separator+"rootpage.html"))) {
            rootpageTemplate.process(input, fileWriter);
        }
    }
}

