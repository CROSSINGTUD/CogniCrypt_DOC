package de.upb.docgen;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import crypto.interfaces.ICrySLPredicateParameter;
import crypto.interfaces.ISLConstraint;
import crypto.rules.*;
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

public class ConstraintsComparison {

    static PrintWriter out;

    private static String mapToOperator(String arithOp) {
        switch (arithOp) {
            case "p":
                return "+";
            case "n":
                return "-";
            case "m":
                return "%";
            case "l":
                return "<";
            case "le":
                return "<=";
            case "g":
                return ">";
            case "ge":
                return ">=";
            case "neq":
                return "!=";
            case "eq":
                return "=";
            default:
                throw new IllegalArgumentException("Unsupported ArithOp: " + arithOp);
        }
    }

    private static char[] getTemplateCompOne() throws IOException {
        return Utils.getTemplatesText("CompConstraint_lengthgreaterequal");
    }

    private static char[] getTemplateComptwo() throws IOException {
        return Utils.getTemplatesText("CompConstraint_lengthlesssum");
    }

    private static char[] getTemplateCompThree() throws IOException {
        return Utils.getTemplatesText("CompConstraint_lengthless");
    }

    private static char[] getTemplateCompFour() throws IOException {
        return Utils.getTemplatesText("CompConstraint_greaterequal");
    }

    private static char[] getTemplateCompFive() throws IOException {
        return Utils.getTemplatesText("CompConstraint_greater");
    }

    private static char[] getTemplateCompSix() throws IOException {
        return Utils.getTemplatesText("CompConstraint_less");
    }

    private static char[] getTemplateCompSeven() throws IOException {
        return Utils.getTemplatesText("CompConstraint_lengthgreater");
    }

    private static char[] getTemplateCompCons1() throws IOException {
        return Utils.getTemplatesText("CompConstraint_lengthgreaterequalCon");
    }

    private static char[] getTemplateCompCons2() throws IOException {
        return Utils.getTemplatesText("CompConstraint_greaterequalCon");
    }

