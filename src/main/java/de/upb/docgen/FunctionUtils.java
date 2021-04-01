/*This class contains the common functions used by other classes*/
package de.upb.docgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import crypto.rules.StateMachineGraph;
import crypto.rules.TransitionEdge;

/**
 * @author Ritika Singh
 */

public class FunctionUtils {

	public static List<String> getEventNames(CrySLRule rule) {
		List<String> methodNames = new ArrayList<String>();
		StateMachineGraph graph = rule.getUsagePattern();
		List<TransitionEdge> edges = graph.getEdges();

		for (TransitionEdge edge : edges) {
			List<CrySLMethod> methods = edge.getLabel();
			for (CrySLMethod method : methods) {
				String[] preMTStrArr = method.toString().replace(".", ",").split(",");
				methodNames.add(preMTStrArr[preMTStrArr.length - 1].replace(";", "").replaceAll("\\( ", "\\(")
						.replaceAll(" ", ","));
			}
		}
		return methodNames.stream().distinct().collect(Collectors.toList());
	}

	public static Map<String, String> getPosWordMap(CrySLRule rule) {

		Map<String, String> posInWords = new HashMap<>();
		posInWords.put("1", "first");
		posInWords.put("2", "second");
		posInWords.put("3", "third");
		posInWords.put("4", "fourth");
		posInWords.put("5", "fifth");
		posInWords.put("6", "sixth");
		posInWords.put("7", "seventh");

		return posInWords;

	}

	public static String getDataType(CrySLRule rule, String var) {
		ArrayList<TransitionEdge> transitions = new ArrayList<TransitionEdge>(
				rule.getUsagePattern().getAllTransitions());
		for (TransitionEdge transition : transitions) {
			List<CrySLMethod> methods = transition.getLabel();
			for (CrySLMethod method : methods) {
				List<Entry<String, String>> parameters = method.getParameters();
				for (Entry<String, String> parameter : parameters) {
					if (parameter.getKey().equals(var)) {
						return parameter.getValue();
					}
				}

				Entry<String, String> ret = method.getRetObject();
				if (ret.getKey().equals(var)) {
					return ret.getValue();
				}
			}
		}
		return null;
	}

}
