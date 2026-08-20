package de.upb.docgen.graphviz;

import crypto.rules.*;
import de.upb.docgen.DocSettings;
import java.util.*;

/**
 * @author Sven Feldmann
 */

public class StateMachineToGraphviz {


    // Translates the state machine provided by a CrySL rule into DOT syntax of Graphviz.
    /**
     * Render a CrySL state machine as Graphviz DOT.
     */
    public static String toGraphviz(StateMachineGraph smg) {
        StringBuilder stringBuilderToFile = new StringBuilder();
        // Graph header and global styling.
        stringBuilderToFile.append("digraph fsm {\n" +
                "rankdir=LR;\n"+
                "graph[bgcolor=transparent]");
        List<TransitionEdge> edges = smg.getEdges();
        StringBuilder acceptingStates = new StringBuilder("node [shape = doublecircle];");
        String States = "node [shape = circle];\n";
        stringBuilderToFile.append("edge [labeldistance=2.5, labelangle=45];");
        // Mark accepting states with a double circle (use Start for initial+accepting).
        for (StateNode node : smg.getNodes()) {
            if (node.getAccepting()) {
               if (node.isInitialState()) acceptingStates.append(" ").append("Start");
               else acceptingStates.append(" ").append(node.getName());
            }
        }
        stringBuilderToFile.append(acceptingStates.toString()).append(";\n");
        stringBuilderToFile.append(States);

        // Render transitions with aggregated labels.
        for (TransitionEdge edge : edges) {
            if (edge.getLeft().getName().equals("-1")) stringBuilderToFile.append("Start").append(" -> ").append(edge.getRight().getName());
            else stringBuilderToFile.append(edge.getLeft().getName()).append(" -> ").append(edge.getRight().getName());
            stringBuilderToFile.append(" [label = ");
            stringBuilderToFile.append("\"");
            List<String> uniqueLabels = new ArrayList<>(); // keeps insertion order, no duplicates

            for (CrySLMethod label : edge.getLabel()) {
                // If booleanG flag is parsed, use fully qualified name as edge label.
                boolean fullyQualified = !DocSettings.getInstance().isBooleanG();
                String labelName = buildLabel(label, fullyQualified);

                // Check if the label is not already present in uniqueLabels
                String trimmedLabel = labelName.trim();
                if (!trimmedLabel.isEmpty() && !uniqueLabels.contains(trimmedLabel)) {
                    uniqueLabels.add(trimmedLabel);
                }
            }
            // Explicit separator: an alternation edge (m1 | m2 | ...) previously ran its
            // labels together, relying on a trailing space that only the short-name path had.
            stringBuilderToFile.append(String.join(" ", uniqueLabels));
            stringBuilderToFile.append("\"");
            stringBuilderToFile.append("];\n");
        }
        stringBuilderToFile.append("}");

        return stringBuilderToFile.toString();


    }

    /**
     * Shorten every qualified name inside a type expression, preserving generic structure:
     * {@code java.util.Set<java.security.cert.TrustAnchor>} becomes {@code Set<TrustAnchor>}.
     *
     * <p>Truncating at the last '.' - the previous approach - returned {@code TrustAnchor>}
     * for that type: the outer type lost and a dangling bracket left behind.
     */
    private static String toSimpleTypeName(String type) {
        return type.replaceAll("(?:[\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+([\\p{L}_$][\\p{L}\\p{N}_$]*)", "$1");
    }

    /**
     * Build a Graphviz edge label as {@code [ret = ]name(paramType, ...)}.
     *
     * <p>Built directly from the method's structured parameters. The previous approach
     * rendered parameter <em>names</em> and then globally {@code String.replace}d each name
     * with its type, which corrupted any label where a parameter name occurred elsewhere in
     * the text: {@code DSAParameterSpec(p, q, g)} with parameters {@code p}, {@code q},
     * {@code g} became
     * {@code DSAParameterSBiBigIntegerInteBigIntegererec(BiBigIntegerInteBigIntegerer, ...)}
     * because "p" and "g" also occur inside the method name and inside "BigInteger" itself.
     * It also duplicated types in the fully-qualified mode ("int int").
     */
    private static String buildLabel(CrySLMethod label, boolean fullyQualified) {
        StringBuilder stmntBuilder = new StringBuilder();
        String returnValue = label.getRetObject().getKey();
        if (!"_".equals(returnValue)) {
            stmntBuilder.append(returnValue);
            stmntBuilder.append(" = ");
        }

        stmntBuilder.append(fullyQualified ? label.getMethodName() : label.getShortMethodName());
        stmntBuilder.append("(");

        List<String> parameterTypes = new ArrayList<>();
        for (Map.Entry<String, String> parameter : label.getParameters()) {
            String type = parameter.getValue();
            if ("AnyType".equals(type)) {
                parameterTypes.add("_");
            } else if (fullyQualified) {
                parameterTypes.add(type);
            } else {
                parameterTypes.add(toSimpleTypeName(type));
            }
        }
        stmntBuilder.append(String.join(", ", parameterTypes));
        stmntBuilder.append(");");
        return stmntBuilder.toString();
    }



}
