//Negated ignored /case1. the parameters does not contains "this" but can have COND,  case2. the parameters does not contain this

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import crypto.rules.*;
import de.upb.docgen.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

/**
 * @author Ritika Singh
 */

public class EnsuresCaseTwo {

	static PrintWriter out;

	/**
	 * Map return-value variables to the method names that produce them.
	 */
	private static Map<String, String> getReturnValues(CrySLRule rule) {
		Map<String, String> retValMap = new LinkedHashMap<>();

		ArrayList<TransitionEdge> transitions = new ArrayList<TransitionEdge>(
				rule.getUsagePattern().getAllTransitions());

		for (TransitionEdge transition : transitions) {
			List<CrySLMethod> methods = transition.getLabel();
			for (CrySLMethod method : methods) {
				if (method.toString().contains(" = ")) {
					String firstReturnTypeVar;
					String secondMethodNameVar;
					// List<String> retEntry = Arrays
					// .asList(method.toString().replaceAll("\\[", "").replaceAll("\\]",
					// "").split(","));
					List<String> retEntry = Arrays.asList(
							(method.getRetObject().getKey()) + " = " + FunctionUtils.getEventCrySLMethodValue(method));
					List<String> rfList = Arrays.asList(retEntry.get(0).split(" = "));
					firstReturnTypeVar = rfList.get(0);
					List<String> rsList = Arrays.asList(
							rfList.get(1).replace(".", ",").replaceAll("\\(.*\\)", "").replaceAll(";", "").split(","));
					secondMethodNameVar = rsList.get((rsList.size()) - 1);
					retValMap.put(firstReturnTypeVar, secondMethodNameVar);
				}
				// break;
			}
		}
		return retValMap;
	}

	/**
	 * Template for conditional ensures with return value: verb + method.
	 */
	private static String getTemplateReturnValueOne() throws IOException {
		return Utils.getTemplatesTextString("EnsuresClauseReturnVal_verbmeth");
	}

	/**
	 * Template for conditional ensures with return value: verb + noun + method.
	 */
	private static String getTemplateReturnValueTwo() throws IOException {
		return Utils.getTemplatesTextString("EnsuresClauseReturnVal_verbnounmeth");
	}

	/**
	 * Template for unconditional ensures with return value: verb.
	 */
	private static String getTemplateReturnValueThree() throws IOException {
		return Utils.getTemplatesTextString("EnsuresClauseReturnVal_verb");
	}

	/**
	 * Template for unconditional ensures with return value: verb + noun.
	 */
	private static String getTemplateReturnValueFour() throws IOException {
		return Utils.getTemplatesTextString("EnsuresClauseReturnVal_verbnoun");
	}

	/**
	 * Template for conditional ensures without return value: verb + method.
	 */
	private static String getTemplateOne() throws IOException {
		return Utils.getTemplatesTextString("Ensures-thisNA-verbmeth");

	}

	/**
	 * Template for conditional ensures without return value: verb + noun + method.
	 */
	private static String getTemplateTwo() throws IOException {
		String strDSix = Utils.getTemplatesTextString("Ensures-thisNA-verbnounmeth");
		return strDSix;
	}

	/**
	 * Template for unconditional ensures without return value: verb.
	 */
	private static String getTemplateThree() throws IOException {
		return Utils.getTemplatesTextString("Ensures-thisNA-verb");

	}

	/**
	 * Template for unconditional ensures without return value: verb + noun.
	 */
	private static String getTemplateFour() throws IOException {
		return Utils.getTemplatesTextString("Ensures-thisNA-verbnoun");
	}

	/**
	 * Build ensures sentences for predicates that do NOT involve "this".
	 * Handles both return-value predicates and parameter predicates, with
	 * optional conditional edges and tooltip links to requiring classes.
	 */
	public ArrayList<String> getEnsures(CrySLRule rule, Map<String, List<Map<String, List<String>>>> stringListMap)
			throws IOException {
		ArrayList<String> composedEnsures = new ArrayList<>();
		String joined = null;
		List<Entry<String, String>> dataTypes = rule.getObjects();
		Map<String, String> DTMap = new LinkedHashMap<>();

		for (Entry<String, String> dt : dataTypes) {
			DTMap.put(dt.getKey(), dt.getValue());
		}

		List<String> methodsNameList = FunctionUtils.getEventNamesKey(rule);
		Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);
		Map<String, String> retTypeMap = getReturnValues(rule);

		StateMachineGraph smg = rule.getUsagePattern();
		List<TransitionEdge> edges = smg.getEdges();

		List<CrySLPredicate> predList = rule.getPredicates().stream()
				.filter(e -> !e.toString().contains("this") && !e.toString().contains("!"))
				.collect(Collectors.toList());

