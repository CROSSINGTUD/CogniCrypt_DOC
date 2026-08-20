package de.upb.docgen;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	/**
	 * Template for negated predicate sentences.
	 */
	private static String getTemplateNegated() throws IOException {
		String strD = Utils.getTemplatesTextString("Negation");
		return strD;
	}

	/**
	 * Build negation sentences for predicates on "this" that are negated.
	 * Resolves conditional predicates to the methods that trigger them and
	 * substitutes parameter types for readability.
	 */
	public ArrayList<String> getNegates(CrySLRule rule) throws IOException {
		ArrayList<String> composedNegates = new ArrayList<>();

		StateMachineGraph smg = rule.getUsagePattern();
		List<TransitionEdge> edges = smg.getEdges();
		String negjoined = "";

		// Select only negated predicates that refer to "this". NEGATES-block predicates
		// are parsed into rule.getNegatedPredicates(), a list disjoint from
		// rule.getPredicates() (which is exclusively ENSURES-sourced) - reading the
		// latter meant this method always returned empty, for every rule.
		List<CrySLPredicate> predNegatesList = rule.getNegatedPredicates().stream()
				.filter(e -> e.toString().contains("this") && e.toString().contains("!")).collect(Collectors.toList());

		if (predNegatesList.size() > 0) {

			for (CrySLPredicate neg : predNegatesList) {

				if (neg instanceof CrySLCondPredicate) {

					// Conditional negation: tie to specific state-machine edges.
					CrySLCondPredicate conPred = (CrySLCondPredicate) neg;

					for (TransitionEdge edge : edges) {

						if (conPred.getConditionalMethods().contains(edge.to()) && !edge.to().equals(edge.from())) {

							List<String> finalpredmethodNamesList = new ArrayList<>();
							List<CrySLMethod> methods = edge.getLabel();

							// Render as name(paramTypes) via the shared helper: the previous
							// hand-rolled string surgery mangled every parameterized method
							// and then dereferenced a null type lookup.
							for (CrySLMethod method : methods) {
								finalpredmethodNamesList.add(FunctionUtils.getEventCrySLMethodValue(method));
							}
							negjoined = String.join(", ", finalpredmethodNamesList);
							// Render the negation sentence once per matching edge.
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
