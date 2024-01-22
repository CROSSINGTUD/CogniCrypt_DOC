package de.upb.docgen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
		String strD = Utils.getTemplatesTextString("ConstraintCrySLVCClauseLHS");
		/*
		File file = new File(".\\src\\main\\resources\\Templates\\ConstraintCrySLVCClauseLHS");
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

	private static String getTemplateVCRHS() throws IOException {
		String strD = Utils.getTemplatesTextString("ConstraintCrySLVCClauseRHS");
		/*
		File file = new File(".\\src\\main\\resources\\Templates\\ConstraintCrySLVCClauseRHS");
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

	public ArrayList<String> getConCryslVC(CrySLRule rule) throws IOException {
		ArrayList<String> composedConsraintsValueConstraints = new ArrayList<>();
		String cname = rule.getClassName().replace(".", ",");
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);
		/*
		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		 */
		List<ISLConstraint> constraintConList = rule.getConstraints().stream()
				.filter(e -> e.getClass().getSimpleName().contains("CrySLConstraint")
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
						DTMap.put(dt.getValue(), FunctionUtils.getDataType(rule, dt.getValue()));
					}

					List<String> impSplitList = Arrays.asList(conCryslStr.split("implies"));
					List<String> LHSList = Arrays.asList(impSplitList.get(0).split("and"));
					List<String> RHSList = Arrays.asList(impSplitList.get(1));

					List<String> methods = FunctionUtils.getEventNames(rule);
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
								String LHSfirstStr = resLHSList.get(0);

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
												String value = DTMap.get(extractParamStr);
												m = m.replaceFirst(extractParamStr, value);
											}
										}

										finalpredmethodList.add(m);
										joined = String.join(", ", finalpredmethodList);
										String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
										List<String> strList = Arrays.asList(mStr.split(" "));
										String posStr = String.valueOf(strList.indexOf(LHSfirstStr));

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

							List<String> resLHSlistsecond = new ArrayList<>();
							resLHSlistsecond = new ArrayList<>(Arrays.asList(a.replaceAll("\\(.*\\)", "")
									.replaceAll("VC:", "").replaceAll(",$", " ").split(" - ")));

							for (String methodStr : methods) {
								String LHSfirstStr = resLHSlistsecond.get(0);
								String posStr = null;

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
							String RHSfirstStr = resRHSList.get(0);
							String posStr = null;

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
										for (int a = 0; a < elements.length; a++) {
											extractParamList.add(elements[a]);
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

									finalpredmethodRHSList.add(m);
									joinedRHS = String.join(", ", finalpredmethodRHSList);

									String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
									List<String> strList = Arrays.asList(mStr.split(" "));
									posStr = String.valueOf(strList.indexOf(RHSfirstStr));
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
					//out.println("" + printout);
				}
			}
		}
		//out.close();
		return composedConsraintsValueConstraints;
	}
}