		for (CrySLPredicate elementN : predList) {

			String paramStr = ((CrySLObject) elementN.getParameters().get(0)).getVarName();
			if (retTypeMap.containsKey(paramStr)) {
				// Branch: predicate applies to a returned value.

				String returnValMethod = retTypeMap.get(paramStr);

				String predNameStr = elementN.getPredName().toString();
				String str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(predNameStr), ' ');
				List<String> verbOrNounList = Arrays.asList(str.split("\\s"));
				String verb;
				List<String> noun = new ArrayList<>();

				if (elementN instanceof CrySLCondPredicate) {

					CrySLCondPredicate conPred = (CrySLCondPredicate) elementN;

					for (TransitionEdge edge : edges) {

						if (conPred.getConditionalMethods().contains(edge.to()) && !edge.to().equals(edge.from())) {

							List<String> predmethodNames = new ArrayList<String>();
							List<CrySLMethod> methods = edge.getLabel();

							for (CrySLMethod method : methods) {
								predmethodNames.add(FunctionUtils.getEventCrySLMethodValue(method));
							}

							joined = String.join(" or ", predmethodNames);

							if (verbOrNounList.size() == 1) {
								verb = verbOrNounList.get(0);

								String strRetOne = getTemplateReturnValueOne();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("returnValMethod", returnValMethod);
								valuesMap.put("verb", toHoverLink(rule, stringListMap, verb, predNameStr));
								valuesMap.put("methodName", joined);

								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(strRetOne);
								composedEnsures.add(resolvedString);
								// out.println(resolvedString);

							} else {
								verb = verbOrNounList.get(0);
								noun = verbOrNounList.subList(1, verbOrNounList.size());
								String nouns = String.join(" ", noun);

								String strRetTwo = getTemplateReturnValueTwo();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("returnValMethod", returnValMethod);
								valuesMap.put("verb", verb);
								valuesMap.put("nouns", toHoverLink(rule, stringListMap, nouns, predNameStr));
								valuesMap.put("joined", joined);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(strRetTwo);
								composedEnsures.add(resolvedString);
								// out.println(resolvedString);
							}
							break;
						}
					}
				} else {

					if (verbOrNounList.size() == 1) {
						verb = verbOrNounList.get(0);

						String strRetThree = getTemplateReturnValueThree();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("returnValMethod", returnValMethod);
						valuesMap.put("verb", toHoverLink(rule, stringListMap, verb, predNameStr));
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(strRetThree);
						composedEnsures.add(resolvedString);
						// out.println(resolvedString);

					} else {
						verb = verbOrNounList.get(0);
						noun = verbOrNounList.subList(1, verbOrNounList.size());
						String nouns = String.join(" ", noun);

						String strRetFour = getTemplateReturnValueFour();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("returnValMethod", returnValMethod);
						valuesMap.put("verb", verb);
						valuesMap.put("nouns", toHoverLink(rule, stringListMap, nouns, predNameStr));
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(strRetFour);
						composedEnsures.add(resolvedString);
						// out.println(resolvedString);
					}
				}
			}

