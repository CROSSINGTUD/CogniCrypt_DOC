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
import org.apache.commons.collections4.bag.SynchronizedSortedBag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import crypto.interfaces.ISLConstraint;

/**
 * @author Ritika Singh
 */

public class ConstraintCrySLInstanceof {

	static PrintWriter out;

	private static String getTemplateinstanceofLHS() throws IOException {
		String strD = Utils.getTemplatesTextString("ConstraintCrySLinstanceofClauseLHS");
		/*
		File file = new File(".\\src\\main\\resources\\Templates\\ConstraintCrySLinstanceofClauseLHS");
		BufferedReader br = new BufferedReader(new FileReader(file));
		String strLine = "";
		String strD = "";

		while ((strLine = br.readLine()) != null) {
			strD += strLine;
			strLine = br.readLine();
		}

		br.close();

		 */
		return strD;
	}

	private static String getTemplateinstanceofRHS() throws IOException {
		String strD = Utils.getTemplatesTextString("ConstraintCrySLinstanceofClauseRHS");
		/*
		File file = new File(".\\src\\main\\resources\\Templates\\ConstraintCrySLinstanceofClauseRHS");
		BufferedReader br = new BufferedReader(new FileReader(file));
		String strLine = "";
		String strD = "";

		while ((strLine = br.readLine()) != null) {
			strD += strLine;
			strLine = br.readLine();
		}
		br.close();

		 */
		return strD;
	}

