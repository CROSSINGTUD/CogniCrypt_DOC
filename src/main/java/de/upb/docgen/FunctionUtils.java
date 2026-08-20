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
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ritika Singh
 */

public class FunctionUtils {

	/**
	 * Extract distinct method signatures from the rule's state machine (raw labels).
	 */
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

	/**
	 * Extract distinct method signatures using parameter variable names as placeholders.
	 */
	public static List<String> getEventNamesKey(CrySLRule rule) {
		List<String> methodNames = new ArrayList<String>();
		StateMachineGraph graph = rule.getUsagePattern();
		List<TransitionEdge> edges = graph.getEdges();

		for (TransitionEdge edge : edges) {
			List<CrySLMethod> methods = edge.getLabel();

			for (CrySLMethod method : methods) {
				StringBuilder sb = new StringBuilder();
				String methodName = method.getShortMethodName();
				sb.append(methodName);
				sb.append("(");
				ArrayList<String> shortMehtodNames = new ArrayList<>();
				for (Entry<String, String> entry : method.getParameters()) {
					shortMehtodNames.add(entry.getKey());
				}
				sb.append(StringUtils.join(shortMehtodNames, ","));
				sb.append(")");
				methodNames.add(sb.toString());
			}
		}
		return methodNames.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * Extract distinct method signatures using parameter types (values) for display.
	 */
	public static List<String> getEventNamesValue(CrySLRule rule) {
		List<String> methodNames = new ArrayList<String>();
		StateMachineGraph graph = rule.getUsagePattern();
		List<TransitionEdge> edges = graph.getEdges();

		for (TransitionEdge edge : edges) {
			List<CrySLMethod> methods = edge.getLabel();

			for (CrySLMethod method : methods) {
				StringBuilder sb = new StringBuilder();
				String methodName = method.getShortMethodName();
				sb.append(methodName);
				sb.append("(");
				ArrayList<String> shortMehtodNames = new ArrayList<>();
				for (Entry<String, String> entry : method.getParameters()) {
					shortMehtodNames.add(entry.getValue());
				}
				sb.append(StringUtils.join(shortMehtodNames, ","));
				sb.append(")");
				methodNames.add(sb.toString());
			}
		}
		return methodNames.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * Build a method signature with parameter variable names.
	 */
	public static String getEventCrySLMethodKey(CrySLMethod method) {
		StringBuilder sb = new StringBuilder();
		String methodName = method.getShortMethodName();
		sb.append(methodName);
		sb.append("(");
		ArrayList<String> shortMehtodNames = new ArrayList<>();
		for (Entry<String, String> entry : method.getParameters()) {
			shortMehtodNames.add(entry.getKey());
		}
		sb.append(StringUtils.join(shortMehtodNames, ","));
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Build a method signature with parameter types, substituting AnyType with '_'.
	 */
	public static String getEventCrySLMethodValue(CrySLMethod method) {
		StringBuilder sb = new StringBuilder();
		String methodName = method.getShortMethodName();
		sb.append(methodName);
		sb.append("(");
		ArrayList<String> shortMethodNames = new ArrayList<>();
		for (Entry<String, String> entry : method.getParameters()) {
			String value = entry.getValue();
			if (value.equals("AnyType")) {
				shortMethodNames.add("_");
			} else {
				shortMethodNames.add(value);
			}
		}
		sb.append(StringUtils.join(shortMethodNames, ","));
		sb.append(")");
		return sb.toString();
	}

	/**
	 * True if {@code variableName} is exactly one of the parameters of a rendered method
	 * signature such as {@code getInstance(algorithm,provider)}.
	 *
	 * <p>Call sites previously used {@code methodStr.contains(variableName)}, which matches
	 * on any substring: an object named {@code preInput} matched {@code update(preInputByte)}.
	 * The position is then computed by an exact {@code indexOf}, which does not match, so the
	 * pair silently produced a position of -1. This predicate agrees with that computation.
	 */
	public static boolean hasParameterNamed(String methodSignature, String variableName) {
		if (methodSignature == null || variableName == null || variableName.isEmpty()) {
			return false;
		}
		int open = methodSignature.indexOf('(');
		int close = methodSignature.lastIndexOf(')');
		if (open < 0 || close < open) {
			return false;
		}
		for (String parameter : methodSignature.substring(open + 1, close).split(",")) {
			if (parameter.trim().equals(variableName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Map numeric parameter positions to human-readable words.
	 */
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

	/**
	 * Resolve the declared type for a parameter or return variable within a rule.
	 */
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
