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

import crypto.rules.CrySLConstraint;
import crypto.rules.CrySLValueConstraint;
import de.upb.docgen.utils.Utils;
import org.apache.commons.text.StringSubstitutor;

import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLRule;

/**
 * @author Ritika Singh
 */

public class ConstraintCrySLVC {

	static PrintWriter out;

	private static String getTemplateVCLHS() throws IOException {
		return Utils.getTemplatesTextString("ConstraintCrySLVCClauseLHS");
		
	}

	private static String getTemplateVCRHS() throws IOException {
		return Utils.getTemplatesTextString("ConstraintCrySLVCClauseRHS");
		
	}

	public ArrayList<String> getConCryslVC(CrySLRule rule) throws IOException {
		ArrayList<String> composedConsraintsValueConstraints = new ArrayList<>();
		List<ISLConstraint> constraintConList = rule.getConstraints().stream()
				.filter(e -> e.getClass().getSimpleName().toString().contains("CrySLConstraint")
						&& !e.toString().contains("enc"))
				.collect(Collectors.toList());

		if (constraintConList.size() > 0) {

			for (ISLConstraint conCryslISL : constraintConList) {

				if (rule.getClassName().equals("javax.crypto.SecretKeyFactory")) {
					continue;
				}

				String conCryslStr = conCryslISL.toString();

				if (conCryslStr.startsWith("VC")) {

					List<Entry<String, String>> dataTypes = rule.getObjects();
					Map<String, String> DTMap = new LinkedHashMap<>();
					for (Entry<String, String> dt : dataTypes) {
						DTMap.put(dt.getKey(), FunctionUtils.getDataType(rule, dt.getKey()));
					}

					List<String> impSplitList = Arrays.asList(conCryslStr.split("implies"));
					List<String> LHSList = Arrays.asList(impSplitList.get(0).split("and"));
					List<String> RHSList = Arrays.asList(impSplitList.get(1));


					CrySLConstraint crySLConstraint = null;
					if (conCryslISL instanceof CrySLConstraint) crySLConstraint = (CrySLConstraint) conCryslISL;
					CrySLValueConstraint leftParam = null;
					if (crySLConstraint.getLeft() instanceof CrySLValueConstraint) {
						leftParam = (CrySLValueConstraint) crySLConstraint.getLeft();

					} else {
						continue;
					}
					if (conCryslISL instanceof CrySLConstraint) crySLConstraint = (CrySLConstraint) conCryslISL;
					CrySLValueConstraint rightParam = null;
					if (crySLConstraint.getRight() instanceof CrySLValueConstraint) {
						rightParam = (CrySLValueConstraint) crySLConstraint.getRight();

					} else {
						continue;
					}
					//CrySLValueConstraint rightParam = (CrySLValueConstraint) crySLConstraint.getRight();

					List<String> realLeft = new ArrayList<>();
					realLeft.add(leftParam.getVarName());
					for (String s : leftParam.getValueRange()) {
						realLeft.add(s);
					}
					//resLHSlist = leftCryslConstraint.var.varname,left.valuerange
					List<String> realRight = new ArrayList<>();
					realRight.add(rightParam.getVarName());
					for (String s : rightParam.getValueRange()) {
						realRight.add(s);
					}

					List<String> methods = FunctionUtils.getEventNamesKey(rule);
					Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);
					String templatestringLHS = getTemplateVCLHS();
					String templatestringRHS = getTemplateVCRHS();

					String printout = "";
					String resultmainstringLHS = "";
					String resultmainstringRHS = "";

					for (int i = 0; i <= LHSList.size() - 1; i++) {

						if (i < 1) {

							String a = LHSList.get(i);
							List<String> resLHSList = new ArrayList<>();
							resLHSList = new ArrayList<>(Arrays.asList(a.replaceAll("\\(.*\\)", "")
									.replaceAll("VC:", "").replaceAll(",$", " ").split(" - ")));
							List<String> finalpredmethodList = new ArrayList<>();
							String joined = null;

							for (String methodStr : methods) {

								if (methodStr.contains(realLeft.get(0))) {

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

										finalpredmethodList.add(m);
										joined = String.join(", ", finalpredmethodList);
										String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
										List<String> strList = Arrays.asList(mStr.split(" "));
										String posStr = String.valueOf(strList.indexOf(realLeft.get(0)));

										resLHSList.add(posStr);

										if (posInWordsMap.containsKey(posStr)) {

											String posinwords = posInWordsMap.get(posStr);
											Collections.replaceAll(resLHSList, posStr, posinwords);
										}
									}
								}
							}

							resLHSList.add(joined);

							String varlhsone = resLHSList.get(1);
							String poslhsone = resLHSList.get(2);
							String methlhsone = resLHSList.get(resLHSList.size() - 1);
							String b = templatestringLHS;

							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("positionVC", poslhsone);
							valuesMap.put("methodNameVC", methlhsone);
							valuesMap.put("varVC", varlhsone);

							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							resultmainstringLHS = sub.replace(b);
						}
						// next
						else {

							String d = "and ";
							String b = templatestringLHS;
							String a = LHSList.get(i);

							List<String> finalpredmethodList = new ArrayList<>();
							String joinedSec = null;

							List<String> resLHSlistsecond = new ArrayList<>(Arrays.asList(a.replaceAll("\\(.*\\)", "")
									.replaceAll("VC:", "").replaceAll(",$", " ").split(" - ")));

							for (String methodStr : methods) {
								String LHSfirstStr = resLHSlistsecond.get(0);
								String posStr;

								if (methodStr.contains(realLeft.get(0))) {

									List<String> methList = new ArrayList<>();
									methList.add(methodStr);

									for (String m : methList) {

										List<String> extractParamList = new ArrayList<>();
										int startIndex = m.indexOf("(");
										int endIndex = m.indexOf(")");
										String bracketExtractStr = m.substring(startIndex + 1, endIndex);

										if (bracketExtractStr.contains(",")) {
											String[] elements = bracketExtractStr.split(",");
											for (int a2 = 0; a2 < elements.length; a2++) {
												extractParamList.add(elements[a2]);
											}
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

										finalpredmethodList.add(m);
										joinedSec = String.join(", ", finalpredmethodList);

										String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
										List<String> strList = Arrays.asList(mStr.split(" "));
										posStr = String.valueOf(strList.indexOf(LHSfirstStr));
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
							String varlhstwo = resLHSlistsecond.get(1);
							String poslhstwo = resLHSlistsecond.get(2);
							String methlhstwo = resLHSlistsecond.get(resLHSlistsecond.size() - 1);

							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("positionVC", poslhstwo);
							valuesMap.put("methodNameVC", methlhstwo);
							valuesMap.put("varVC", varlhstwo);

							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							resultmainstringLHS += d + sub.replace(b);
						}
					}

					String b = templatestringRHS;

					for (String RHSStr : RHSList) {

						List<String> finalpredmethodRHSList = new ArrayList<>();
						String joinedRHS = null;

						List<String> resRHSList = new ArrayList<>();
						resRHSList = new ArrayList<>(Arrays.asList(RHSStr.replaceAll("\\(.*\\)", "")
								.replaceAll("VC:", "").replaceAll(",$", " ").split(" - ")));

						for (String methodStr : methods) {
							//String RHSfirstStr = resRHSList.get(0);
							String posStr;

							if (methodStr.contains(realRight.get(0))) {

								List<String> methList = new ArrayList<>();
								methList.add(methodStr);

								for (String m : methList) {

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
											String value = DTMap.get(extractParamStr).toString();
											m = m.replaceFirst(extractParamStr, value);
										}
									}

									finalpredmethodRHSList.add(m);
									joinedRHS = String.join(", ", finalpredmethodRHSList);

									String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
									List<String> strList = Arrays.asList(mStr.split(" "));
									posStr = String.valueOf(strList.indexOf(realRight.get(0)));
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

						String varrhsone = resRHSList.get(1);
						String posrhsone = resRHSList.get(2);
						String methrhsone = resRHSList.get(resRHSList.size() - 1);

						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("positionRVC", posrhsone);
						valuesMap.put("methodNameRVC", methrhsone);
						valuesMap.put("varRVC", varrhsone);

						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						resultmainstringRHS = sub.replace(b);
					}
					printout = resultmainstringLHS + resultmainstringRHS;
					composedConsraintsValueConstraints.add(printout);
				}
			}
		}
		return composedConsraintsValueConstraints;
	}
}
