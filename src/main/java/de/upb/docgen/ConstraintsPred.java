package de.upb.docgen;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLRule;
import de.upb.docgen.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Ritika Singh
 */

public class ConstraintsPred {

    static PrintWriter out;

    private static char[] getTemplatePredOne() throws IOException {
        return Utils.getTemplatesText("ConstraintPredTypeOne");
    }

    private static char[] getTemplatePredTwo() throws IOException {
        return Utils.getTemplatesText("ConstraintPredTypeTwo");
    }

    private static char[] getTemplatePredThree() throws IOException {
        return Utils.getTemplatesText("ConstraintPredTypeThree");
    }

    private static char[] getTemplatePredFour() throws IOException {
        return Utils.getTemplatesText("ConstraintPredTypeFour");
    }

    private static char[] getTemplatePred1Con() throws IOException {
        return Utils.getTemplatesText("ConstraintPredType1Con");
    }

    private static char[] getTemplatePred2Con() throws IOException {
        return Utils.getTemplatesText("ConstraintPredType2Con");
    }

    private static char[] getTemplatePred3Con() throws IOException {
        return Utils.getTemplatesText("ConstraintPredType3Con");
    }

    private static char[] getTemplatePred4Con() throws IOException {
        return Utils.getTemplatesText("ConstraintPredType4Con");
    }

    public ArrayList<String> getConstraintsPred(CrySLRule rule, Set<String> ensuresForThis,
            Map<String, List<Map<String, List<String>>>> singleRuleEnsuresMap) throws IOException {
        ArrayList<String> composedConstraintsPredicates = new ArrayList<>();
        List<String> methodsList = FunctionUtils.getEventNamesKey(rule);
        List<String> valueList = FunctionUtils.getEventNamesValue(rule);
        Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);
        List<Entry<String, String>> dataTypes = rule.getObjects();
        Map<String, String> DTMap = new LinkedHashMap<>();
        for (Entry<String, String> dt : dataTypes) {
            DTMap.put(dt.getKey(), dt.getValue());
        }
        String classnamecheck = rule.getClassName().substring(rule.getClassName().lastIndexOf('.') + 1);
        List<ISLConstraint> constraintPredList = rule.getConstraints().stream()
                .filter(e -> e.getClass().getSimpleName().contains("CrySLPredicate") && !e.toString().contains("!"))
                .collect(Collectors.toList());

