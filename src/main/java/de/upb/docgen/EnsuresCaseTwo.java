//Negated ignored /case1. the parameters does not contains "this" but can have COND,  case2. the parameters does not contain this

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.upb.docgen.utils.Utils;
import org.apache.commons.lang3.StringUtils;
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

public class EnsuresCaseTwo {

	static PrintWriter out;

	private static Map<String, String> getReturnValues(CrySLRule rule) {
		Map<String, String> retValMap = new LinkedHashMap<>();

		ArrayList<TransitionEdge> transitions = new ArrayList<TransitionEdge>(
				rule.getUsagePattern().getAllTransitions());

		for (TransitionEdge transition : transitions) {
			List<CrySLMethod> methods = transition.getLabel();
			for (CrySLMethod method : methods) {
				if (method.toString().contains(" = ")) {
					String firstReturnTypeVar;
					String secondMethodNameVar;
					List<String> retEntry = Arrays
							.asList(method.toString().replaceAll("\\[", "").replaceAll("\\]", "").split(","));
					List<String> rfList = Arrays.asList(retEntry.get(0).split(" = "));
					firstReturnTypeVar = rfList.get(0);
					List<String> rsList = Arrays.asList(
							rfList.get(1).replace(".", ",").replaceAll("\\(.*\\)", "").replaceAll(";", "").split(","));
					secondMethodNameVar = rsList.get((rsList.size()) - 1);
					retValMap.put(firstReturnTypeVar, secondMethodNameVar);
				}
				//break;
			}
		}
		return retValMap;
	}

	private static String getTemplateReturnValueOne() throws IOException {
		String strDOne = Utils.getTemplatesTextString("EnsuresClauseReturnVal_verbmeth");
		/*
		File fileOne = new File(".\\src\\main\\resources\\Templates\\EnsuresClauseReturnVal_verbmeth");
		BufferedReader brOne = new BufferedReader(new FileReader(fileOne));
		String strLineOne = "";
		String strDOne = "";

		while ((strLineOne = brOne.readLine()) != null) {
			strDOne += strLineOne + "\n";
			strLineOne = brOne.readLine();
		}
		brOne.close();

		 */
		return strDOne;
	}

	private static String getTemplateReturnValueTwo() throws IOException {
		String strDTwo = Utils.getTemplatesTextString("EnsuresClauseReturnVal_verbnounmeth");
		/*
		File fileTwo = new File(".\\src\\main\\resources\\Templates\\EnsuresClauseReturnVal_verbnounmeth");
		BufferedReader brTwo = new BufferedReader(new FileReader(fileTwo));
		String strLineTwo = "";
		String strDTwo = "";

		while ((strLineTwo = brTwo.readLine()) != null) {
			strDTwo += strLineTwo + "\n";
			strLineTwo = brTwo.readLine();
		}
		brTwo.close();

		 */
		return strDTwo;
	}

	private static String getTemplateReturnValueThree() throws IOException {
		String strDTwo = Utils.getTemplatesTextString("EnsuresClauseReturnVal_verb");
		/*

		File fileThree = new File(".\\src\\main\\resources\\Templates\\EnsuresClauseReturnVal_verb");
		BufferedReader brThree = new BufferedReader(new FileReader(fileThree));
		String strLineThree = "";
		String strDThree = "";

		while ((strLineThree = brThree.readLine()) != null) {
			strDThree += strLineThree + "\n";
			strLineThree = brThree.readLine();
		}
		brThree.close();

		 */
		return strDTwo;
	}

	private static String getTemplateReturnValueFour() throws IOException {
		String strDFour = Utils.getTemplatesTextString("EnsuresClauseReturnVal_verbnoun");
		/*
		File fileFour = new File(".\\src\\main\\resources\\Templates\\EnsuresClauseReturnVal_verbnoun");
		BufferedReader brFour = new BufferedReader(new FileReader(fileFour));
		String strLineFour = "";
		String strDFour = "";

		while ((strLineFour = brFour.readLine()) != null) {
			strDFour += strLineFour + "\n";
			strLineFour = brFour.readLine();
		}
		brFour.close();

		 */
		return strDFour;
	}

	private static String getTemplateOne() throws IOException {
		String strDFive = Utils.getTemplatesTextString("Ensures-thisNA-verbmeth");
		/*
		File fileFive = new File(".\\src\\main\\resources\\Templates\\Ensures-thisNA-verbmeth");
		BufferedReader brFive = new BufferedReader(new FileReader(fileFive));
		String strLineFive = "";
		String strDFive = "";

		while ((strLineFive = brFive.readLine()) != null) {
			strDFive += strLineFive + "\n";
			strLineFive = brFive.readLine();
		}
		brFive.close();

		 */
		return strDFive;
	}

