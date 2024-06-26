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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import crypto.rules.*;
import de.upb.docgen.utils.Utils;
import org.apache.commons.text.StringSubstitutor;

import crypto.interfaces.ISLConstraint;

/**
 * @author Ritika Singh
 */
public class ConstraintCrySLandencmode {

	static PrintWriter out;

	private static String getTemplateEncLHS() throws IOException {
		return Utils.getTemplatesTextString("ConstraintCrySLVCandEncmodeLHS1Clause");

	}

	private static String getTemplateEncCallLHS2() throws IOException {
		return Utils.getTemplatesTextString("ConstraintCrySLVCandEncmodeCallLHS2Clause");
	}

	private static String getTemplateEncCallRHS() throws IOException {
		return Utils.getTemplatesTextString("ConstraintCrySLVCandEncmodeCallRHSClause");
	}

	private static String getTemplateEncNoCallLHS2() throws IOException {
		return Utils.getTemplatesTextString("ConstraintCrySLVCandEncmodeNocallLHS2Clause");

	}

	private static String getTemplateEncNoCallRHS() throws IOException {
		return Utils.getTemplatesTextString("ConstraintCrySLVCandEncmodeNocallLRHSClause");

	}

	private static Map<String, String> getwordMap(CrySLRule rule) {

		Map<String, String> posInWords = new HashMap<>();
		posInWords.put("1", "one");
		posInWords.put("2", "two");

		return posInWords;

	}

