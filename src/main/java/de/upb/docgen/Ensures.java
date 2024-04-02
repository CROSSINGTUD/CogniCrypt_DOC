//case1.parameters contain "this" + method. case2. parameters contains "this"
package de.upb.docgen;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.upb.docgen.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import crypto.rules.CrySLCondPredicate;
import crypto.rules.CrySLMethod;
import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLRule;
import crypto.rules.StateMachineGraph;
import crypto.rules.TransitionEdge;

/**
 * @author Ritika Singh
 */

public class Ensures {

	static PrintWriter out;

	private static String getTemplateverbedge() throws IOException {
		return Utils.getTemplatesTextString("EnsuresClauseVerb-edge");
	}

	private static String getTemplateverbnounedge() throws IOException {
		return Utils.getTemplatesTextString("EnsuresClauseVerb-noun-edge");
	}

	private static String getTemplateverbnoun() throws IOException {
		return Utils.getTemplatesTextString("EnsuresClauseVerb-noun");
	}

	private static String getTemplateverbnounedgeCon() throws IOException {
		return Utils.getTemplatesTextString("EnsuresClauseVerb-noun-edgeCons");
	}

	public ArrayList<String> getEnsuresThis(CrySLRule rule, Map<String, List<Map<String, List<String>>>> stringListMap)
			throws IOException {
		ArrayList<String> composedEnsures = new ArrayList<>();
		List<Entry<String, String>> dataTypes = rule.getObjects();
		Map<String, String> DTMap = new LinkedHashMap<>();

		for (Entry<String, String> dt : dataTypes) {
			DTMap.put(dt.getValue(), dt.getKey());
		}

		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);

		StateMachineGraph smg = rule.getUsagePattern();
		List<TransitionEdge> edges = smg.getEdges();

		List<CrySLPredicate> predsThisList = rule.getPredicates().stream()
				.filter(e -> e.toString().contains("this") && !e.toString().contains("!"))
				.collect(Collectors.toList());

		if (predsThisList.size() > 0) {

			for (CrySLPredicate elementStr : predsThisList) {

				String predTNameStr = elementStr.getPredName().toString();
				String str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(predTNameStr), ' ');
				List<String> verbOrNounList = Arrays.asList(str.split("\\s"));
				String verb;
				List<String> noun = new ArrayList<>();
				String joined = null;

				if (elementStr instanceof CrySLCondPredicate) {

					CrySLCondPredicate conPred = (CrySLCondPredicate) elementStr;

					for (TransitionEdge edge : edges) {

						if (conPred.getConditionalMethods().contains(edge.to()) && !edge.to().equals(edge.from())) {
							List<String> predmethodThisNamesList = new ArrayList<String>();
							List<CrySLMethod> methodsThisedgeList = edge.getLabel();

							for (CrySLMethod method : methodsThisedgeList) {
								predmethodThisNamesList.add(FunctionUtils.getEventCrySLMethodValue(method));

							}

							/*
							 * for (String methodlistStr : predmethodThisNamesList) {
							 * List<String> extractParamList = new ArrayList<>();
							 * int startIndex = methodlistStr.indexOf("(");
							 * int endIndex = methodlistStr.indexOf(")");
							 * String bracketExtractStr = methodlistStr.substring(startIndex + 1, endIndex);
							 * 
							 * if (bracketExtractStr.contains(",")) {
							 * String[] elements = bracketExtractStr.split(",");
							 * for (int a = 0; a < elements.length; a++) {
							 * extractParamList.add(elements[a]);
							 * }
							 * } else {
							 * extractParamList.add(bracketExtractStr);
							 * }
							 * 
							 * for (String extractParamStr : extractParamList) {
							 * if (!DTMap.containsKey(extractParamStr)) {
							 * } else {
							 * String value = DTMap.get(extractParamStr).toString();
							 * methodlistStr = methodlistStr.replace(extractParamStr, value);
							 * }
							 * }
							 * 
							 * finalpredmethodThisNamesList.add(methodlistStr);
							 * joined = String.join(" or ", finalpredmethodThisNamesList);
							 * //System.out.println(joined);
							 * }
							 */
							joined = String.join(" or ", predmethodThisNamesList);
							List<String> msplit = Arrays.asList(joined.split("\\("));

							if (verbOrNounList.size() == 1) {
								verb = verbOrNounList.get(0);
								String verbedge = getTemplateverbedge();
								Map<String, String> valuesMap = new HashMap<String, String>();
								verb = toHoverLink(rule, stringListMap, verb);
								valuesMap.put("verb", verb);
								valuesMap.put("edgeName", joined);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(verbedge);
								composedEnsures.add(resolvedString);
								// out.println(resolvedString);
							}

							else {
								verb = verbOrNounList.get(0);
								noun = verbOrNounList.subList(1, verbOrNounList.size());
								String nouns = String.join(" ", noun);

								nouns = toHoverLink(rule, stringListMap, nouns, predTNameStr);

								if (msplit.get(0).contains(classnamecheck)) {

									String verbedge = getTemplateverbnounedgeCon();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("verb", verb);
									valuesMap.put("noun", nouns);
									valuesMap.put("edName", joined);

									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(verbedge);
									// out.println(resolvedString);
									composedEnsures.add(resolvedString);

									break;
								}
								String verbnounedge = getTemplateverbnounedge();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("verb", verb);
								valuesMap.put("noun", nouns);
								valuesMap.put("edName", joined);

								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(verbnounedge);
								// out.println(resolvedString);
								composedEnsures.add(resolvedString);

							}
							break;
						}
					}
				} else {

					if (verbOrNounList.size() == 1) {
						verb = verbOrNounList.get(0);

					} else {
						verb = verbOrNounList.get(0);
						noun = verbOrNounList.subList(1, verbOrNounList.size());
						String nouns = String.join(" ", noun);

						String verbnoun = getTemplateverbnoun();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("verb", verb);
						valuesMap.put("noun", toHoverLink(rule, stringListMap, nouns, predTNameStr));

						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(verbnoun);
						// out.println(resolvedString);
						composedEnsures.add(resolvedString);

					}
				}
			}
		}
		return composedEnsures;
	}

	private String toHoverLink(CrySLRule rule, Map<String, List<Map<String, List<String>>>> stringListMap, String word,
			String predicate) {
		List<Map<String, List<String>>> requiresOfClasses = stringListMap.get(rule.getClassName());
		for (Map<String, List<String>> maps : requiresOfClasses) {
			if (maps.containsKey(predicate)) {
				word = "<span class=\"tooltip\">" + word;
				String tooltiptext = "<span class=\"tooltiptext\">The following classes can require this predicate:\n";
				String classesLinks = htmlLinksClass(stringListMap.get(rule.getClassName()), predicate);
				String end = "</span></span>";

				word += tooltiptext + classesLinks + end;
			}
		}
		return word;
	}

	private String toHoverLink(CrySLRule rule, Map<String, List<Map<String, List<String>>>> stringListMap,
			String word) {
		List<Map<String, List<String>>> requiresOfClasses = stringListMap.get(rule.getClassName());
		for (Map<String, List<String>> maps : requiresOfClasses) {
			if (maps.containsKey(word)) {
				String classToLink = word;
				word = "<span class=\"tooltip\">" + word;
				String tooltiptext = "<span class=\"tooltiptext\">The following classes can require this predicate:\n";
				String classesLinks = htmlLinksClass(stringListMap.get(rule.getClassName()), classToLink);
				String end = "</span></span>";

				word += tooltiptext + classesLinks + end;
			}
		}
		return word;
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
}
