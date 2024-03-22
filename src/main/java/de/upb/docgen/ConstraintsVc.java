package de.upb.docgen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import crypto.rules.CrySLValueConstraint;
import de.upb.docgen.utils.Utils;
import org.apache.commons.text.StringSubstitutor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLRule;

/**
 * @author Ritika Singh
 */

public class ConstraintsVc {
    private static char[] getTemplateVC() throws IOException {
        return Utils.getTemplatesText("ConstraintsVcClause");
    }

    private static char[] getTemplateVCCon() throws IOException {
        return Utils.getTemplatesText("ConstraintsVcClauseCon");
    }

    public ArrayList<String> getConstraintsVc(CrySLRule rule) throws IOException {
        ArrayList<String> composedConstraints = new ArrayList<>();
        Map<String, String> constraintVCMap;
        List<String> methodsList = FunctionUtils.getEventNamesKey(rule);
        Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);
        List<Entry<String, String>> dataTypes = rule.getObjects();
        Map<String, String> DTMap = new LinkedHashMap<>(dataTypes.size());
        for (Entry<String, String> dt : dataTypes) {
            DTMap.put(dt.getKey(), dt.getValue());
        }
        String classnamecheck = rule.getClassName().substring(rule.getClassName().lastIndexOf('.') + 1);
        String paraConVCMapValStr;
        String paraPosInWordValStr;
        List<ISLConstraint> constraintVCList = rule.getConstraints().stream()
                .filter(e -> e.getClass().getSimpleName().contains("CrySLValueConstraint"))
                .collect(Collectors.toList());
        if (constraintVCList.size() > 0) {
            List<String> firstConVCList = new ArrayList<>();
            Multimap<String, String> paraMethNameMMap = ArrayListMultimap.create();
            Multimap<String, String> paraPosMMap = ArrayListMultimap.create();
            for (ISLConstraint valueConstraint : constraintVCList) {
                firstConVCList.add(((CrySLValueConstraint) valueConstraint).getVar().getVarName());
            }
            Map<String, String> parameterAndValuesToAssumeList = new HashMap<>();
            constraintVCList.stream()
                    .filter(constraint -> constraint instanceof CrySLValueConstraint)
                    .map(constraint -> (CrySLValueConstraint) constraint)
                    .forEach(valueConstraint -> {
                        String key = valueConstraint.getVar().getVarName();
                        String value = String.join(",", valueConstraint.getValueRange());
                        parameterAndValuesToAssumeList.put(key, value);
                    });
            constraintVCMap = parameterAndValuesToAssumeList;
            for (String firstConVCStr : firstConVCList) {
                for (String methodStr : methodsList) {
                    if (methodStr.contains(firstConVCStr)) {
                        List<String> methList = new ArrayList<>();
                        methList.add(methodStr);
                        for (String m : methList) {
                            List<String> extractParamList = new ArrayList<>();
                            int startIndex = m.indexOf("(");
                            int endIndex = m.indexOf(")");
                            String bracketExtractStr = m.substring(startIndex + 1, endIndex);
                            if (bracketExtractStr.contains(",")) {
                                String[] elements = bracketExtractStr.split(",");
                                extractParamList.addAll(Arrays.asList(elements));
                            } else {
                                extractParamList.add(bracketExtractStr);
                            }
                            for (String extractParamStr : extractParamList) {
                                if (extractParamStr.equals("_")) continue;
                                String value = DTMap.get(extractParamStr);
                                m = m.replace(extractParamStr, value);
                            }
                            String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
                            List<String> strList = Arrays.asList(mStr.split(" "));
                            String posStr = String.valueOf(strList.indexOf(firstConVCStr));
                            paraMethNameMMap.put(firstConVCStr, m);
                            paraPosMMap.put(firstConVCStr, posStr);
                        }
                    }
                }
            }
            //reslist format: positionOfParameter|method|valuesWhichParameter has to assume
            List<String> resList = new ArrayList<>();
            for (String firstConVCStr : firstConVCList) {
                List<String> postionOfParameterList = new ArrayList<>();
                List<String> methodList = new ArrayList<>();

                paraConVCMapValStr = constraintVCMap.getOrDefault(firstConVCStr, "");

                for (Map.Entry<String, String> paraPosentry : paraPosMMap.entries()) {
                    if (paraPosentry.getKey().equals(firstConVCStr)) {
                        String value = paraPosentry.getValue();
                        if (posInWordsMap.containsKey(value)) {
                            paraPosInWordValStr = posInWordsMap.get(value);
                            value = paraPosInWordValStr;
                        }
                        postionOfParameterList.add(value);
                    }
                }

                for (Map.Entry<String, String> paraMethentry : paraMethNameMMap.entries()) {
                    if (paraMethentry.getKey().equals(firstConVCStr)) {
                        methodList.add(paraMethentry.getValue());
                    }
                }

                for (int i = 0; i < postionOfParameterList.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(postionOfParameterList.get(i)).append("|").append(methodList.get(i)).append("|").append(paraConVCMapValStr);
                    resList.add(sb.toString());
                }
            }
            for (String fl : resList) {
                String l1 = fl;
                List<String> ls = Arrays.asList(l1.split("\\|"));
                String paraPos = ls.get(0);
                String mname = ls.get(1);
                String var = ls.get(2);
                List<String> msplit = Arrays.asList(ls.get(1).split("\\("));
                char[] str;
                if (msplit.get(0).contains(classnamecheck)) {
                    str = getTemplateVCCon();
                } else {
                    str = getTemplateVC();
                }
                Map<String, String> valuesMap = new HashMap<>();
                valuesMap.put("position", paraPos);
                valuesMap.put("methodname", mname);
                valuesMap.put("var2", var);
                StringSubstitutor sub = new StringSubstitutor(valuesMap);
                String resolvedString = sub.replace(new String(str));
                composedConstraints.add(resolvedString);
            }
        }
        return composedConstraints;
    }
}