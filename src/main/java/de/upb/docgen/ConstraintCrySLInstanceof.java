package de.upb.docgen;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import crypto.rules.*;
import de.upb.docgen.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import crypto.interfaces.ISLConstraint;

/**
 * @author Ritika Singh
 */

public class ConstraintCrySLInstanceof {

	static PrintWriter out;

	/**
	 * Load the template for the instanceof constraint LHS.
	 */
	private static String getTemplateinstanceofLHS() throws IOException {
		return Utils.getTemplatesTextString("ConstraintCrySLinstanceofClauseLHS");
	}

	/**
	 * Load the template for the instanceof constraint RHS.
	 */
	private static String getTemplateinstanceofRHS() throws IOException {
		return Utils.getTemplatesTextString("ConstraintCrySLinstanceofClauseRHS");
	}

	/**
	 * Build formatted instanceof constraints for a rule by mapping predicate
	 * parameters back to method signatures and positions.
	 */
	public ArrayList<String> getInstanceof(CrySLRule rule) throws IOException {
		ArrayList<String> composedInstaceOf = new ArrayList<>();
		List<ISLConstraint> constraintConList = rule.getConstraints().stream()
				.filter(e -> e.getClass().getSimpleName().toString().contains("CrySLConstraint"))
				.collect(Collectors.toList());
		if (constraintConList.size() > 0) {
			for (ISLConstraint conCryslISL : constraintConList) {
				CrySLConstraint leftConstraint = null;
				List<ISLConstraint> allNodesLeft = null;
				List<ISLConstraint> allLeftConstraints = new ArrayList<>();
				CrySLConstraint rightConstraint = null;
				List<ISLConstraint> allNodesRight = null;
				List<ISLConstraint> allRightConstraints = new ArrayList<>();
				if (conCryslISL instanceof CrySLConstraint) {
					if (((CrySLConstraint) conCryslISL).getLeft() instanceof CrySLConstraint)
						leftConstraint = (CrySLConstraint) ((CrySLConstraint) conCryslISL).getLeft();
					if (((CrySLConstraint) conCryslISL).getRight() instanceof CrySLConstraint)
						rightConstraint = (CrySLConstraint) ((CrySLConstraint) conCryslISL).getRight();
					if (leftConstraint != null) {
						allNodesLeft = getAllLeafNodes(allLeftConstraints, leftConstraint);
					}
					if (rightConstraint != null) {
						allNodesRight = getAllLeafNodes(allRightConstraints, rightConstraint);
					}
				}
				String conCryslStr = conCryslISL.toString();
				if (conCryslStr.startsWith("instance")) {
					List<String> impSplitList = Arrays.asList(conCryslStr.split("implies"));
					List<String> LHSList = Arrays.asList(impSplitList.get(0).split("or"));
					List<String> RHSList = Arrays.asList(impSplitList.get(1));
					List<String> methods = FunctionUtils.getEventNamesKey(rule);
					Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);
					List<Entry<String, String>> dataTypes = rule.getObjects();
					Map<String, String> DTMap = new LinkedHashMap<>();
					for (Entry<String, String> dt : dataTypes) {
						DTMap.put(dt.getKey(), dt.getValue());
					}
					String templatestringLHS = getTemplateinstanceofLHS();
					String templatestringRHS = getTemplateinstanceofRHS();

					String resultmainstringLHS = "";
					String resultmainstringRHS = "";

					for (int i = 0; i <= LHSList.size() - 1; i++) {

						if (i < 1) {

							String a = LHSList.get(i);
							List<String> resLHSlist = new ArrayList<>();
							Map<String, List<String>> methodsByPosition = new LinkedHashMap<>();

							if (a.contains("(") && a.contains(")")) {
								String result = StringUtils.substringBetween(a, "(", ")");
								resLHSlist = new ArrayList<>(Arrays.asList(result.split(",")));
							} else {
								resLHSlist = new ArrayList<>(
										Arrays.asList(a.replaceAll("VC:", "").replaceAll(",$", "").split(" - ")));
							}

							if (allNodesLeft == null) {
								allNodesLeft = new ArrayList<>();
								allNodesLeft.add(((CrySLConstraint) conCryslISL).getLeft());
							}
							// The first LHS leaf is not necessarily a predicate - a bare value
							// constraint is equally valid, and is what the i >= 1 branch below
							// already handles. Casting blindly threw ClassCastException here.
							ISLConstraint firstLeftNode = allNodesLeft.isEmpty() ? null : allNodesLeft.get(0);
							String realLHS = null;
							String real = null;
							if (firstLeftNode instanceof CrySLPredicate) {
								CrySLPredicate lhsPredicate = (CrySLPredicate) firstLeftNode;
								if (!lhsPredicate.getParameters().isEmpty()
										&& lhsPredicate.getParameters().get(0) instanceof CrySLObject) {
									realLHS = ((CrySLObject) lhsPredicate.getParameters().get(0)).getVarName();
								}
								if (lhsPredicate.getParameters().size() > 1
										&& lhsPredicate.getParameters().get(1) instanceof CrySLObject) {
									real = ((CrySLObject) lhsPredicate.getParameters().get(1)).getJavaType();
								}
							} else if (firstLeftNode instanceof CrySLValueConstraint) {
								realLHS = ((CrySLValueConstraint) firstLeftNode).getVarName();
							}

							for (String methodStr : methods) {
								if (realLHS == null) {
									break; // nothing to match on; keep the text-derived values
								}
								if (real != null && resLHSlist.size() > 1) {
									resLHSlist.set(0, realLHS);
									resLHSlist.set(1, real);
								}

								if (FunctionUtils.hasParameterNamed(methodStr, realLHS)) {

									List<String> methList = new ArrayList<>();
									methList.add(methodStr);

									for (String m : methList) {

										List<String> extractParamList = new ArrayList<>();
										int startIndex = m.indexOf("(");
										int endIndex = m.indexOf(")");
										String bracketExtractStr = m.substring(startIndex + 1, endIndex);

										if (bracketExtractStr.contains(",")) {
											String[] elements = bracketExtractStr.split(",");
											for (int a1 = 0; a1 < elements.length; a1++) {
												extractParamList.add(elements[a1]);
											}
										} else {
											extractParamList.add(bracketExtractStr);
										}

										for (String extractParamStr : extractParamList) {
											if (!DTMap.containsKey(extractParamStr)) {
											} else {
												String value = DTMap.get(extractParamStr).toString();
												m = m.replaceFirst(extractParamStr, value);
											}
										}

										String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
										List<String> strList = Arrays.asList(mStr.split(" "));
										String posStr = String.valueOf(strList.indexOf(realLHS));
										String posWord = posInWordsMap.getOrDefault(posStr, posStr);

										// See ConstraintCrySLVC: "either of the methods" implies a
										// single shared position, so only methods where the variable
										// sits at the same position may share a clause.
										methodsByPosition.computeIfAbsent(posWord, k -> new ArrayList<>()).add(m);
									}
								}
							}

							String varinstLHS = stripNullVarName(resLHSlist.get(1));
							String b = templatestringLHS;

							List<String> positionClauses = new ArrayList<>();
							for (Map.Entry<String, List<String>> group : methodsByPosition.entrySet()) {
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("positioni", group.getKey());
								valuesMap.put("methodnamei", String.join(", ", group.getValue()));
								valuesMap.put("vari1", varinstLHS);

								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								positionClauses.add(sub.replace(b));
							}
							resultmainstringLHS = String.join(" or ", positionClauses);

						} else {

							String d = " or";
							String b = templatestringLHS;
							String a = LHSList.get(i);
							ISLConstraint currentConstraint = allNodesLeft.get(i);
							String leftSidePredicateOrVCvarname = null;
							if (currentConstraint instanceof CrySLPredicate) {
								CrySLPredicate predicate = (CrySLPredicate) currentConstraint;
								leftSidePredicateOrVCvarname = ((CrySLObject) predicate.getParameters().get(0))
										.getVarName();
							} else if (currentConstraint instanceof CrySLValueConstraint) {
								CrySLValueConstraint valueConstraint = (CrySLValueConstraint) currentConstraint;
								leftSidePredicateOrVCvarname = valueConstraint.getVarName();
							} else {
								// Skip this clause rather than killing the JVM: an unexpected leaf type is a
								// reason to omit one sentence, not to abort the whole 51-rule run.
								System.err.println("[WARN] Skipping instanceOf clause for " + rule.getClassName()
									+ ": unsupported left-hand constraint type " + currentConstraint.getClass().getSimpleName());
								continue;
							}

							List<String> resLHSlistsecond;
							Map<String, List<String>> methodsByPositionSec = new LinkedHashMap<>();

							if (a.contains("(") && a.contains(")")) {
								String result = StringUtils.substringBetween(a, "(", ")");
								resLHSlistsecond = new ArrayList<>(Arrays.asList(result.split(",")));
							} else {

								resLHSlistsecond = new ArrayList<>(
										Arrays.asList(a.replaceAll("VC:", "").replaceAll(",$", "").split(" - ")));
							}

							for (String methodStr : methods) {

								if (FunctionUtils.hasParameterNamed(methodStr, leftSidePredicateOrVCvarname)) {

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
											if (!DTMap.containsKey(extractParamStr)) {
											} else {
												String value = DTMap.get(extractParamStr);
												m = m.replaceFirst(extractParamStr, value);
											}
										}
										String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
										List<String> strList = Arrays.asList(mStr.split(" "));
										String posStr = String.valueOf(strList.indexOf(leftSidePredicateOrVCvarname));
										String posWord = posInWordsMap.getOrDefault(posStr, posStr);

										methodsByPositionSec.computeIfAbsent(posWord, k -> new ArrayList<>()).add(m);
									}
								}
							}

							String varinstLHS2 = stripNullVarName(resLHSlistsecond.get(1));

							List<String> positionClausesSec = new ArrayList<>();
							for (Map.Entry<String, List<String>> group : methodsByPositionSec.entrySet()) {
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("positioni", group.getKey());
								valuesMap.put("methodnamei", String.join(", ", group.getValue()));
								valuesMap.put("vari1", varinstLHS2);

								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								positionClausesSec.add(sub.replace(b));
							}
							resultmainstringLHS += d + " " + String.join(" or ", positionClausesSec);
						}
					}

					String b = templatestringRHS;

					if (allNodesRight == null) {
						allNodesRight = new ArrayList<>();
						allNodesRight.add(((CrySLConstraint) conCryslISL).getRight());
					}

					for (String RHSStr : RHSList) {

						List<String> resRHSList = new ArrayList<>();
						resRHSList = new ArrayList<>(Arrays.asList(RHSStr.replaceAll("\\(.*\\)", "")
								.replaceAll("VC:", "").replaceAll(",$", "").split(" - ")));
						Map<String, List<String>> methodsByPositionRHS = new LinkedHashMap<>();

						// Loop-invariant: derived from allNodesRight, not from the method being examined.
						String rightSidePredicateOrVCvarname = null;
						if (allNodesRight.get(0) instanceof CrySLPredicate) {
							CrySLPredicate predicate = (CrySLPredicate) allNodesRight.get(0);
							rightSidePredicateOrVCvarname = ((CrySLObject) predicate.getParameters().get(0)).getVarName();
						} else if (allNodesRight.get(0) instanceof CrySLValueConstraint) {
							CrySLValueConstraint valueConstraint = (CrySLValueConstraint) allNodesRight.get(0);
							rightSidePredicateOrVCvarname = valueConstraint.getVarName();
						} else {
							// Skip this constraint rather than killing the JVM.
							System.err.println("[WARN] Skipping instanceOf clause for " + rule.getClassName()
								+ ": unsupported right-hand constraint type "
								+ allNodesRight.get(0).getClass().getSimpleName());
							continue;
						}

						String RHSfirstStr = rightSidePredicateOrVCvarname;

						for (String methodStr : methods) {

							if (FunctionUtils.hasParameterNamed(methodStr, RHSfirstStr)) {

								List<String> methList = new ArrayList<>();
								methList.add(methodStr);

								for (String m : methList) {

									List<String> extractParamList = new ArrayList<>();
									int startIndex = m.indexOf("(");
									int endIndex = m.indexOf(")");
									String bracketExtractStr = m.substring(startIndex + 1, endIndex);

									if (bracketExtractStr.contains(",")) {
										String[] elements = bracketExtractStr.split(",");
										for (int a1 = 0; a1 < elements.length; a1++) {
											extractParamList.add(elements[a1]);
										}
									} else {
										extractParamList.add(bracketExtractStr);
									}

									for (String extractParamStr : extractParamList) {
										if (!DTMap.containsKey(extractParamStr)) {
										} else {
											String value = DTMap.get(extractParamStr).toString();
											m = m.replaceFirst(extractParamStr, value);
										}
									}

									String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
									List<String> strList = Arrays.asList(mStr.split(" "));
									String posStr = String.valueOf(strList.indexOf(RHSfirstStr));
									String posWord = posInWordsMap.getOrDefault(posStr, posStr);

									// Group by position: "either of the methods" implies one shared
									// position, so a method where the variable sits elsewhere gets its
									// own clause instead of being merged under the first match's position.
									methodsByPositionRHS.computeIfAbsent(posWord, k -> new ArrayList<>()).add(m);
								}
							}
						}

						String varinstRHS = stripNullVarName(resRHSList.get(1));

						List<String> positionClausesRHS = new ArrayList<>();
						for (Map.Entry<String, List<String>> group : methodsByPositionRHS.entrySet()) {
							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("positions", group.getKey());
							valuesMap.put("methodnames", String.join(", ", group.getValue()));
							valuesMap.put("vars2", varinstRHS);

							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							positionClausesRHS.add(sub.replace(b));
						}
						resultmainstringRHS = String.join(" or ", positionClausesRHS);
						composedInstaceOf.add(resultmainstringLHS + resultmainstringRHS);
					}
				}
			}
		}
		return composedInstaceOf;
	}

	/**
	 * Strip the trailing "null" that CrySLObject.toString() appends for a type-only
	 * argument. instanceOf[key, java.security.PrivateKey] carries its type as a
	 * CrySLObject with no variable name, and toString() renders javaType + " " +
	 * varName - so the rendered sentence read "is of type java.security.PrivateKey null".
	 */
	private static String stripNullVarName(String typeToken) {
		if (typeToken == null) {
			return null;
		}
		if (typeToken.endsWith(" null")) {
			return typeToken.substring(0, typeToken.length() - " null".length());
		}
		return typeToken;
	}

	/**
	 * Collect all leaf constraints in a constraint tree (predicates or value constraints).
	 */
	public List<ISLConstraint> getAllLeafNodes(List<ISLConstraint> leafNodes, ISLConstraint node) {
		collectLeafNodes(node, leafNodes);
		return leafNodes;
	}

	/**
	 * Recursively traverse a constraint tree and accumulate leaf nodes.
	 */
	private void collectLeafNodes(ISLConstraint node, List<ISLConstraint> leafNodes) {
		if (node instanceof CrySLConstraint) {
			CrySLConstraint crySLNode = (CrySLConstraint) node;
			ISLConstraint left = crySLNode.getLeft();
			ISLConstraint right = crySLNode.getRight();

			if (left == null && right == null) {
				// This node is a leaf node
				leafNodes.add(node);
			} else {
				// Recursively explore left and right nodes, checking for leaf nodes
				if (left != null) {
					if (left instanceof CrySLConstraint) {
						collectLeafNodes((CrySLConstraint) left, leafNodes);
					} else {
						// Handle if left is a leaf node (CrySLValueConstraint or CrySLPredicate)
						leafNodes.add(left);
					}
				}
				if (right != null) {
					if (right instanceof CrySLConstraint) {
						collectLeafNodes((CrySLConstraint) right, leafNodes);
					} else {
						// Handle if right is a leaf node (CrySLValueConstraint or CrySLPredicate)
						leafNodes.add(right);
					}
				}
			}
		}
	}
}