	public ArrayList<String> getConCryslandenc(CrySLRule rule) throws IOException {
		ArrayList<String> composedConAndEnc = new ArrayList<>();

		List<ISLConstraint> constraintConencmodeList = rule.getConstraints().stream()
				.filter(e -> e.getClass().getSimpleName().toString().contains("CrySLConstraint")
						&& e.toString().contains("int encmode"))
				.collect(Collectors.toList());

		if (constraintConencmodeList.size() > 0) {

			for (ISLConstraint conCryslISL : constraintConencmodeList) {

				if (rule.getClassName().equals("javax.crypto.SecretKeyFactory")) {
					continue;
				}

				String conCryslStr = conCryslISL.toString();

				if (conCryslStr.startsWith("VC")) {

					List<String> impSplitList = Arrays.asList(conCryslStr.split("implies"));
					List<String> LHSList = Arrays.asList(impSplitList.get(0).split("and"));
					List<String> RHSList = Arrays.asList(impSplitList.get(1));

					List<String> methods = FunctionUtils.getEventNamesKey(rule);
					Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);
					List<Entry<String, String>> dataTypes = rule.getObjects();
					Map<String, String> DTMap = new LinkedHashMap<>();
					for (Entry<String, String> dt : dataTypes) {
						DTMap.put(dt.getKey(), dt.getValue());
					}

					String templatestringEncLHS = getTemplateEncLHS();
					String templatestringEncCallLHS2 = getTemplateEncCallLHS2();
					String templatestringEncCallRHS = getTemplateEncCallRHS();
					String templatestringEncNoCallLHS2 = getTemplateEncNoCallLHS2();
					String templatestringEncNoCallRHS = getTemplateEncNoCallRHS();

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
								resLHSList.get(0);

								CrySLConstraint crySLConstraint = (CrySLConstraint) conCryslISL;
								CrySLConstraint leftConstraint = (CrySLConstraint) crySLConstraint.getLeft();
								CrySLValueConstraint LeftValueConstraint = (CrySLValueConstraint) leftConstraint
										.getLeft();
								CrySLObject CrySLObject = (CrySLObject) LeftValueConstraint.getVar();
								String varname = CrySLObject.getVarName();

								if (methodStr.contains(varname)) {

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
										String posStr = String.valueOf(strList.indexOf(varname));
										resLHSList.add(posStr);

										if (posInWordsMap.containsKey(posStr)) {
											String posinwords = posInWordsMap.get(posStr);
											Collections.replaceAll(resLHSList, posStr, posinwords);
											break;
										}
									}
								}
							}

							resLHSList.add(joined);

							String varlhsone = resLHSList.get(1);
							String poslhsone = resLHSList.get(2);
							String methlhsone = resLHSList.get(resLHSList.size() - 1);
							String b = templatestringEncLHS;

							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("positionVC", poslhsone);
							valuesMap.put("methodNameVC", methlhsone);
							valuesMap.put("varVC", varlhsone);

							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							resultmainstringLHS = sub.replace(b);

						} else {
							String d = "and";
							String a = LHSList.get(i);
							String b = "";
							Map<String, String> wordMap = getwordMap(rule);

							List<String> resLHSlistsecond = new ArrayList<>();
							resLHSlistsecond = new ArrayList<>(
									Arrays.asList(a.replaceFirst("^0+(?!$)", "").split(" ")));
							List<String> finalpredmethodRHSList = new ArrayList<>();
							String joinedRHS = null;

							for (String res : resLHSlistsecond) {
								if (wordMap.containsKey(res)) {

									String word = wordMap.get(res);
									Collections.replaceAll(resLHSlistsecond, res, word);
									break;
								}
							}

							for (String methodStr : methods) {
								String LHSfirstStr = resLHSlistsecond.get(0);

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

										finalpredmethodRHSList.add(m);
										joinedRHS = String.join(", ", finalpredmethodRHSList);

										String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
										List<String> strList = Arrays.asList(mStr.split(" "));
										String posStr = String.valueOf(strList.indexOf(LHSfirstStr));
										resLHSlistsecond.add(posStr);
										resLHSlistsecond.add(joinedRHS);

										if (posInWordsMap.containsKey(posStr)) {
											String posinwords = posInWordsMap.get(posStr);
											Collections.replaceAll(resLHSlistsecond, posStr, posinwords);
											break;
										}

									}
								}
							}

							for (int p = 0; p <= resLHSlistsecond.size(); p++) {

								if (resLHSlistsecond.contains("!=")) {

									int indexsym = resLHSlistsecond.indexOf("!=");
									String noteqVal = resLHSlistsecond.get(indexsym + 2);
									b = templatestringEncNoCallLHS2;

									String poslhstwo = resLHSlistsecond.get(resLHSlistsecond.size() - 2);
									String methlhstwo = resLHSlistsecond.get(resLHSlistsecond.size() - 1);

									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("positionec", poslhstwo);
									valuesMap.put("methodnameec", methlhstwo);
									valuesMap.put("numericval", noteqVal);

									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									resultmainstringLHS += d + " " + sub.replace(b);
									break;
								}

								else if (resLHSlistsecond.contains("=")) {

									int indexsym = resLHSlistsecond.indexOf("=");
									String eqVal = resLHSlistsecond.get(indexsym + 1);
									b = templatestringEncCallLHS2;

									String poslhstwo = resLHSlistsecond.get(resLHSlistsecond.size() - 2);
									String methlhstwo = resLHSlistsecond.get(resLHSlistsecond.size() - 1);

									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("positionec", poslhstwo);
									valuesMap.put("methodnameec", methlhstwo);
									valuesMap.put("numericval", eqVal);

									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									resultmainstringLHS += d + " " + sub.replace(b);
									break;
								}
							}
						}
					}
					// rhs
					for (String RHSStr : RHSList) {

						if (RHSStr.startsWith("noCall")) {
							// List<String> NCList = Arrays.asList(RHSStr.split(","));
							List<String> NCList = extractMethodSignatures(RHSStr);
							String nocall = "";
							String finalnocallstring = "";

							for (String nc : NCList) {

								List<String> fList = new ArrayList<>();
								String joinedMethods = "";
								String b = templatestringEncNoCallRHS;

								nc.replace(".", ",").split(",");

								String tempStr = extractMethodParameters(nc);

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

									String[] parts = extractParamStr.trim().split(" ");
									String value = DTMap.get(parts[1]);
									tempStr = tempStr.replace(extractParamStr, value);
								}
								fList.add(tempStr.replaceAll("\\[", "").replaceAll("\\]", ""));
								joinedMethods = String.join("", fList);

								nocall += joinedMethods + ", ";
								finalnocallstring = nocall.substring(0, nocall.lastIndexOf(","));

								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("nocallmethName", finalnocallstring);

								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								resultmainstringRHS = sub.replace(b);

							}

						} else if (RHSStr.startsWith("call")) {

							List<String> CList = Arrays.asList(RHSStr.split(","));
							String call = "";
							String finalcallstring = "";

							for (String cl : CList) {

								List<String> cTempList = new ArrayList<>();
								List<String> fList = new ArrayList<>();
								String joinedMethods = "";
								String b = templatestringEncCallRHS;

								String[] clArr = cl.replace(".", ",").split(",");
								cTempList.add(clArr[clArr.length - 1].replace(";)", "").replace(";", "")
										.replaceAll("\\( ", "\\(").replaceAll(" ", ","));

								for (String tempStr : cTempList) {
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

									fList.add(tempStr.replaceAll("\\[", "").replaceAll("\\]", ""));
									joinedMethods = String.join("", fList);

								}
								call += joinedMethods + ", ";
								finalcallstring = call.substring(0, call.lastIndexOf(","));

								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("callmethName", finalcallstring);

								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								resultmainstringRHS = sub.replace(b);
							}
						}
					}
					printout = resultmainstringLHS + resultmainstringRHS;
					composedConAndEnc.add(printout);
					// out.println(printout);
				}
			}
		}
		// out.close();
		return composedConAndEnc;
	}

	public static ArrayList<String> extractMethodSignatures(String input) {
		ArrayList<String> methodSignatures = new ArrayList<>();

		Pattern pattern = Pattern.compile("(\\w+\\.\\w+\\([^)]+\\))");
		Matcher matcher = pattern.matcher(input);

		while (matcher.find()) {
			methodSignatures.add(matcher.group(1));
		}

		return methodSignatures;
	}

	public static String extractMethodParameters(String methodSignature) {
		// Define a regular expression to match parameters
		String regex = "\\((.*?)\\)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(methodSignature);

		if (matcher.find()) {
			return "(" + matcher.group(1) + ")";
		}

		return null;
	}
}
