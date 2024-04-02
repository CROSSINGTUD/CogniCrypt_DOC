package de.upb.docgen;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.upb.docgen.utils.Utils;
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

public class Negates {

	static PrintWriter out;

	private static String getTemplateNegated() throws IOException {
		String strD = Utils.getTemplatesTextString("Negation");
		return strD;
	}

	public ArrayList<String> getNegates(CrySLRule rule) throws IOException {
		ArrayList<String> composedNegates = new ArrayList<>();

		StateMachineGraph smg = rule.getUsagePattern();
		List<TransitionEdge> edges = smg.getEdges();
		List<Entry<String, String>> dataTypes = rule.getObjects();
		Map<String, String> DTMap = new LinkedHashMap<>();
		for (Entry<String, String> dt : dataTypes) {
			DTMap.put(dt.getValue(), FunctionUtils.getDataType(rule, dt.getValue()));
		}
		String negjoined = "";

		List<CrySLPredicate> predNegatesList = rule.getPredicates().stream()
				.filter(e -> e.toString().contains("this") && e.toString().contains("!")).collect(Collectors.toList());

		if (predNegatesList.size() > 0) {

			for (CrySLPredicate neg : predNegatesList) {

				if (neg instanceof CrySLCondPredicate) {

					CrySLCondPredicate conPred = (CrySLCondPredicate) neg;

					for (TransitionEdge edge : edges) {

						if (conPred.getConditionalMethods().contains(edge.to()) && !edge.to().equals(edge.from())) {

							List<String> predmethodNames = new ArrayList<String>();
							List<String> finalpredmethodNamesList = new ArrayList<>();
							List<CrySLMethod> methods = edge.getLabel();

							for (CrySLMethod method : methods) {
								String[] preM = method.toString().replace(".", ",").split(",");
								predmethodNames.add(preM[preM.length - 1].replace(";", "").replaceAll("\\( ", "\\(")
										.replaceAll(" ", ","));
							}
							for (String tempStr : predmethodNames) {
								List<String> extractParamList = new ArrayList<>();
								int startIndex = tempStr.indexOf("(");
								int endIndex = tempStr.indexOf(")");
								String bracketExtractStr = tempStr.substring(startIndex + 1, endIndex);

								if (bracketExtractStr.contains(",")) {

									String[] elements = bracketExtractStr.split(",");
									for (int a = 0; a < elements.length; a++) {
										extractParamList.add(elements[a]);
									}
								} else {
									extractParamList.add(bracketExtractStr);
								}

								for (String extractParamStr : extractParamList) {
									if (!extractParamStr.isEmpty()) {
										String value = DTMap.get(extractParamStr).toString();
										tempStr = tempStr.replace(extractParamStr, value);
									}
								}
								finalpredmethodNamesList.add(tempStr);
								negjoined = String.join(", ", finalpredmethodNamesList);
							}
							String strRetOne = getTemplateNegated();
							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("NegatedMethods", negjoined);

							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							String resolvedString = sub.replace(strRetOne);
							// out.println(resolvedString);
							composedNegates.add(resolvedString);
							break;

						}

					}

				}

			}

		}
		// out.close();
		return composedNegates;

	}

}