			else {
				// Branch: predicate applies to a method parameter (non-return).
				// One entry per matching method. These were LinkedHashMaps keyed by the
				// predicate's parameter, so every match overwrote the previous one and
				// only the last survived - the sibling ConstraintsPred keeps every match
				// and emits one sentence each, which is what this now does too.
				List<String> matchedPositions = new ArrayList<>();
				List<String> matchedMethodNames = new ArrayList<>();

				for (String methodStr : methodsNameList) {
					String result = StringUtils.substringBetween(methodStr, "(", ")");
					List<String> resList = Arrays.asList(result.split(","));

					for (String rl : resList) {
						if (rl.equals(paramStr)) {
							String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
							List<String> strList = Arrays.asList(mStr.split(" "));
							String posStr = String.valueOf(strList.indexOf(paramStr));

							matchedPositions.add(posStr);

							String m = methodStr;
							List<String> extractParamList = new ArrayList<>();
							int startIndex = m.indexOf("(");
							int endIndex = m.indexOf(")");
							String bracketExtractStr = m.substring(startIndex + 1, endIndex);

							if (bracketExtractStr.contains(",")) {
								String[] elements = bracketExtractStr.split(",");
								for (int a = 0; a < elements.length; a++) {
									extractParamList.add(elements[a]);
								}
							} else {
								extractParamList.add(bracketExtractStr);
							}

							for (String extractParamStr : extractParamList) {

								if (!DTMap.containsKey(extractParamStr)) {

								} else {
									int startInd = 0;
									int endInd = 0;
									String value = DTMap.get(extractParamStr).toString();

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

							matchedMethodNames.add(m);
							break;
						}
					}
				}

				for (int matchIndex = 0; matchIndex < matchedMethodNames.size(); matchIndex++) {
				String paraPosMapValStr = matchedPositions.get(matchIndex);
				String paraMethNameMapValStr = matchedMethodNames.get(matchIndex);
				String paraPosInWordValStr = posInWordsMap.getOrDefault(paraPosMapValStr, paraPosMapValStr);

				String predNameStr = elementN.getPredName().toString();
				String str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(predNameStr), ' ');
				List<String> verbOrNounList = Arrays.asList(str.split("\\s"));
				String verb;
				List<String> noun = new ArrayList<>();

				if (elementN instanceof CrySLCondPredicate) {

					CrySLCondPredicate conPred = (CrySLCondPredicate) elementN;

					for (TransitionEdge edge : edges) {

						if (conPred.getConditionalMethods().contains(edge.to()) && !edge.to().equals(edge.from())) {

							List<String> finalpredmethodNamesList = new ArrayList<>();
							List<CrySLMethod> methods = edge.getLabel();

							// Same helper the analogous branch above uses: hand-rolled splitting
							// on "."/" " mangled every multi-parameter method name.
							for (CrySLMethod method : methods) {
								finalpredmethodNamesList.add(FunctionUtils.getEventCrySLMethodValue(method));
							}
							joined = String.join(", ", finalpredmethodNamesList);
							if (verbOrNounList.size() == 1) {
								verb = verbOrNounList.get(0);

								String strOne = getTemplateOne();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("paraPosInWordValStr", paraPosInWordValStr);
								valuesMap.put("paraMethNameMapValStr", paraMethNameMapValStr);
								valuesMap.put("verb", toHoverLink(rule, stringListMap, verb, predNameStr));
								valuesMap.put("joined", joined);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(strOne);
								// out.println(resolvedString);
								composedEnsures.add(resolvedString);

							} else {
								verb = verbOrNounList.get(0);
								noun = verbOrNounList.subList(1, verbOrNounList.size());
								String nouns = String.join(" ", noun);
								String strTwo = getTemplateTwo();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("paraPosInWordValStr", paraPosInWordValStr);
								valuesMap.put("paraMethNameMapValStr", paraMethNameMapValStr);
								valuesMap.put("verb", verb);
								valuesMap.put("nouns", toHoverLink(rule, stringListMap, nouns, predNameStr));
								valuesMap.put("joined", joined);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(strTwo);
								// out.println(resolvedString);
								composedEnsures.add(resolvedString);
							}
							break;
						}
					}
				} else {

					if (verbOrNounList.size() == 1) {
						verb = verbOrNounList.get(0);

						String strThree = getTemplateThree();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("paraPosInWordValStr", paraPosInWordValStr);
						valuesMap.put("paraMethNameMapValStr", paraMethNameMapValStr);
						valuesMap.put("verb", toHoverLink(rule, stringListMap, verb, predNameStr));
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(strThree);
						// out.println(resolvedString);
						composedEnsures.add(resolvedString);

					} else {
						verb = verbOrNounList.get(0);
						noun = verbOrNounList.subList(1, verbOrNounList.size());
						String nouns = String.join(" ", noun);

						String strFour = getTemplateFour();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("paraPosInWordValStr", paraPosInWordValStr);
						valuesMap.put("paraMethNameMapValStr", paraMethNameMapValStr);
						valuesMap.put("verb", verb);
						valuesMap.put("nouns", toHoverLink(rule, stringListMap, nouns, predNameStr));
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(strFour);
						// out.println(resolvedString);
						composedEnsures.add(resolvedString);

					}
				}
				} // end per-match loop
			}
		}
		// out.close();
		return composedEnsures;
	}

	/**
	 * Wrap a word with a tooltip linking to classes that require the predicate.
	 */
	private String toHoverLink(CrySLRule rule, Map<String, List<Map<String, List<String>>>> stringListMap, String word,
			String predicate) {
		List<Map<String, List<String>>> requiresOfClasses = stringListMap.get(rule.getClassName());
		for (Map<String, List<String>> maps : requiresOfClasses) {
			if (maps.containsKey(predicate)) {
				String classToLink = word;
				word = "<span class=\"tooltip\">" + word;
				String tooltiptext = "<span class=\"tooltiptext\">The following classes can require this predicate:\n";
				String classesLinks = htmlLinksClass(stringListMap.get(rule.getClassName()), classToLink, predicate);
				String end = "</span></span>";

				word += tooltiptext + classesLinks + end;
			}
		}
		return word;
	}

	/**
	 * Build HTML links for classes that require a given predicate.
	 */
	private String htmlLinksClass(List<Map<String, List<String>>> maps, String var1, String predicate) {
		StringBuilder sb = new StringBuilder();
		for (Map<String, List<String>> map : maps) {
			if (map.containsKey(predicate)) {
				for (String className : map.get(predicate)) {
					sb.append("<a href=\"").append(className).append(".html\">").append(className).append("</a>\n");
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Escape square brackets for safe regex matching.
	 */
	private String escapeString(String inputString) {
		// Check if the input string contains square brackets
		if (inputString.contains("[") || inputString.contains("]")) {
			// If yes, escape the string by adding a backslash before each square bracket
			return inputString.replace("[", "\\[").replace("]", "\\]");
		} else {
			// Otherwise, return the original string
			return inputString;
		}
	}
}
