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

import crypto.interfaces.ICrySLPredicateParameter;
import crypto.rules.*;
import de.upb.docgen.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import crypto.interfaces.ISLConstraint;

import static java.lang.String.valueOf;

/**
 * @author Ritika Singh
 */

public class ConstraintCryslnocallto {

	static PrintWriter out;

	private static char[] getTemplatenocallto() throws IOException {
		return Utils.getTemplatesText("ConsraintCrySLnocalltoClause");
	}

	public ArrayList<String> getnoCalltoConstraint(CrySLRule rule) throws IOException {
		ArrayList<String> composedNocallToConstraints = new ArrayList<>();
		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		List<ISLConstraint> constraintConList = rule.getConstraints().stream()
				.filter(e -> e.getClass().getSimpleName().toString().contains("CrySLConstraint"))
				.collect(Collectors.toList());
		ArrayList<String> methds = new ArrayList<>();
		ArrayList<String> valuesWhichHaveToBeUsedThen = new ArrayList<>();
		ArrayList<CrySLMethod> crySLMethods = extractMethodsFromSmg(rule);
		Entry<String,String> cryslObjectEntry;
		if (constraintConList.size() > 0) {
			String valuesWhichHaveToBeSet;
			for (ISLConstraint conCryslISL : constraintConList) {
				if (conCryslISL instanceof CrySLConstraint) {
					CrySLConstraint crySLConstraint = ((CrySLConstraint) conCryslISL);
					if (crySLConstraint.getName().contains("noCallTo")) {
						if ("implies".equals(valueOf(crySLConstraint.getOperator()))) {
							if (crySLConstraint.getLeft() instanceof CrySLPredicate && "noCallTo".equals(((CrySLPredicate) crySLConstraint.getLeft()).getPredName())&& crySLConstraint.getRight() instanceof CrySLValueConstraint) {
								CrySLPredicate crySLPredicate = (CrySLPredicate) crySLConstraint.getLeft();
								for (ICrySLPredicateParameter parameter : crySLPredicate.getParameters()) {
									methds.add(FunctionUtils.getEventCrySLMethodValue((CrySLMethod) parameter));
								}
								CrySLValueConstraint crySLValueConstraint = (CrySLValueConstraint) crySLConstraint.getRight();
								CrySLObject object = crySLValueConstraint.getVar();
								String javaType = object.getJavaType();
								String objectName = object.getVarName();
								List<String> values = crySLValueConstraint.getValueRange();
								valuesWhichHaveToBeUsedThen.addAll(values);
								cryslObjectEntry = Map.entry(objectName, javaType);
								valuesWhichHaveToBeSet = StringUtils.join(crySLValueConstraint.getValueRange(), ", ");
								String joinedstring = StringUtils.join(methds, ", ");
								List<CrySLMethod> getInstances = new ArrayList<>();
								for (CrySLMethod method : crySLMethods) {
									for (Entry<String, String> parameters : method.getParameters()) {
										if (cryslObjectEntry != null && parameters.getKey().equals(cryslObjectEntry.getKey())) {
											getInstances.add(method);
										}
									}
								}
								List<String> extractedWithObject = new ArrayList<>();
								StringBuilder sb = new StringBuilder();
								for (CrySLMethod method : getInstances) {
									sb.append("first|");
									sb.append(method.getMethodName());
									sb.append("(");
									ArrayList<String> tempForJoinParametersNames = new ArrayList<>();
									for (Entry<String, String> s : method.getParameters()) {
										if ("AnyType".equals(s.getValue())) {
											tempForJoinParametersNames.add("_");
										} else {
											tempForJoinParametersNames.add(s.getValue());
										}
									}
									sb.append(StringUtils.join(tempForJoinParametersNames, ", "));
									sb.append(")");
									extractedWithObject.add(sb.toString());
									sb.setLength(0);
								}
								for (String s : extractedWithObject) {
									String tempForSplit = s;
									List<String> ls = Arrays.asList(tempForSplit.split("\\|"));
									String paraPos = ls.get(0);
									String mname = ls.get(1);
									char[] str = getTemplatenocallto();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("methodNames", joinedstring);
									valuesMap.put("position", paraPos);
									valuesMap.put("method", mname);
									valuesMap.put("var2", valuesWhichHaveToBeSet);
									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(str);
									composedNocallToConstraints.add(resolvedString);
								}
							}
						}
					}
				}
			}
		}
		return composedNocallToConstraints;
	}




	public static <T> void addIfNotExists(List<CrySLMethod> list, CrySLMethod element) {
		if (!list.contains(element)) {
			list.add(element);
		}
	}


	private static ArrayList<CrySLMethod> extractMethodsFromSmg(CrySLRule rule) {
		ArrayList<CrySLMethod> allMethodsOfCrySLRule = new ArrayList<>();
		StateMachineGraph smg = rule.getUsagePattern();
		List<TransitionEdge> transitionEdgeList = smg.getEdges();
		for (TransitionEdge e : transitionEdgeList) {
			for(CrySLMethod method : e.getLabel()) {
				addIfNotExists(allMethodsOfCrySLRule, method);
			}
		}
		return allMethodsOfCrySLRule;
	}
}