	public ArrayList<String> getInstanceof(CrySLRule rule) throws IOException {
		ArrayList<String> composedInstaceOf = new ArrayList<>();
		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		/*
		String classnamecheck = strArray.get((strArray.size()) - 1);
		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		 */
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
				String whopper = "";
				String dopper = whopper;
				System.out.println(dopper);

				if (conCryslISL instanceof CrySLConstraint) {

					if (((CrySLConstraint) conCryslISL).getLeft() instanceof CrySLConstraint) leftConstraint = (CrySLConstraint) ((CrySLConstraint) conCryslISL).getLeft();
					if (((CrySLConstraint) conCryslISL).getRight() instanceof CrySLConstraint) rightConstraint = (CrySLConstraint) ((CrySLConstraint) conCryslISL).getRight();

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
						DTMap.put(dt.getValue(), FunctionUtils.getDataType(rule, dt.getValue()));
					}

					String templatestringLHS = getTemplateinstanceofLHS();
					String templatestringRHS = getTemplateinstanceofRHS();

					String printout = "";
					String resultmainstringLHS = "";
					String resultmainstringRHS = "";

					for (int i = 0; i <= LHSList.size() - 1; i++) {

						if (i < 1) {

							String a = LHSList.get(i);
							List<String> resLHSlist = new ArrayList<>();
							List<String> finalpredmethodList = new ArrayList<>();
							String joined = null;

							if (a.contains("(") && a.contains(")")) {
								String result = StringUtils.substringBetween(a, "(", ")");
								resLHSlist = new ArrayList<>(Arrays.asList(result.split(",")));
							} else {
								resLHSlist = new ArrayList<>(
										Arrays.asList(a.replaceAll("VC:", "").replaceAll(",$", "").split(" - ")));
							}

							if (allNodesLeft == null) {
								allNodesLeft = new ArrayList<>();
								allNodesLeft.add(((CrySLConstraint)conCryslISL).getLeft());
							}
							for (String methodStr : methods) {
								String realLHS = ((CrySLObject)((CrySLPredicate)allNodesLeft.get(0)).getParameters().get(0)).getVarName();
								String real = ((CrySLObject)((CrySLPredicate)allNodesLeft.get(0)).getParameters().get(1)).getVarName();
								String LHSfirstStr = resLHSlist.get(0);
								resLHSlist.set(0,realLHS);
								resLHSlist.set(1,real);
//iCH  brauche key

								if (methodStr.contains(realLHS)) {

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

										finalpredmethodList.add(m);
										joined = String.join(", ", finalpredmethodList);

										String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
										List<String> strList = Arrays.asList(mStr.split(" "));
										String posStr = String.valueOf(strList.indexOf(realLHS));
										resLHSlist.add(posStr);

										if (posInWordsMap.containsKey(posStr)) {
											String posinwords = posInWordsMap.get(posStr);
											Collections.replaceAll(resLHSlist, posStr, posinwords);
											break;
										}
									}
								}
							}

							resLHSlist.add(joined);

							String varinstLHS = resLHSlist.get(1);
							String positioninstLHS = resLHSlist.get(2);
							String methodinstLHS = resLHSlist.get(resLHSlist.size() - 1);

							String b = templatestringLHS;

							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("positioni", positioninstLHS);
							valuesMap.put("methodnamei", methodinstLHS);
							valuesMap.put("vari1", varinstLHS);

							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							resultmainstringLHS = sub.replace(b);

						} else {

							String d = " or";
							String b = templatestringLHS;
							String a = LHSList.get(i);
							ISLConstraint currentConstraint = allNodesLeft.get(i);
							String leftSidePredicateOrVCvarname = null;
							if (currentConstraint instanceof CrySLPredicate) {
								CrySLPredicate predicate = (CrySLPredicate) currentConstraint;
								leftSidePredicateOrVCvarname = ((CrySLObject)predicate.getParameters().get(0)).getVarName();
								// Your code specific to CrySLPredicate
							} else if (currentConstraint instanceof CrySLValueConstraint) {
								CrySLValueConstraint valueConstraint = (CrySLValueConstraint) currentConstraint;
								leftSidePredicateOrVCvarname = valueConstraint.getVarName();
							} else {
								// Handle other cases if needed
								System.exit(255);
							}


							List<String> resLHSlistsecond = new ArrayList<>();
							List<String> finalpredmethodSecList = new ArrayList<>();
							String joinedSec = null;

							if (a.contains("(") && a.contains(")")) {
								String result = StringUtils.substringBetween(a, "(", ")");
								resLHSlistsecond = new ArrayList<>(Arrays.asList(result.split(",")));
							} else {

								resLHSlistsecond = new ArrayList<>(
										Arrays.asList(a.replaceAll("VC:", "").replaceAll(",$", "").split(" - ")));
							}

							for (String methodStr : methods) {

								String LHSfirstStr = leftSidePredicateOrVCvarname;

								if (methodStr.contains(LHSfirstStr)) {

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
										finalpredmethodSecList.add(m);
										joinedSec = String.join(", ", finalpredmethodSecList);

										String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
										List<String> strList = Arrays.asList(mStr.split(" "));
										String posStr = String.valueOf(strList.indexOf(LHSfirstStr));
										resLHSlistsecond.add(posStr);

										if (posInWordsMap.containsKey(posStr)) {
											String posinwords = posInWordsMap.get(posStr);
											Collections.replaceAll(resLHSlistsecond, posStr, posinwords);
											break;
										}
									}
								}
							}

							resLHSlistsecond.add(joinedSec);

							String varinstLHS2 = resLHSlistsecond.get(1);
							String positioninstLHS2 = resLHSlistsecond.get(2);
							String methodinstLHS2 = resLHSlistsecond.get(resLHSlistsecond.size() - 1);

							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("positioni", positioninstLHS2);
							valuesMap.put("methodnamei", methodinstLHS2);
							valuesMap.put("vari1", varinstLHS2);

							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							resultmainstringLHS += d + " " + sub.replace(b);
						}
					}

					String b = templatestringRHS;

					if (allNodesRight == null) {
						allNodesRight = new ArrayList<>();
						allNodesRight.add(((CrySLConstraint)conCryslISL).getRight());
					}



					for (String RHSStr : RHSList) {

						List<String> resRHSList = new ArrayList<>();
						resRHSList = new ArrayList<>(Arrays.asList(RHSStr.replaceAll("\\(.*\\)", "")
								.replaceAll("VC:", "").replaceAll(",$", "").split(" - ")));
						List<String> finalpredmethodRHSList = new ArrayList<>();
						String joinedRHS = null;


						for (String methodStr : methods) {
							String rightSidePredicateOrVCvarname = null;
							if (allNodesRight.get(0) instanceof CrySLPredicate) {
								CrySLPredicate predicate = (CrySLPredicate) allNodesRight.get(0);
								rightSidePredicateOrVCvarname = ((CrySLObject)predicate.getParameters().get(0)).getVarName();
								// Your code specific to CrySLPredicate
							} else if (allNodesRight.get(0) instanceof CrySLValueConstraint) {
								CrySLValueConstraint valueConstraint = (CrySLValueConstraint) allNodesRight.get(0);
								rightSidePredicateOrVCvarname = valueConstraint.getVarName();
								// Your code specific to CrySLValueConstraint
							} else {
								// Handle other cases if needed
								System.exit(255);
							}


							String RHSfirstStr = rightSidePredicateOrVCvarname;


							if (methodStr.contains(RHSfirstStr)) {

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

									finalpredmethodRHSList.add(m);
									joinedRHS = String.join(", ", finalpredmethodRHSList);
									String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
									List<String> strList = Arrays.asList(mStr.split(" "));
									String posStr = String.valueOf(strList.indexOf(RHSfirstStr));
									resRHSList.add(posStr);

									if (posInWordsMap.containsKey(posStr)) {

										String posinwords = posInWordsMap.get(posStr);
										Collections.replaceAll(resRHSList, posStr, posinwords);
										break;
									}
								}
							}
						}

						resRHSList.add(joinedRHS);

						String varinstRHS = resRHSList.get(1);
						String positioninstRHS = resRHSList.get(2);
						String methodinstRHS = resRHSList.get(resRHSList.size() - 1);

						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("positions", positioninstRHS);
						valuesMap.put("methodnames", methodinstRHS);
						valuesMap.put("vars2", varinstRHS);

						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						resultmainstringRHS = sub.replace(b);
					}

					printout = resultmainstringLHS + resultmainstringRHS;
					composedInstaceOf.add(printout);
					//out.println("" + printout);
				}
			}
		}
		//out.close();
		return composedInstaceOf;
	}

	public List<ISLConstraint> getAllLeafNodes(List<ISLConstraint> leafNodes, ISLConstraint node) {
		collectLeafNodes(node, leafNodes);
		return leafNodes;
	}

	// Helper method to recursively collect leaf nodes
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