    public ArrayList<String> getConstriantsComp(CrySLRule rule) throws IOException {
        ArrayList<String> composedComparsionConstraint = new ArrayList<>();
        List<ISLConstraint> constraintCompConList = rule.getConstraints().stream()
                .filter(e -> e.getClass().getSimpleName().contains("CrySLComparisonConstraint"))
                .collect(Collectors.toList());
        List<Entry<String, String>> dataTypes = rule.getObjects();
        Map<String, String> dataTypeAndParameterNameMap = new LinkedHashMap<>();

        for (Entry<String, String> dt : dataTypes) {
            dataTypeAndParameterNameMap.put(dt.getKey(), dt.getValue());
        }

        String classnamecheck = rule.getClassName().substring(rule.getClassName().lastIndexOf('.') + 1);
        if (constraintCompConList.size() > 0) {

            List<String> subListLHS = new ArrayList<>();
            List<String> subListRHS = new ArrayList<>();
            String symbolStr;

            List<String> methods = FunctionUtils.getEventNamesKey(rule);
            Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);

            for (ISLConstraint compCon : constraintCompConList) {

                Multimap<String, String> paraMethNameMap = ArrayListMultimap.create();
                Multimap<String, String> paraPosMap = ArrayListMultimap.create();

                String compStr = compCon.toString();

                CrySLComparisonConstraint crySLComparisonConstraint = (CrySLComparisonConstraint) compCon;
                CrySLArithmeticConstraint leftArit = crySLComparisonConstraint.getLeft();
                String compStrTemp;

                if (leftArit.getLeft() instanceof CrySLPredicate) {
                    CrySLPredicate leftAritPrd = (CrySLPredicate) leftArit.getLeft();
                    List<String> leftAritPrdVarNames = new ArrayList<>();
                    for (ICrySLPredicateParameter e : leftAritPrd.getParameters()) {
                        if (e instanceof CrySLObject) {
                            CrySLObject crySLObject = (CrySLObject) e;
                            leftAritPrdVarNames.add(crySLObject.getVarName());
                        }

                    }

                    compStrTemp = leftAritPrd.getPredName() + "(" + StringUtils.join(leftAritPrdVarNames, ",") + ")";
                } else if (leftArit.getLeft() instanceof CrySLObject) {
                    // Handle the case where getLeft is a CrySLObject
                    CrySLObject leftArtObj = (CrySLObject) leftArit.getLeft();
                    compStrTemp = leftArtObj.getVarName();
                } else {
                    // Handle other cases if needed
                    compStrTemp = ""; 
                }

// Continue with the operator and rightArit
                compStrTemp += " " + mapToOperator(leftArit.getOperator().toString()) + " ";

// Repeat the similar logic for getRight
                if (leftArit.getRight() instanceof CrySLPredicate) {
                    CrySLPredicate rightAritPrd = (CrySLPredicate) leftArit.getRight();

                    List<String> rightAritPrdVarNames = new ArrayList<>();
                    for (ICrySLPredicateParameter e : rightAritPrd.getParameters()) {
                        if (e instanceof CrySLObject) {
                            CrySLObject crySLObject = (CrySLObject) e;
                            rightAritPrdVarNames.add(crySLObject.getVarName());
                        }
                    }
                    compStrTemp += rightAritPrd.getPredName() + "(" + StringUtils.join(rightAritPrdVarNames, ",") + ")";
                } else if (leftArit.getRight() instanceof CrySLObject) {
                    // Handle the case where getRight is a CrySLObject
                    CrySLObject rightArtObj = (CrySLObject) leftArit.getRight();
                    compStrTemp += rightArtObj.getVarName();
                } else {
                    // Handle other cases if needed
                    compStrTemp += ""; 
                }
                CrySLArithmeticConstraint rightArit = crySLComparisonConstraint.getRight();
                // length + ( + varnames + ) + operator + int
                // add operator
                compStrTemp += " " + mapToOperator(crySLComparisonConstraint.getOperator().toString()) + " ";
                // add rightside
                List<LeafNodeWithOperator> rightOperations = new ArrayList<>();
                ArithmeticNode root = buildTree(rightArit);
                String equation = root.buildEquation();
                collectLeafNodes(rightArit, rightOperations);
                compStrTemp += equation;
                compStr = compStrTemp;
                if (compStr.contains("length")) {
                    List<String> splitCompList = Arrays
                            .asList(compStr.replaceAll("[()]", " ").replaceAll("\\s+", " ").split(" "));
                    for (String parameterNameComp : splitCompList) {
                        for (String methodStr : methods) {
                            String substringBetween = StringUtils.substringBetween(methodStr, "(", ")");
                            String[] parameterNamesFromMethod = substringBetween.split(",");
                            for (String parameterNameFromMethod : parameterNamesFromMethod) {
                                if (parameterNameFromMethod.equals(parameterNameComp)) {
                                    String methodStrTemp = methodStr;
                                    List<String> extractParamList = new ArrayList<>();
                                    int startIndex = methodStrTemp.indexOf("(");
                                    int endIndex = methodStrTemp.indexOf(")");
                                    String bracketExtractStr = methodStrTemp.substring(startIndex + 1, endIndex);
                                    if (bracketExtractStr.contains(",")) {
                                        String[] elements = bracketExtractStr.split(",");
                                        Collections.addAll(extractParamList, elements);
                                    } else {
                                        extractParamList.add(bracketExtractStr);
                                    }
                                    for (String parameterName : extractParamList) {
                                        if (!dataTypeAndParameterNameMap.containsKey(parameterName)) {
                                        } else {
                                            String javaType = dataTypeAndParameterNameMap.get(parameterName);
                                            methodStrTemp = methodStrTemp.replaceFirst(parameterName, javaType);
                                        }
                                    }

                                    paraMethNameMap.put(parameterNameComp, methodStrTemp);

                                    String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
                                    List<String> strList = Arrays.asList(mStr.split(" "));
                                    String posStr = String.valueOf(strList.indexOf(parameterNameComp));
                                    paraPosMap.put(parameterNameComp, posStr);
                                }
                            }
                        }
                    }

                    for (int index = 0; index < splitCompList.size(); index++) {

                        if (splitCompList.contains(">=")) {

                            int indexOp = splitCompList.indexOf(">=");
                            subListLHS = splitCompList.subList(0, indexOp);
                            subListRHS = splitCompList.subList(indexOp, splitCompList.size());

                        } else if (splitCompList.contains(">")) {
                            int indexOp = splitCompList.indexOf(">");
                            subListLHS = splitCompList.subList(0, indexOp);
                            subListRHS = splitCompList.subList(indexOp, splitCompList.size());

                        } else {
                            int indexOp = splitCompList.indexOf("<");
                            subListLHS = splitCompList.subList(0, indexOp);
                            subListRHS = splitCompList.subList(indexOp, splitCompList.size());
                        }
                    }

                    String parLHSstr = subListLHS.get(1);
                    String parLHSPosStr = null;

                    List<String> elistposLHS = new ArrayList<>();
                    List<String> elisttmethLHS = new ArrayList<>();
                    List<String> resListLHS = new ArrayList<>();

                    for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraPosentry.toString().split("="));
                        if (pe.get(0).equals(parLHSstr)) {
                            if (posInWordsMap.containsKey(pe.get(1))) {
                                parLHSPosStr = posInWordsMap.get(pe.get(1));
                                Collections.replaceAll(pe, pe.get(1), parLHSPosStr);
                            }
                            elistposLHS.add(pe.get(1));
                        }
                    }

                    for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraMethentry.toString().split("="));
                        if (pe.get(0).equals(parLHSstr)) {
                            elisttmethLHS.add(pe.get(1));
                        }
                    }

                    for (int i = 0; i < elistposLHS.size(); i++) {
                        resListLHS.add(elistposLHS.get(i) + "|" + elisttmethLHS.get(i));
                    }

                    String parRHSstrOne = subListRHS.get(1);
                    String parRHSstrTwo = null;
                    String parRHSstrOnePos = null;
                    String parRHSstrTwoPos = null;

                    if (!subListRHS.get(subListRHS.size() - 3).equals("0")) {
                        parRHSstrTwo = subListRHS.get(subListRHS.size() - 3);
                    } else {

                    }

                    List<String> elistposRHSOne = new ArrayList<>();
                    List<String> elisttmethRHSOne = new ArrayList<>();
                    List<String> resListRHSOne = new ArrayList<>();

                    for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraPosentry.toString().split("="));
                        if (pe.get(0).equals(parRHSstrOne)) {
                            if (posInWordsMap.containsKey(pe.get(1))) {
                                parRHSstrOnePos = posInWordsMap.get(pe.get(1));
                                Collections.replaceAll(pe, pe.get(1), parRHSstrOnePos);
                            }
                            elistposRHSOne.add(pe.get(1));
                        }
                    }

                    for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraMethentry.toString().split("="));
                        if (pe.get(0).equals(parRHSstrOne)) {
                            elisttmethRHSOne.add(pe.get(1));
                        }
                    }

                    for (int i = 0; i < elistposRHSOne.size(); i++) {
                        resListRHSOne.add(elistposRHSOne.get(i) + "|" + elisttmethRHSOne.get(i));
                    }

                    List<String> elistposRHSTwo = new ArrayList<>();
                    List<String> elisttmethRHSTWo = new ArrayList<>();
                    List<String> resListRHSTwo = new ArrayList<>();

                    for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraPosentry.toString().split("="));
                        if (pe.get(0).equals(parRHSstrTwo)) {
                            if (posInWordsMap.containsKey(pe.get(1))) {
                                parRHSstrTwoPos = posInWordsMap.get(pe.get(1));
                                Collections.replaceAll(pe, pe.get(1), parRHSstrTwoPos);
                            }
                            elistposRHSTwo.add(pe.get(1));
                        }
                    }

                    for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraMethentry.toString().split("="));
                        if (pe.get(0).equals(parRHSstrTwo)) {
                            elisttmethRHSTWo.add(pe.get(1));
                        }
                    }

                    for (int i = 0; i < elistposRHSTwo.size(); i++) {
                        resListRHSTwo.add(elistposRHSTwo.get(i) + "|" + elisttmethRHSTWo.get(i));
                    }

                    // template replacement
                    symbolStr = subListRHS.get(0);

                    if (resListRHSTwo.size() > 0) {

                        for (int i = 0; i < resListLHS.size(); i++) {

                            List<String> newLHSList = Arrays.asList(resListLHS.get(i).split("\\|"));
                            String varposLHS = newLHSList.get(0);
                            String methLHS = newLHSList.get(1);
                            List<String> msplit = Arrays.asList(methLHS.split("\\("));

                            for (int j = 0; j < resListRHSOne.size(); j++) {

                                List<String> newRHSListOne = Arrays.asList(resListRHSOne.get(j).split("\\|"));
                                List<String> newRHSListTwo = Arrays.asList(resListRHSTwo.get(j).split("\\|"));

                                String varposRHSOne = newRHSListOne.get(0);
                                String methRHSOne = newRHSListOne.get(1);

                                String varposRHSTwo = newRHSListTwo.get(0);
                                String methRHSTwo = newRHSListTwo.get(1);

                                if (symbolStr.equals(">=")) {

                                    if (msplit.get(0).contains(classnamecheck)) {

                                        char[] strOne = getTemplateCompCons1();
                                        Map<String, String> valuesMap = new HashMap<String, String>();
                                        valuesMap.put("positionLHS", varposLHS);
                                        valuesMap.put("LHSMethod", methLHS);
                                        valuesMap.put("positionRHSOne", varposRHSOne);
                                        valuesMap.put("RHSOnemethodName", methRHSOne);
                                        valuesMap.put("positionRHSTwo", varposRHSTwo);
                                        valuesMap.put("RHSTwomethodName", methRHSTwo);
                                        StringSubstitutor sub = new StringSubstitutor(valuesMap);
                                        String resolvedString = sub.replace(strOne);
                                        composedComparsionConstraint.add(resolvedString);
                                    } else {
                                        char[] strOne = getTemplateCompOne();
                                        Map<String, String> valuesMap = new HashMap<String, String>();
                                        valuesMap.put("positionLHS", varposLHS);
                                        valuesMap.put("LHSMethod", methLHS);
                                        valuesMap.put("positionRHSOne", varposRHSOne);
                                        valuesMap.put("RHSOnemethodName", methRHSOne);
                                        valuesMap.put("positionRHSTwo", varposRHSTwo);
                                        valuesMap.put("RHSTwomethodName", methRHSTwo);
                                        StringSubstitutor sub = new StringSubstitutor(valuesMap);
                                        String resolvedString = sub.replace(strOne);
                                        composedComparsionConstraint.add(resolvedString);
                                    }

                                } else if (symbolStr.equals("<")) {

                                    char[] strTwo = getTemplateComptwo();
                                    Map<String, String> valuesMap = new HashMap<String, String>();
                                    valuesMap.put("positionLHS", varposLHS);
                                    valuesMap.put("LHSMethod", methLHS);
                                    valuesMap.put("positionRHSOne", varposRHSOne);
                                    valuesMap.put("RHSOnemethodName", methRHSOne);
                                    valuesMap.put("positionRHSTwo", varposRHSTwo);
                                    valuesMap.put("RHSTwomethodName", methRHSTwo);
                                    StringSubstitutor sub = new StringSubstitutor(valuesMap);
                                    String resolvedString = sub.replace(strTwo);
                                    composedComparsionConstraint.add(resolvedString);

                                    //out.println(resolvedString);
                                }
                            }
                        }
                    } else {

                        for (int i = 0; i < resListLHS.size(); i++) {

                            List<String> newLHSList = Arrays.asList(resListLHS.get(i).split("\\|"));
                            String varposLHS = newLHSList.get(0);
                            String methLHS = newLHSList.get(1);

                            for (int j = 0; j < resListRHSOne.size(); j++) {

                                List<String> newRHSList = Arrays.asList(resListRHSOne.get(j).split("\\|"));
                                String varposRHS = newRHSList.get(0);
                                String methRHS = newRHSList.get(1);

                                if (symbolStr.equals("<")) {

                                    char[] strThree = getTemplateCompThree();
                                    Map<String, String> valuesMap = new HashMap<String, String>();
                                    valuesMap.put("positionLHS", varposLHS);
                                    valuesMap.put("LHSMethod", methLHS);
                                    valuesMap.put("positionRHSOne", varposRHS);
                                    valuesMap.put("RHSOnemethodName", methRHS);
                                    StringSubstitutor sub = new StringSubstitutor(valuesMap);
                                    String resolvedString = sub.replace(strThree);
                                    composedComparsionConstraint.add(resolvedString);


                                } else if (symbolStr.equals(">")) {

                                    char[] strThree = getTemplateCompSeven();
                                    Map<String, String> valuesMap = new HashMap<String, String>();
                                    valuesMap.put("positionLHS", varposLHS);
                                    valuesMap.put("LHSMethod", methLHS);
                                    valuesMap.put("positionRHSOne", varposRHS);
                                    valuesMap.put("RHSOnemethodName", methRHS);
                                    StringSubstitutor sub = new StringSubstitutor(valuesMap);
                                    String resolvedString = sub.replace(strThree);
                                    composedComparsionConstraint.add(resolvedString);

                                }
                            }
                        }
                    }
                }

                // length ends
                else {

                    /* remaining sublist, check second par - numeric or alpha */
                    List<String> splitCompListTwo = Arrays.asList(compStr.replaceFirst("^0+(?!$)", "").split(" "));

                    for (String splitCompTwoStr : splitCompListTwo) {

                        for (String methodStr : methods) {

                            String result = StringUtils.substringBetween(methodStr, "(", ")");
                            String[] resList = result.split(","); // params

                            for (String r : resList) {

                                if (r.equals(splitCompTwoStr)) {

                                    String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
                                    List<String> strList = Arrays.asList(mStr.split(" "));
                                    String posStr = String.valueOf(strList.indexOf(splitCompTwoStr));

                                    paraPosMap.put(splitCompTwoStr, posStr);

                                    String m = methodStr;
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

                                        if (!dataTypeAndParameterNameMap.containsKey(extractParamStr)) {

                                        } else {
                                            int startInd = 0;
                                            int endInd = 0;
                                            String value = dataTypeAndParameterNameMap.get(extractParamStr);

                                            Pattern word = Pattern.compile(extractParamStr);
                                            Matcher match = word.matcher(m);

                                            while (match.find()) {
                                                startInd = match.start();
                                                endInd = match.end() - 1;
                                            }
                                            String strDiv = m.substring(startInd, endInd + 1);
                                            if (strDiv.equals(extractParamStr)) {
                                                StringBuilder sDB = new StringBuilder(m);
                                                sDB.replace(startInd, endInd + 1, value);
                                                m = sDB.toString();
                                            }
                                        }
                                    }

                                    paraMethNameMap.put(splitCompTwoStr, m);
                                    break;
                                }
                            }
                        }
                    }

                    for (int index = 0; index < splitCompListTwo.size(); index++) {

                        if (splitCompListTwo.contains(">")) {

                            int indexOp = splitCompListTwo.indexOf(">");
                            subListLHS = splitCompListTwo.subList(0, indexOp);
                            subListRHS = splitCompListTwo.subList(indexOp, splitCompListTwo.size());

                        } else if (splitCompListTwo.contains("<")) {

                            int indexOp = splitCompListTwo.indexOf("<");
                            subListLHS = splitCompListTwo.subList(0, indexOp);
                            subListRHS = splitCompListTwo.subList(indexOp, splitCompListTwo.size());
                        } else if (splitCompListTwo.contains(">=")) {

                            int indexOp = splitCompListTwo.indexOf(">=");
                            subListLHS = splitCompListTwo.subList(0, indexOp);
                            subListRHS = splitCompListTwo.subList(indexOp, splitCompListTwo.size());
                        } else {
                            int indexOp = splitCompListTwo.indexOf("!=");
                            subListLHS = splitCompListTwo.subList(0, indexOp);
                            subListRHS = splitCompListTwo.subList(indexOp, splitCompListTwo.size());
                        }
                    }

                    String paramLhsStr = subListLHS.get(0);
                    String paramLhsPosStr = null;

                    List<String> elistposLhs = new ArrayList<>();
                    List<String> elisttmethLhs = new ArrayList<>();
                    List<String> resListLhs = new ArrayList<>();

                    for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraPosentry.toString().split("="));
                        if (pe.get(0).equals(paramLhsStr)) {
                            if (posInWordsMap.containsKey(pe.get(1))) {
                                paramLhsPosStr = posInWordsMap.get(pe.get(1));
                                Collections.replaceAll(pe, pe.get(1), paramLhsPosStr);
                            }
                            elistposLhs.add(pe.get(1));
                        }
                    }

                    for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraMethentry.toString().split("="));
                        if (pe.get(0).equals(paramLhsStr)) {
                            elisttmethLhs.add(pe.get(1));
                        }
                    }

                    for (int i = 0; i < elistposLhs.size(); i++) {
                        resListLhs.add(elistposLhs.get(i) + "|" + elisttmethLhs.get(i));
                    }

                    String paramRhsNumStr = null;
                    String paramRhsAphaStr = null;
                    String paramRhsAphaPosStr;

                    // checks for numbers
                    if (subListRHS.get(1).matches(".*[0-9].*")) {
                        paramRhsNumStr = subListRHS.get(1);
                    } else {
                        paramRhsAphaStr = subListRHS.get(1);
                    }

                    List<String> elistposRhsAlpha = new ArrayList<>();
                    List<String> elisttmethRhsAlpha = new ArrayList<>();
                    List<String> resListRhsAlpha = new ArrayList<>();

                    for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraPosentry.toString().split("="));
                        if (pe.get(0).equals(paramRhsAphaStr)) {
                            if (posInWordsMap.containsKey(pe.get(1))) {
                                paramRhsAphaPosStr = posInWordsMap.get(pe.get(1));
                                Collections.replaceAll(pe, pe.get(1), paramRhsAphaPosStr);
                            }
                            elistposRhsAlpha.add(pe.get(1));
                        }
                    }

                    for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

                        List<String> pe = new ArrayList<>();
                        pe = Arrays.asList(paraMethentry.toString().split("="));
                        if (pe.get(0).equals(paramRhsAphaStr)) {
                            elisttmethRhsAlpha.add(pe.get(1));
                        }
                    }

                    for (int i = 0; i < elistposRhsAlpha.size(); i++) {
                        resListRhsAlpha.add(elistposRhsAlpha.get(i) + "|" + elisttmethRhsAlpha.get(i));
                    }

                    symbolStr = subListRHS.get(0);

                    if (resListRhsAlpha.size() > 0) {

                        for (int i = 0; i < resListLhs.size(); i++) {

                            List<String> Lhslist = Arrays.asList(resListLhs.get(i).split("\\|"));
                            String Lhspos = Lhslist.get(0);
                            String Lhsmeth = Lhslist.get(1);

                            for (int j = 0; j < resListRhsAlpha.size(); j++) {

                                List<String> RhsAlpha = Arrays.asList(resListRhsAlpha.get(j).split("\\|"));
                                String Rhsposalpha = RhsAlpha.get(0);
                                String Rhsmethalpha = RhsAlpha.get(1);

                                if (symbolStr.equals(">")) {

                                    char[] strFive = getTemplateCompFive();
                                    Map<String, String> valuesMap = new HashMap<String, String>();
                                    valuesMap.put("paramLhsPosWordStr", Lhspos);
                                    valuesMap.put("paramLhsMethStr", Lhsmeth);
                                    valuesMap.put("paramRhsAlphaPosWordStr", Rhsposalpha);
                                    valuesMap.put("paramRhsAphaMethStr", Rhsmethalpha);
                                    StringSubstitutor sub = new StringSubstitutor(valuesMap);
                                    String resolvedString = sub.replace(strFive);
                                    composedComparsionConstraint.add(resolvedString);

                                } else if (symbolStr.equals("<")) {

                                    char[] strSix = getTemplateCompSix();
                                    Map<String, String> valuesMap = new HashMap<String, String>();
                                    valuesMap.put("paramLhsPosWordStr", Lhspos);
                                    valuesMap.put("paramLhsMethStr", Lhsmeth);
                                    valuesMap.put("paramRhsAlphaPosWordStr", Rhsposalpha);
                                    valuesMap.put("paramRhsAphaMethStr", Rhsmethalpha);

                                    StringSubstitutor sub = new StringSubstitutor(valuesMap);
                                    String resolvedString = sub.replace(strSix);
                                    composedComparsionConstraint.add(resolvedString);


                                }
                            }
                        }
                    } else {

                        for (int i = 0; i < resListLhs.size(); i++) {

                            List<String> Lhselement = Arrays.asList(resListLhs.get(i).split("\\|"));
                            String posLhs = Lhselement.get(0);
                            String methLhs = Lhselement.get(1);
                            List<String> msplit = Arrays.asList(methLhs.split("\\("));

                            if ((symbolStr.equals(">=") || symbolStr.equals(">")) && paramRhsNumStr != null) {

                                if (msplit.get(0).contains(classnamecheck)) {

                                    char[] strFour = getTemplateCompCons2();
                                    Map<String, String> valuesMap = new HashMap<String, String>();
                                    valuesMap.put("paramLhsPosWordStr", posLhs);
                                    valuesMap.put("paramLhsMethStr", methLhs);
                                    valuesMap.put("paramRhsNumStr", paramRhsNumStr);
                                    StringSubstitutor sub = new StringSubstitutor(valuesMap);
                                    String resolvedString = sub.replace(strFour);
                                    composedComparsionConstraint.add(resolvedString);


                                } else {

                                    char[] strFour = getTemplateCompFour();
                                    Map<String, String> valuesMap = new HashMap<String, String>();
                                    valuesMap.put("paramLhsPosWordStr", posLhs);
                                    valuesMap.put("paramLhsMethStr", methLhs);
                                    valuesMap.put("paramRhsNumStr", paramRhsNumStr);
                                    StringSubstitutor sub = new StringSubstitutor(valuesMap);
                                    String resolvedString = sub.replace(strFour);
                                    composedComparsionConstraint.add(resolvedString);


                                }
                            }
                        }
                    }
                }
            }
        }

        //out.close();
        return composedComparsionConstraint;
    }


    private void collectLeafNodes(CrySLArithmeticConstraint rightArit, List<LeafNodeWithOperator> rightOperations) {
        if (rightArit.getLeft() instanceof CrySLObject && rightArit.getRight() instanceof CrySLObject) {
            // Both left and right are leaf nodes
            rightOperations.add(new LeafNodeWithOperator((CrySLObject) rightArit.getLeft(), rightArit.getOperator().toString()));
            rightOperations.add(new LeafNodeWithOperator((CrySLObject) rightArit.getRight(), rightArit.getOperator().toString()));
        } else {
            // Recursively explore left and right nodes, checking for leaf nodes
            if (rightArit.getLeft() instanceof ISLConstraint) {
                ISLConstraint left = (ISLConstraint) rightArit.getLeft();
                if (left instanceof CrySLArithmeticConstraint) {
                    collectLeafNodes((CrySLArithmeticConstraint) left, rightOperations);
                } else {
                    // Handle if left is a leaf node (CrySLObject or other leaf types)
                    rightOperations.add(new LeafNodeWithOperator((CrySLObject) left, rightArit.getOperator().toString()));
                }
            }

            if (rightArit.getRight() instanceof ISLConstraint) {
                ISLConstraint right = (ISLConstraint) rightArit.getRight();
                if (right instanceof CrySLArithmeticConstraint) {
                    collectLeafNodes((CrySLArithmeticConstraint) right, rightOperations);
                } else {
                    // Handle if right is a leaf node (CrySLObject or other leaf types)
                    rightOperations.add(new LeafNodeWithOperator((CrySLObject) right, rightArit.getOperator().toString()));
                }
            }
        }
    }

    public class LeafNodeWithOperator {
        private final CrySLObject leafNode;
        private final String operator;

        public LeafNodeWithOperator(CrySLObject leafNode, String operator) {
            this.leafNode = leafNode;
            this.operator = operator;
        }

        public CrySLObject getLeafNode() {
            return leafNode;
        }

        public String getOperator() {
            return operator;
        }
    }

    public class ArithmeticNode {
        private final String operator;
        private final CrySLObject leftLeafNode;
        private final CrySLObject rightLeafNode;
        private ArithmeticNode left;
        private ArithmeticNode right;

        public ArithmeticNode(String operator, CrySLObject leftLeafNode, CrySLObject rightLeafNode) {
            this.operator = operator;
            this.leftLeafNode = leftLeafNode;
            this.rightLeafNode = rightLeafNode;
        }

        public void setLeft(ArithmeticNode left) {
            this.left = left;
        }

        public void setRight(ArithmeticNode right) {
            this.right = right;
        }

        public String buildEquation() {
            StringBuilder equationBuilder = new StringBuilder();

            if (left != null) {
                String leftEquation = left.buildEquation().trim();
                if (!leftEquation.isEmpty()) {
                    equationBuilder.append(leftEquation).append(" ");
                }
            } else if (leftLeafNode != null) {
                equationBuilder.append(leftLeafNode.getVarName()).append(" ");
            }

            equationBuilder.append(mapToOperator(operator)).append(" ");

            if (right != null) {
                String rightEquation = right.buildEquation().trim();
                if (!rightEquation.isEmpty()) {
                    equationBuilder.append(rightEquation).append(" ");
                }
            } else if (rightLeafNode != null) {
                equationBuilder.append(rightLeafNode.getVarName()).append(" ");
            }

            return equationBuilder.toString().trim();
        }

    }

    public ArithmeticNode buildTree(CrySLArithmeticConstraint rightArit) {
        if (rightArit.getLeft() instanceof CrySLObject && rightArit.getRight() instanceof CrySLObject) {
            // Both left and right are leaf nodes
            return new ArithmeticNode(rightArit.getOperator().toString(),
                    (CrySLObject) rightArit.getLeft(), (CrySLObject) rightArit.getRight());
        } else {
            // Recursively explore left and right nodes, checking for leaf nodes
            ArithmeticNode node = new ArithmeticNode(rightArit.getOperator().toString(), null, null);

            if (rightArit.getLeft() instanceof ISLConstraint) {
                ISLConstraint left = (ISLConstraint) rightArit.getLeft();
                if (left instanceof CrySLArithmeticConstraint) {
                    node.setLeft(buildTree((CrySLArithmeticConstraint) left));
                } else {
                    // Handle if left is a leaf node (CrySLObject or other leaf types)
                    node.setLeft(new ArithmeticNode("", (CrySLObject) left, null));
                }
            }

            if (rightArit.getRight() instanceof ISLConstraint) {
                ISLConstraint right = (ISLConstraint) rightArit.getRight();
                if (right instanceof CrySLArithmeticConstraint) {
                    node.setRight(buildTree((CrySLArithmeticConstraint) right));
                } else {
                    // Handle if right is a leaf node (CrySLObject or other leaf types)
                    node.setRight(new ArithmeticNode("", null, (CrySLObject) right));
                }
            }

            return node;
        }
    }

}