        if (constraintPredList.size() > 0) {

            for (ISLConstraint consPredStr : constraintPredList) {
                String crySLPredicateType = null;
                String predicate = null;
                String attachedPredicate = null;
                if (consPredStr instanceof CrySLPredicate) {
                    crySLPredicateType = ((CrySLPredicate) consPredStr).getPredName();
                    predicate = ((CrySLPredicate) consPredStr).getParameters().get(0).getName();
                    if (((CrySLPredicate) consPredStr).getParameters().size() > 1) {
                        attachedPredicate = ((CrySLPredicate) consPredStr).getParameters().get(1).getName();
                    }

                }

                Multimap<String, String> var2MethNameMap = ArrayListMultimap.create();
                Multimap<String, String> var2paraPosMap = ArrayListMultimap.create();

                String predicatePosWordsStr;

                String camelCasePattern = "([a-z]+[A-Z]+\\w+)+";

                List<String> resList = new ArrayList<>();

                int iterationCount = 0;
                for (String methodStr : methodsList) {

                    String escapedPredicate = "\\b" + predicate + "\\b";
                    Pattern p = Pattern.compile(escapedPredicate, Pattern.CASE_INSENSITIVE);
                    Matcher mt = p.matcher(methodStr);

                    if (mt.find()) {

                        List<String> methList = new ArrayList<>();
                        methList.add(methodStr);

                        for (String m : methList) {

                            List<String> extractParamList = new ArrayList<>();
                            int startIndex = m.indexOf("(");
                            int endIndex = m.indexOf(")");
                            String bracketExtractStr = m.substring(startIndex + 1, endIndex);

                            if (bracketExtractStr.contains(",")) {
                                String[] elements = bracketExtractStr.split(",");
                                Collections.addAll(extractParamList, elements);
                            } else {
                                extractParamList.add(bracketExtractStr);
                            }

                            for (String extractParamStr : extractParamList) {
                                if (extractParamStr.equals("_"))
                                    continue;
                                String value = DTMap.get(extractParamStr);
                                m = m.replace(extractParamStr, value);
                            }

                            String methStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
                            List<String> splitMethList = Arrays.asList(methStr.split(" "));
                            String posStr = String.valueOf(splitMethList.indexOf(predicate));
                            var2MethNameMap.put(predicate, replaceAnyType(valueList.get(iterationCount)));
                            var2paraPosMap.put(predicate, posStr);
                            iterationCount++;
                        }
                    }
                }

                List<String> positionOfParameterList = new ArrayList<>();
                List<String> methodList = new ArrayList<>();

                for (Map.Entry<String, String> paraPosentry : var2paraPosMap.entries()) {
                    // format: paramter \n number
                    List<String> parameterAndPositionList = Arrays.asList(paraPosentry.toString().split("="));
                    if (parameterAndPositionList.get(0).equals(predicate)) {
                        if (posInWordsMap.containsKey(parameterAndPositionList.get(1))) {
                            predicatePosWordsStr = posInWordsMap.get(parameterAndPositionList.get(1));
                            Collections.replaceAll(parameterAndPositionList, parameterAndPositionList.get(1),
                                    predicatePosWordsStr);
                        }
                        positionOfParameterList.add(parameterAndPositionList.get(1));
                    }
                }

                for (Map.Entry<String, String> paraMethentry : var2MethNameMap.entries()) {

                    List<String> parameterAndMethodList = Arrays.asList(paraMethentry.toString().split("="));
                    if (parameterAndMethodList.get(0).equals(predicate)) {
                        methodList.add(parameterAndMethodList.get(1));
                    }
                }

                for (int i = 0; i < positionOfParameterList.size(); i++) {
                    resList.add(positionOfParameterList.get(i) + "|" + methodList.get(i));
                }

                for (String rl : resList) {

                    String l1 = rl;
                    List<String> ls = Arrays.asList(l1.split("\\|"));
                    String paraPos = ls.get(0);
                    String mname = ls.get(1);

                    List<String> msplit = Arrays.asList(ls.get(1).split("\\("));

                    if (crySLPredicateType.matches(camelCasePattern)) {

                        String str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(crySLPredicateType),
                                ' ');
                        List<String> verbOrNounList = Arrays.asList(str.split("\\s"));
                        String verb = verbOrNounList.get(0);
                        List<String> noun = verbOrNounList.subList(1, verbOrNounList.size());
                        String nouns = String.join(" ", noun);
                        List<Map<String, List<String>>> ensuresOfThisClassWithVariableName = singleRuleEnsuresMap
                                .get(rule.getClassName());
                        for (Map<String, List<String>> maps : ensuresOfThisClassWithVariableName) {
                            if (maps.containsKey(crySLPredicateType)) {
                                maps.get(crySLPredicateType).get(0);
                            }
                        }
                        // this links the right class which ensures something for the current rule
                        // nouns = "<a href=\"" + ensures +".html\">" + nouns + "</a>";
                        for (Map<String, List<String>> maps : ensuresOfThisClassWithVariableName) {
                            if (maps.containsKey(crySLPredicateType)) {
                                nouns = "<span class=\"tooltip\">" + nouns;
                                String tooltiptext = "<span class=\"tooltiptext\">The following classes ensure this predicate:\n";
                                String classesLinks = htmlLinksClass(singleRuleEnsuresMap.get(rule.getClassName()),
                                        crySLPredicateType);
                                String end = "</span></span>";

                                nouns += tooltiptext + classesLinks + end;
                            }
                        }

                        if (verb.endsWith("ed")) {
                            char[] template;
                            if (msplit.get(0).equals(classnamecheck)) {
                                template = getTemplatePred1Con();
                            } else {
                                template = getTemplatePredOne();
                            }

                            Map<String, String> valuesMap = new HashMap<>();
                            valuesMap.put("position", paraPos);
                            valuesMap.put("methodName", mname);
                            valuesMap.put("verb", verb);
                            valuesMap.put("nouns", nouns);

                            StringSubstitutor sub = new StringSubstitutor(valuesMap);
                            String resolvedString = sub.replace(new String(template));
                            composedConstraintsPredicates.add(resolvedString);
                        }
                        char[] template = null;

                        if (attachedPredicate != null && attachedPredicate.equals("java.lang.String")) {
                            template = msplit.get(0).equals(classnamecheck) ? getTemplatePred2Con()
                                    : getTemplatePredTwo();
                        } else if (attachedPredicate == null) {
                            template = msplit.get(0).equals(classnamecheck) ? getTemplatePred3Con()
                                    : getTemplatePredThree();
                        }

                        if (template != null) {
                            Map<String, String> valuesMap = new HashMap<>();
                            valuesMap.put("position", paraPos);
                            valuesMap.put("methodName", mname);
                            if (attachedPredicate != null) {
                                valuesMap.put("var3", attachedPredicate);
                            }

                            StringSubstitutor sub = new StringSubstitutor(valuesMap);
                            String resolvedString = sub.replace(new String(template));
                            composedConstraintsPredicates.add(resolvedString);
                        }

                    } else {
                        List<Map<String, List<String>>> ensuresOfThisClassWithVariableName = singleRuleEnsuresMap
                                .get(rule.getClassName());
                        for (Map<String, List<String>> maps : ensuresOfThisClassWithVariableName) {
                            if (maps.containsKey(crySLPredicateType)) {
                                String classToLink = crySLPredicateType;
                                crySLPredicateType = "<span class=\"tooltip\">" + crySLPredicateType;
                                String tooltiptext = "<span class=\"tooltiptext\">The following classes ensure this predicate:\n";
                                String classesLinks = htmlLinksClass(singleRuleEnsuresMap.get(rule.getClassName()),
                                        classToLink);
                                String end = "</span></span>";

                                crySLPredicateType += tooltiptext + classesLinks + end;
                            }
                        }

                        if (msplit.get(0).equals(classnamecheck)) {
                            char[] sFour = getTemplatePred4Con();
                            Map<String, String> valuesMap = new HashMap<String, String>();
                            valuesMap.put("position", paraPos);
                            valuesMap.put("methodName", mname);
                            valuesMap.put("var1", crySLPredicateType);
                            StringSubstitutor sub = new StringSubstitutor(valuesMap);
                            String resolvedString = sub.replace(sFour);
                            composedConstraintsPredicates.add(resolvedString);
                        } else {
                            char[] sFour = getTemplatePredFour();
                            Map<String, String> valuesMap = new HashMap<String, String>();
                            valuesMap.put("position", paraPos);
                            valuesMap.put("methodName", mname);
                            valuesMap.put("var1", crySLPredicateType);
                            StringSubstitutor sub = new StringSubstitutor(valuesMap);
                            String resolvedString = sub.replace(sFour);
                            composedConstraintsPredicates.add(resolvedString);
                        }
                    }
                }
            }
        }
        return composedConstraintsPredicates;
    }

    private String htmlLinksClass(List<Map<String, List<String>>> maps, String var1) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, List<String>> map : maps) {
            if (map.containsKey(var1)) {
                for (String className : map.get(var1)) {
                    sb.append("<a href=\"").append(className).append(".html\">").append(className).append("</a>\n");
                }
            }
        }
        return sb.toString();
    }

    private String replaceAnyType(String inputString) {
        if (inputString.contains("AnyType")) {
            return inputString.replace("AnyType", "_");
        } else {
            return inputString;
        }
    }
}