	private static String getTemplateTwo() throws IOException {
		String strDSix = Utils.getTemplatesTextString("Ensures-thisNA-verbnounmeth");
		/*
		File fileSix = new File(".\\src\\main\\resources\\Templates\\Ensures-thisNA-verbnounmeth");
		BufferedReader brSix = new BufferedReader(new FileReader(fileSix));
		String strLineSix = "";
		String strDSix = "";

		while ((strLineSix = brSix.readLine()) != null) {
			strDSix += strLineSix + "\n";
			strLineSix = brSix.readLine();
		}
		brSix.close();

		 */
		return strDSix;
	}

	private static String getTemplateThree() throws IOException {
		String strDSeven = Utils.getTemplatesTextString("Ensures-thisNA-verb");
		/*
		File fileSeven = new File(".\\src\\main\\resources\\Templates\\Ensures-thisNA-verb");
		BufferedReader brSeven = new BufferedReader(new FileReader(fileSeven));
		String strLineSeven = "";
		String strDSeven = "";

		while ((strLineSeven = brSeven.readLine()) != null) {
			strDSeven += strLineSeven + "\n";
			strLineSeven = brSeven.readLine();
		}
		brSeven.close();

		 */
		return strDSeven;
	}

	private static String getTemplateFour() throws IOException {
		String strDEight = Utils.getTemplatesTextString("Ensures-thisNA-verbnoun");
		/*
		File fileEight = new File(".\\src\\main\\resources\\Templates\\Ensures-thisNA-verbnoun");
		BufferedReader brEight = new BufferedReader(new FileReader(fileEight));
		String strLineEight = "";
		String strDEight = "";

		while ((strLineEight = brEight.readLine()) != null) {
			strDEight += strLineEight + "\n";
			strLineEight = brEight.readLine();
		}
		brEight.close();

		 */
		return strDEight;
	}

	public ArrayList<String> getEnsures(CrySLRule rule, Map<String, List<Map<String, List<String>>>> stringListMap) throws IOException {
		ArrayList<String> composedEnsures = new ArrayList<>();
		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);
/*
		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

 */

		String joined = null;
		List<Entry<String, String>> dataTypes = rule.getObjects();
		Map<String, String> DTMap = new LinkedHashMap<>();

		for (Entry<String, String> dt : dataTypes) {
			DTMap.put(dt.getValue(), FunctionUtils.getDataType(rule, dt.getValue()));
		}

