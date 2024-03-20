package de.upb.docgen.graphviz;

import crypto.rules.*;
import de.upb.docgen.DocSettings;
import java.util.*;

/**
 * @author Sven Feldmann
 */

public class StateMachineToGraphviz {


    //To run this process on his own not used in the actual generation process
   /* public static void main(String[] args) throws IOException {
        Map<File, CrySLRule> rules = CrySLReader.readRulesFromSourceFiles(Constant.rulePath);
        for (Map.Entry<File, CrySLRule> ruleEntry : rules.entrySet()) {
            rulesOrderSectionToDot(ruleEntry.getValue());
            toPNG(ruleEntry.getValue().getClassName());
        }
    }*/
    //Runs the translation and generation of PNG for every rule

    //Translates the state machine provided by a CrySL rule into DOT syntax of Graphviz
    public static String toGraphviz(StateMachineGraph smg) {
        StringBuilder stringBuilderToFile = new StringBuilder();
        stringBuilderToFile.append("digraph fsm {\n" +
                "rankdir=LR;\n"+
                "graph[bgcolor=transparent]");
        List<TransitionEdge> edges = smg.getEdges();
        StringBuilder acceptingStates = new StringBuilder("node [shape = doublecircle];");
        String States = "node [shape = circle];\n";
        stringBuilderToFile.append("edge [labeldistance=2.5, labelangle=45];");
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
            Set<String> uniqueLabels = new HashSet<>(); // Set to keep track of unique labels

            for (CrySLMethod label : edge.getLabel()) {
                // If booleanG flag is parsed, use fully qualified name as edge label
                String labelName = !DocSettings.getInstance().isBooleanG() ? label.getName() : getShortName(label);

                for (Map.Entry<String, String> method : label.getParameters()) {
                    if (!method.getValue().equals("AnyType")) {
                        labelName = !DocSettings.getInstance().isBooleanG() ? labelName.replace(method.getKey(), method.getValue()) : labelName.replace(method.getKey(), method.getValue().substring(method.getValue().lastIndexOf(".") + 1));

                    }
                }

                // Check if the label is not already present in uniqueLabels
                if (!uniqueLabels.contains(labelName)) {
                    stringBuilderToFile.append(labelName);
                    uniqueLabels.add(labelName); // Add the label to the Set
                }
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



}
