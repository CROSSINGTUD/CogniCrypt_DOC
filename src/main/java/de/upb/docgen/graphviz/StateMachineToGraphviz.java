package de.upb.docgen.graphviz;

import crypto.rules.*;
import de.upb.docgen.DocSettings;
import de.upb.docgen.crysl.CrySLReader;
import de.upb.docgen.utils.Constant;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Sven Feldmann
 */

public class StateMachineToGraphviz {


    //To run this process on his own not used in the actual generation process
    public static void main(String[] args) throws IOException {
        Map<File, CrySLRule> rules = CrySLReader.readRulesFromSourceFiles(Constant.rulePath);
        for (Map.Entry<File, CrySLRule> ruleEntry : rules.entrySet()) {
            rulesOrderSectionToDot(ruleEntry.getValue());
            toPNG(ruleEntry.getValue().getClassName());
        }
    }
    //Runs the translation and generation of PNG for every rule
    public static void generateGraphvizStateMachines(String pathToCryslRules, String pathToRootpage) throws IOException {
        Map<File, CrySLRule> rules = CrySLReader.readRulesFromSourceFiles(pathToCryslRules);
        new File(pathToRootpage+"/"+"dotFSMs/").mkdir();
        for (Map.Entry<File, CrySLRule> ruleEntry : rules.entrySet()) {
            rulesOrderSectionToDot(ruleEntry.getValue(), pathToRootpage);
            toPNG(ruleEntry.getValue().getClassName(), pathToRootpage);
        }
    }

    //creates DOT files that are used to create the PNGs
    private static void rulesOrderSectionToDot(CrySLRule rule, String pathToRootpage) throws IOException {
        StateMachineGraph smg = rule.getUsagePattern();
        String fsm = toGraphviz(smg);
        String path = pathToRootpage+"/"+"dotFSMs/" + rule.getClassName() + ".dot";
        File output = new File(path);
        FileWriter writer = new FileWriter(output);
        writer.write(fsm);
        writer.flush();
        writer.close();

    }

    private static void rulesOrderSectionToDot(CrySLRule rule) throws IOException {
            StateMachineGraph smg = rule.getUsagePattern();
            String fsm = toGraphviz(smg);
            String path = "dotFSMs/" + rule.getClassName() + ".dot";
            File output = new File(path);
            FileWriter writer = new FileWriter(output);
            writer.write(fsm);
            writer.flush();
            writer.close();

    }

    //Translates the state machine provided by a CrySL rule into DOT syntax of Graphviz
    public static String toGraphviz(StateMachineGraph smg) {
        StringBuilder stringBuilderToFile = new StringBuilder();
        stringBuilderToFile.append("digraph fsm {\n" +
                "rankdir=LR;\n"+
                "graph[bgcolor=transparent]");
        List<TransitionEdge> edges = smg.getEdges();
        StringBuilder acceptingStates = new StringBuilder("node [shape = doublecircle];");
        String States = "node [shape = circle];\n";
        for (StateNode node : smg.getNodes()) {
            if (node.getAccepting()) {
               if (node.isInitialState()) acceptingStates.append(" ").append("Start");
               else acceptingStates.append(" ").append(node.getName());
            }
        }
        stringBuilderToFile.append(acceptingStates.toString()).append(";\n");
        stringBuilderToFile.append(States);

        for (TransitionEdge edge : edges) {
            if (edge.getLeft().getName().equals("-1")) stringBuilderToFile.append("Start").append(" -> ").append(edge.getRight().getName());
            else stringBuilderToFile.append(edge.getLeft().getName()).append(" -> ").append(edge.getRight().getName());
            stringBuilderToFile.append(" [label = ");
            stringBuilderToFile.append("\"");
            for (CrySLMethod label : edge.getLabel()) {
                //if booleanG flag is parsed use fully qualified name as edge label
                String labelName = !DocSettings.getInstance().isBooleanG() ? label.getName() : getShortName(label);
                for (Map.Entry<String,String> method: label.getParameters()) {
                    if (!method.getValue().equals("AnyType")) {
                        if (DocSettings.getInstance().isBooleanG()) {
                            labelName = labelName.replace(method.getKey(), method.getValue().substring(method.getValue().lastIndexOf(".") + 1));
                        } else {
                            labelName = labelName.replace(method.getKey(), method.getValue());
                        }
                    }
                }
                stringBuilderToFile.append(labelName);
            }
            stringBuilderToFile.append("\"");
            stringBuilderToFile.append("];\n");
        }
        stringBuilderToFile.append("}");

        return stringBuilderToFile.toString();


    }

    private static String getShortName(CrySLMethod label) {
        StringBuilder stmntBuilder = new StringBuilder();
        String returnValue = (String)label.getRetObject().getKey();
        if (!"_".equals(returnValue)) {
            stmntBuilder.append(returnValue);
            stmntBuilder.append(" = ");
        }

        stmntBuilder.append(label.getShortMethodName());
        stmntBuilder.append("(");
        Iterator paramIter = label.getParameters().iterator();

        while(paramIter.hasNext()) {
            Map.Entry<String, String> par = (Map.Entry)paramIter.next();
            stmntBuilder.append((String)par.getKey());
            if (paramIter.hasNext()) stmntBuilder.append(", ");
        }

        stmntBuilder.append("); ");
        return stmntBuilder.toString();
    }

    public static void toPNG(String name) {
        try {    MutableGraph g = new Parser().read(new File("dotFSMs\\" + name +".dot"));
            Graphviz.fromGraph(g).render(Format.SVG).toFile(new File("dotFSMs/" +name +".svg"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //reads the dot translation and creates a png file that is later used by the FTL template
    public static void toPNG(String name, String pathToRootpage) {
        try {    MutableGraph g = new Parser().read(new File(pathToRootpage+"\\"+"dotFSMs\\" + name +".dot"));
            Graphviz.fromGraph(g).render(Format.SVG).toFile(new File(pathToRootpage+"/"+"dotFSMs/" +name +".svg"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
