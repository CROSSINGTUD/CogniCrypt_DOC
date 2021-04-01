//case1.parameters contain "this" + method. case2. parameters contains "this"
package de.upb.docgen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import crypto.rules.CrySLCondPredicate;
import crypto.rules.CrySLMethod;
import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLRule;
import crypto.rules.StateMachineGraph;
import crypto.rules.TransitionEdge;
import de.upb.docgen.utils.Utils;

/**
 * @author Ritika Singh
 */

public class Ensures {

	static PrintWriter out;

	private static String getTemplateverbedge() throws IOException {
		File fileOne = new File(".\\src\\main\\resources\\Templates\\EnsuresClauseVerb-edge");
		BufferedReader brOne = new BufferedReader(new FileReader(fileOne));
		String strLineOne = "";
		String strDOne = "";

		while ((strLineOne = brOne.readLine()) != null) {
			strDOne += strLineOne + "\n";
			strLineOne = brOne.readLine();
		}
		brOne.close();
		return strDOne;
	}

	private static String getTemplateverbnounedge() throws IOException {
		File fileTwo = new File(".\\src\\main\\resources\\Templates\\EnsuresClauseVerb-noun-edge");
		BufferedReader brTwo = new BufferedReader(new FileReader(fileTwo));
		String strLineTwo = "";
		String strDTwo = "";

		while ((strLineTwo = brTwo.readLine()) != null) {
			strDTwo += strLineTwo + "\n";
			strLineTwo = brTwo.readLine();
		}
		brTwo.close();
		return strDTwo;
	}

	private static String getTemplateverbnoun() throws IOException {
		File fileThree = new File(".\\src\\main\\resources\\Templates\\EnsuresClauseVerb-noun");
		BufferedReader brThree = new BufferedReader(new FileReader(fileThree));
		String strLineThree = "";
		String strDThree = "";

		while ((strLineThree = brThree.readLine()) != null) {
			strDThree += strLineThree + "\n";
			strLineThree = brThree.readLine();
		}
		brThree.close();
		return strDThree;
	}

	private static String getTemplateverbnounedgeCon() throws IOException {
		File fileCon = new File(".\\src\\main\\resources\\Templates\\EnsuresClauseVerb-noun-edgeCons");
		BufferedReader brCon = new BufferedReader(new FileReader(fileCon));
		String strLineCon = "";
		String strDCon = "";

		while ((strLineCon = brCon.readLine()) != null) {
			strDCon += strLineCon + "\n";
			strLineCon = brCon.readLine();
		}
		brCon.close();
		return strDCon;
	}

	public void getEnsuresThis(CrySLRule rule) throws IOException {

		List<Entry<String, String>> dataTypes = rule.getObjects();
		Map<String, String> DTMap = new LinkedHashMap<>();

		for (Entry<String, String> dt : dataTypes) {
			DTMap.put(dt.getValue(), FunctionUtils.getDataType(rule, dt.getValue()));
		}

		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);

		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		StateMachineGraph smg = rule.getUsagePattern();
		List<TransitionEdge> edges = smg.getEdges();

		List<CrySLPredicate> predsThisList = rule.getPredicates().stream().filter(e -> e.toString().contains("this"))
				.collect(Collectors.toList()); 

		if (predsThisList.size() > 0) {

			for (CrySLPredicate elementStr : predsThisList) {

				String predTNameStr = elementStr.getPredName().toString();
				String str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(predTNameStr), ' ');
				List<String> verbOrNounList = Arrays.asList(str.split("\\s"));
				String verb;
				List<String> noun = new ArrayList<>();
				List<String> finalpredmethodThisNamesList = new ArrayList<>();
				String joined = null;

				if (elementStr instanceof CrySLCondPredicate) {

					CrySLCondPredicate conPred = (CrySLCondPredicate) elementStr;

					for (TransitionEdge edge : edges) {

						if (conPred.getConditionalMethods().contains(edge.to()) && !edge.to().equals(edge.from())) {
							List<String> predmethodThisNamesList = new ArrayList<String>();
							List<CrySLMethod> methodsThisedgeList = edge.getLabel();

							for (CrySLMethod methodT : methodsThisedgeList) {
								String[] preMTStrArr = methodT.toString().replace(".", ",").split(",");
								predmethodThisNamesList.add(preMTStrArr[preMTStrArr.length - 1].replace(";", "")
										.replaceAll("\\( ", "\\(").replaceAll(" ", ","));
							}

							for (String methodlistStr : predmethodThisNamesList) {
								List<String> extractParamList = new ArrayList<>();
								int startIndex = methodlistStr.indexOf("(");
								int endIndex = methodlistStr.indexOf(")");
								String bracketExtractStr = methodlistStr.substring(startIndex + 1, endIndex);

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
										methodlistStr = methodlistStr.replace(extractParamStr, value);
									}
								}

								finalpredmethodThisNamesList.add(methodlistStr);
								joined = String.join(" or ", finalpredmethodThisNamesList);
								//System.out.println(joined);
							}

							List<String> msplit = Arrays.asList(joined.split("\\("));

							if (verbOrNounList.size() == 1) {
								verb = verbOrNounList.get(0);
								String verbedge = getTemplateverbedge();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("verb", verb);
								valuesMap.put("edgeName", joined);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(verbedge);
								out.println(resolvedString);
							}

							else {
								verb = verbOrNounList.get(0);
								noun = verbOrNounList.subList(1, verbOrNounList.size());
								String nouns = String.join(" ", noun);

								if (msplit.get(0).contains(classnamecheck)) {

									String verbedge = getTemplateverbnounedgeCon();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("verb", verb);
									valuesMap.put("noun", nouns);
									valuesMap.put("edName", joined);

									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(verbedge);
									out.println(resolvedString);
									break;
								}
								String verbnounedge = getTemplateverbnounedge();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("verb", verb);
								valuesMap.put("noun", nouns);
								valuesMap.put("edName", joined);

								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(verbnounedge);
								out.println(resolvedString);
							}
							break;
						}
					}
				} else {

					if (verbOrNounList.size() == 1) {
						verb = verbOrNounList.get(0);

					} else {
						verb = verbOrNounList.get(0);
						noun = verbOrNounList.subList(1, verbOrNounList.size());
						String nouns = String.join(" ", noun);

						String verbnoun = getTemplateverbnoun();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("verb", verb);
						valuesMap.put("noun", nouns);

						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(verbnoun);
						out.println(resolvedString);
					}
				}
			}
		}
		out.close();
	}
}