		List<String> methodsNameList = FunctionUtils.getEventNames(rule);
		Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);
		Map<String, String> retTypeMap = getReturnValues(rule);
		
		StateMachineGraph smg = rule.getUsagePattern();
		List<TransitionEdge> edges = smg.getEdges();

		List<CrySLPredicate> predList = rule.getPredicates().stream()
				.filter(e -> !e.toString().contains("this") && !e.toString().contains("!"))
				.collect(Collectors.toList());

		for (CrySLPredicate elementN : predList) {

			String paramStr = elementN.getParameters().get(0).toString();
        	if (retTypeMap.containsKey(paramStr)) {

				String returnValMethod = retTypeMap.get(paramStr);

				String predNameStr = elementN.getPredName().toString();
				String str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(predNameStr), ' ');
				List<String> verbOrNounList = Arrays.asList(str.split("\\s"));
				String verb;
				List<String> noun = new ArrayList<>();

				if (elementN instanceof CrySLCondPredicate) {

					CrySLCondPredicate conPred = (CrySLCondPredicate) elementN;

					for (TransitionEdge edge : edges) {

						if (conPred.getConditionalMethods().contains(edge.to()) && !edge.to().equals(edge.from())) {
						
							List<String> predmethodNames = new ArrayList<String>();
							List<String> finalpredmethodNamesList = new ArrayList<>();
							List<CrySLMethod> methods = edge.getLabel();

							for (CrySLMethod method : methods) {
								String[] preM = method.toString().replace(".", ",").split(",");
								predmethodNames.add(preM[preM.length - 1].replace(";", "").replaceAll("\\( ", "\\(")
										.replaceAll(" ", ","));
							}

							for (String methodlistStr : predmethodNames) {
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
										int startInd = 0;
										int endInd = 0;
										String value = DTMap.get(extractParamStr).toString();

										Pattern word = Pattern.compile(extractParamStr);
										Matcher match = word.matcher(methodlistStr);

										while (match.find()) {
											startInd = match.start();
											endInd = match.end() - 1;
											break;
										}
										String strDiv = methodlistStr.substring(startInd, endInd + 1);
										if (strDiv.equals(extractParamStr)) {
											StringBuilder sDB = new StringBuilder(methodlistStr);
											sDB.replace(startInd, endInd + 1, value);
											methodlistStr = sDB.toString();
										}
									}
								}

								finalpredmethodNamesList.add(methodlistStr);
								joined = String.join(" or ", finalpredmethodNamesList);
								
							}

							if (verbOrNounList.size() == 1) {
								verb = verbOrNounList.get(0);

								String strRetOne = getTemplateReturnValueOne();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("returnValMethod", returnValMethod);
								valuesMap.put("verb", toHoverLink(rule, stringListMap, verb,predNameStr));
								valuesMap.put("methodName", joined);

								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(strRetOne);
								composedEnsures.add(resolvedString);
								//out.println(resolvedString);

							} else {
								verb = verbOrNounList.get(0);
								noun = verbOrNounList.subList(1, verbOrNounList.size());
								String nouns = String.join(" ", noun);

								String strRetTwo = getTemplateReturnValueTwo();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("returnValMethod", returnValMethod);
								valuesMap.put("verb", verb);
								valuesMap.put("nouns", toHoverLink(rule, stringListMap, nouns,predNameStr));
								valuesMap.put("joined", joined);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(strRetTwo);
								composedEnsures.add(resolvedString);
								//out.println(resolvedString);
							}
							break;
						}
					}
				} else {

					if (verbOrNounList.size() == 1) {
						verb = verbOrNounList.get(0);

						String strRetThree = getTemplateReturnValueThree();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("returnValMethod", returnValMethod);
						valuesMap.put("verb", toHoverLink(rule, stringListMap, verb ,predNameStr));
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(strRetThree);
						composedEnsures.add(resolvedString);
						//out.println(resolvedString);

					} else {
						verb = verbOrNounList.get(0);
						noun = verbOrNounList.subList(1, verbOrNounList.size());
						String nouns = String.join(" ", noun);

						String strRetFour = getTemplateReturnValueFour();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("returnValMethod", returnValMethod);
						valuesMap.put("verb", verb);
						valuesMap.put("nouns", toHoverLink(rule, stringListMap, nouns , predNameStr));
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(strRetFour);
						composedEnsures.add(resolvedString);
						//out.println(resolvedString);
					}
				}
			}

			else {

				Map<String, String> paraMethNameMap = new LinkedHashMap<>();
				Map<String, String> paraPosMap = new LinkedHashMap<>();
				String paraPosMapValStr = null;
				String paraPosInWordValStr = null;
				String paraMethNameMapValStr = null;

				for (String methodStr : methodsNameList) {
					String result = StringUtils.substringBetween(methodStr, "(", ")");
					List<String> resList = Arrays.asList(result.split(","));

					for (String rl : resList) {
						if (rl.equals(paramStr)) {
							String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
							List<String> strList = Arrays.asList(mStr.split(" "));
							String posStr = String.valueOf(strList.indexOf(paramStr));

							paraPosMap.put(paramStr, posStr);

							String m = methodStr;
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
									int startInd = 0;
									int endInd = 0;
									String value = DTMap.get(extractParamStr).toString();

									Pattern word = Pattern.compile(extractParamStr);
									Matcher match = word.matcher(m);

									while (match.find()) {
										startInd = match.start();
										endInd = match.end() - 1;
									}
									String strDiv = m.substring(startInd, endInd + 1);
									if (strDiv.equals(extractParamStr)) {
										StringBuilder sDB = new StringBuilder(m);
										sDB.replace(startInd, endInd + 1, value);
										m = sDB.toString();
									}
								}
							}

							paraMethNameMap.put(paramStr, m);
							break;
						}
					}
				}

				if (paraMethNameMap.containsKey(paramStr) && paraPosMap.containsKey(paramStr)) {
					paraPosMapValStr = paraPosMap.get(paramStr);
					paraMethNameMapValStr = paraMethNameMap.get(paramStr);
				}

				if (posInWordsMap.containsKey(paraPosMapValStr)) {
					paraPosInWordValStr = posInWordsMap.get(paraPosMapValStr);
				}

				String predNameStr = elementN.getPredName().toString();
				String str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(predNameStr), ' ');
				List<String> verbOrNounList = Arrays.asList(str.split("\\s"));
				String verb;
				List<String> noun = new ArrayList<>();

				if (elementN instanceof CrySLCondPredicate) {

					CrySLCondPredicate conPred = (CrySLCondPredicate) elementN;

					for (TransitionEdge edge : edges) {

						if (conPred.getConditionalMethods().contains(edge.to()) && !edge.to().equals(edge.from())) {

							List<String> predmethodNames = new ArrayList<String>();
							List<String> finalpredmethodNamesList = new ArrayList<>();
							List<CrySLMethod> methods = edge.getLabel();

							for (CrySLMethod method : methods) {
								String[] preM = method.toString().replace(".", ",").split(",");
								predmethodNames.add(preM[preM.length - 1].replace(";", "").replaceAll("\\( ", "\\(")
										.replaceAll(" ", ","));
							}

							for (String methodlistStr : predmethodNames) {
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

										int startInd = 0;
										int endInd = 0;
										String value = DTMap.get(extractParamStr).toString();

										Pattern word = Pattern.compile(extractParamStr);
										Matcher match = word.matcher(methodlistStr);

										while (match.find()) {
											startInd = match.start();
											endInd = match.end() - 1;
										}
										String strDiv = methodlistStr.substring(startInd, endInd + 1);
										if (strDiv.equals(extractParamStr)) {
											StringBuilder sDB = new StringBuilder(methodlistStr);
											sDB.replace(startInd, endInd + 1, value);
											methodlistStr = sDB.toString();
										}
									}
								}

								finalpredmethodNamesList.add(methodlistStr);
								joined = String.join(", ", finalpredmethodNamesList);
							}
							if (verbOrNounList.size() == 1) {
								verb = verbOrNounList.get(0);

								String strOne = getTemplateOne();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("paraPosInWordValStr", paraPosInWordValStr);
								valuesMap.put("paraMethNameMapValStr", paraMethNameMapValStr);
								valuesMap.put("verb", toHoverLink(rule, stringListMap, verb, predNameStr));
								valuesMap.put("joined", joined);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(strOne);
								//out.println(resolvedString);
								composedEnsures.add(resolvedString);

							} else {
								verb = verbOrNounList.get(0);
								noun = verbOrNounList.subList(1, verbOrNounList.size());
								String nouns = String.join(" ", noun);
								String strTwo = getTemplateTwo();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("paraPosInWordValStr", paraPosInWordValStr);
								valuesMap.put("paraMethNameMapValStr", paraMethNameMapValStr);
								valuesMap.put("verb", verb);
								valuesMap.put("nouns", toHoverLink(rule, stringListMap, nouns ,predNameStr));
								valuesMap.put("joined", joined);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(strTwo);
							    //out.println(resolvedString);
								composedEnsures.add(resolvedString);
							}
							break;
						}
					}
				} else {

					if (verbOrNounList.size() == 1) {
						verb = verbOrNounList.get(0);

						String strThree = getTemplateThree();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("paraPosInWordValStr", paraPosInWordValStr);
						valuesMap.put("paraMethNameMapValStr", paraMethNameMapValStr);
						valuesMap.put("verb", toHoverLink(rule, stringListMap, verb ,predNameStr));
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(strThree);
						//out.println(resolvedString);
						composedEnsures.add(resolvedString);

					} else {
						verb = verbOrNounList.get(0);
						noun = verbOrNounList.subList(1, verbOrNounList.size());
						String nouns = String.join(" ", noun);

						String strFour = getTemplateFour();
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("paraPosInWordValStr", paraPosInWordValStr);
						valuesMap.put("paraMethNameMapValStr", paraMethNameMapValStr);
						valuesMap.put("verb", verb);
						valuesMap.put("nouns", toHoverLink(rule, stringListMap, nouns,predNameStr));
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(strFour);
						//out.println(resolvedString);
						composedEnsures.add(resolvedString);
						
					}
				}
			}			
		}
		//out.close();
		return composedEnsures;
	}

	private String toHoverLink(CrySLRule rule, Map<String, List<Map<String, List<String>>>> stringListMap, String word, String predicate) {
		List<Map<String, List<String>>> requiresOfClasses = stringListMap.get(rule.getClassName());
		for (Map<String, List<String>> maps : requiresOfClasses) {
			if (maps.containsKey(predicate)) {
				String classToLink = word;
				word = "<span class=\"tooltip\">" + word;
				String tooltiptext = "<span class=\"tooltiptext\">The following classes can require this predicate:\n";
				String classesLinks = htmlLinksClass(stringListMap.get(rule.getClassName()), classToLink, predicate);
				String end = "</span></span>";

				word += tooltiptext + classesLinks + end;
			}
		}
		return word;
	}

	private String htmlLinksClass(List<Map<String, List<String>>> maps, String var1, String predicate) {
		StringBuilder sb = new StringBuilder();
		for (Map<String, List <String>> map : maps) {
			if (map.containsKey(predicate)) {
				for (String className : map.get(predicate)) {
					sb.append("<a href=\"").append(className).append(".html\">").append(className).append("</a>\n");
				}
			}
		}
		return sb.toString();
	}
}
