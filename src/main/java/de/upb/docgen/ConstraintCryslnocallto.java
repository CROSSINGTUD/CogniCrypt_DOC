package de.upb.docgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLRule;

/**
 * @author Ritika Singh
 */

public class ConstraintCryslnocallto {

	static PrintWriter out;

	private static char[] getTemplatenocallto() throws IOException {
		char[] buffOne = Utils.getTemplatesText("ConsraintCrySLnocalltoClause");
		/*
		File file = new File(".\\src\\main\\resources\\Templates\\ConsraintCrySLnocalltoClause");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		char[] buffOne = new char[500];
		for (int charsRead; (charsRead = reader.read(buffOne)) != -1;) {
			stringBuffer.append(buffOne, 0, charsRead);
		}
		reader.close();

		 */
		return buffOne;
	}

	public ArrayList<String> getnoCalltoConstraint(CrySLRule rule) throws IOException {
		ArrayList<String> composedNocallToConstraints = new ArrayList<>();
		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);
		/*
		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		 */
		List<ISLConstraint> constraintConList = rule.getConstraints().stream()
				.filter(e -> e.getClass().getSimpleName().toString().contains("CrySLConstraint"))
				.collect(Collectors.toList());

		if (constraintConList.size() > 0) {

			for (ISLConstraint conCryslISL : constraintConList) {

				if (rule.getClassName().equals("javax.crypto.SecretKeyFactory")) {
					continue;
				}

				String conCryslStr = conCryslISL.toString();

				if (conCryslStr.startsWith("noCallTo")) {

					if (conCryslStr.contains("implies")) {
						List<String> impSplitList = Arrays.asList(conCryslStr.split("implies"));
						List<String> LHSList = Arrays.asList(impSplitList.get(0).split(","));
						List<String> RHSList = Arrays.asList(impSplitList.get(1));
						List<String> methods = FunctionUtils.getEventNames(rule);
						Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);

						List<Entry<String, String>> dataTypes = rule.getObjects();
						Map<String, String> DTMap = new LinkedHashMap<>();
						for (Entry<String, String> dt : dataTypes) {
							DTMap.put(dt.getValue(), FunctionUtils.getDataType(rule, dt.getValue()));
						}

						String LhsStr = "";
						String joinedstring = "";

						for (String LHSlistStr : LHSList) {
							List<String> lhsTempList = new ArrayList<>();
							List<String> fLHSList = new ArrayList<>();
							String joined = null;
							String[] LHSArr = LHSlistStr.replace(".", ",").split(",");
							lhsTempList.add(LHSArr[LHSArr.length - 1].replace(";)", "").replace(";", "")
									.replaceAll("\\( ", "\\(").replaceAll(" ", ","));

							for (String tempStr : lhsTempList) {

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
									String value = DTMap.get(extractParamStr).toString();
									tempStr = tempStr.replace(extractParamStr, value);
								}
								fLHSList.add(tempStr.replaceAll("\\[", "").replaceAll("\\]", ""));
								joined = String.join("", fLHSList);
							}

							LhsStr += joined + ", ";
							joinedstring = LhsStr.substring(0, LhsStr.lastIndexOf(","));

						}

						List<String> resRHSList = new ArrayList<>();

						for (String RHSStr : RHSList) {

							resRHSList = new ArrayList<>(Arrays.asList(RHSStr.replaceAll("\\(.*\\)", "")
									.replaceAll("VC:", "").replaceAll(",$", "").split(" - ")));

						}

						List<String> elist = new ArrayList<>();
						List<String> elistt = new ArrayList<>();

						for (String methodStr : methods) {

							String RHSfirstStr = resRHSList.get(0);

							if (methodStr.contains(RHSfirstStr)) {

								Multimap<String, String> paraMethNameMMap = ArrayListMultimap.create();
								Multimap<String, String> paraPosMMap = ArrayListMultimap.create();

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
									paraPosMMap.put(RHSfirstStr, posStr);
									paraMethNameMMap.put(RHSfirstStr, m);
								}

								for (Map.Entry<String, String> paraPosentry : paraPosMMap.entries()) {

									List<String> pe = new ArrayList<>();
									pe = Arrays.asList(paraPosentry.toString().split("="));
									if (pe.get(0).equals(RHSfirstStr)) {

										if (posInWordsMap.containsKey(pe.get(1))) {

											String paraPosInWordValStr = posInWordsMap.get(pe.get(1));
											Collections.replaceAll(pe, pe.get(1), paraPosInWordValStr);
										}
										elist.add(pe.get(1));
									}
								}

								for (Map.Entry<String, String> paraMethentry : paraMethNameMMap.entries()) {

									List<String> pe = new ArrayList<>();
									pe = Arrays.asList(paraMethentry.toString().split("="));
									if (pe.get(0).equals(RHSfirstStr)) {
										elistt.add(pe.get(1));
									}
								}
							}
						}

						List<String> resList = new ArrayList<>();

						for (int i = 0; i < elist.size(); i++) {
							resList.add(elist.get(i) + "|" + elistt.get(i));
						}

						for (String rl : resList) {

							String l1 = rl;
							List<String> ls = Arrays.asList(l1.split("\\|"));
							String paraPos = ls.get(0);
							String mname = ls.get(1);

							char[] str = getTemplatenocallto();
							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("methodNames", joinedstring);
							valuesMap.put("position", paraPos);
							valuesMap.put("method", mname);
							valuesMap.put("var2", resRHSList.get(1));

							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							String resolvedString = sub.replace(str);
							composedNocallToConstraints.add(resolvedString);

							//out.println(resolvedString);
						}

					}
				}
			}
		}
		//out.close();
		return composedNocallToConstraints;
	}
}
