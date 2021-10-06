package de.upb.docgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.upb.docgen.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLRule;

/**
 * @author Ritika Singh
 */

public class ConstraintsPred {

	static PrintWriter out;

	private static char[] getTemplatePredOne() throws IOException {
		char[] buffOne = Utils.getTemplatesText("ConstraintPredTypeOne");
		/*
		File fileOne = new File(".\\src\\main\\resources\\Templates\\ConstraintPredTypeOne");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileOne), StandardCharsets.UTF_8);
		char[] buffOne = new char[500];
		for (int charsRead; (charsRead = reader.read(buffOne)) != -1;) {
			stringBuffer.append(buffOne, 0, charsRead);
		}
		reader.close();

		 */
		return buffOne;
	}

	private static char[] getTemplatePredTwo() throws IOException {
		char[] buffTwo = Utils.getTemplatesText("ConstraintPredTypeTwo");
		/*
		File fileTwo = new File(".\\src\\main\\resources\\Templates\\ConstraintPredTypeTwo");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileTwo), StandardCharsets.UTF_8);
		char[] buffTwo = new char[500];
		for (int charsRead; (charsRead = reader.read(buffTwo)) != -1;) {
			stringBuffer.append(buffTwo, 0, charsRead);
		}
		reader.close();

		 */
		return buffTwo;
	}

	private static char[] getTemplatePredThree() throws IOException {
		char[] buffThree = Utils.getTemplatesText("ConstraintPredTypeThree");
		/*
		File fileThree = new File(".\\src\\main\\resources\\Templates\\ConstraintPredTypeThree");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileThree), StandardCharsets.UTF_8);
		char[] buffThree = new char[500];
		for (int charsRead; (charsRead = reader.read(buffThree)) != -1;) {
			stringBuffer.append(buffThree, 0, charsRead);
		}
		reader.close();

		 */
		return buffThree;
	}

	private static char[] getTemplatePredFour() throws IOException {
		char[] buffFour = Utils.getTemplatesText("ConstraintPredTypeFour");
		/*
		File fileFour = new File(".\\src\\main\\resources\\Templates\\ConstraintPredTypeFour");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileFour), StandardCharsets.UTF_8);
		char[] buffFour = new char[500];
		for (int charsRead; (charsRead = reader.read(buffFour)) != -1;) {
			stringBuffer.append(buffFour, 0, charsRead);
		}
		reader.close();

		 */
		return buffFour;
	}

	private static char[] getTemplatePred1Con() throws IOException {
		char[] buffOneCon = Utils.getTemplatesText("ConstraintPredType1Con");
		/*
		File fileOneCon = new File(".\\src\\main\\resources\\Templates\\ConstraintPredType1Con");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileOneCon), StandardCharsets.UTF_8);
		char[] buffOneCon = new char[500];
		for (int charsRead; (charsRead = reader.read(buffOneCon)) != -1;) {
			stringBuffer.append(buffOneCon, 0, charsRead);
		}
		reader.close();

		 */
		return buffOneCon;
	}

	private static char[] getTemplatePred2Con() throws IOException {
		char[] buffOneCon = Utils.getTemplatesText("ConstraintPredType2Con");
		/*
		File fileTwoCon = new File(".\\src\\main\\resources\\Templates\\ConstraintPredType2Con");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileTwoCon), StandardCharsets.UTF_8);
		char[] buffTwoCon = new char[500];
		for (int charsRead; (charsRead = reader.read(buffTwoCon)) != -1;) {
			stringBuffer.append(buffTwoCon, 0, charsRead);
		}
		reader.close();

		 */
		return buffOneCon;
	}

	private static char[] getTemplatePred3Con() throws IOException {
		char[] buffThreeCon = Utils.getTemplatesText("ConstraintPredType3Con");
		/*
		File fileThreeCon = new File(".\\src\\main\\resources\\Templates\\ConstraintPredType3Con");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileThreeCon), StandardCharsets.UTF_8);
		char[] buffThreeCon = new char[500];
		for (int charsRead; (charsRead = reader.read(buffThreeCon)) != -1;) {
			stringBuffer.append(buffThreeCon, 0, charsRead);
		}
		reader.close();

		 */
		return buffThreeCon;
	}

	private static char[] getTemplatePred4Con() throws IOException {
		char[] buffFourCon = Utils.getTemplatesText("ConstraintPredType4Con");
		/*
		File fileFourCon = new File(".\\src\\main\\resources\\Templates\\ConstraintPredType4Con");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileFourCon), StandardCharsets.UTF_8);
		char[] buffFourCon = new char[500];
		for (int charsRead; (charsRead = reader.read(buffFourCon)) != -1;) {
			stringBuffer.append(buffFourCon, 0, charsRead);
		}
		reader.close();

		 */
		return buffFourCon;
	}

	public ArrayList<String> getConstraintsPred(CrySLRule rule, Set<String> ensuresForThis, Map<String, List<Map<String, List<String>>>> singleRuleEnsuresMap) throws IOException {
		ArrayList<String> composedConstraintsPredicates = new ArrayList<>();
		List<String> methodsList = FunctionUtils.getEventNames(rule);
		Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);
		List<Entry<String, String>> dataTypes = rule.getObjects();
		Map<String, String> DTMap = new LinkedHashMap<>();
		for (Entry<String, String> dt : dataTypes) {
			DTMap.put(dt.getValue(), FunctionUtils.getDataType(rule, dt.getValue()));
		}

		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);
		/*
		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));


		 */
		List<ISLConstraint> constraintPredList = rule.getConstraints().stream().filter(
				e -> e.getClass().getSimpleName().toString().contains("CrySLPredicate") && !e.toString().contains("!"))
				.collect(Collectors.toList());

		if (constraintPredList.size() > 0) {

			for (ISLConstraint consPredStr : constraintPredList) {

				String predString = consPredStr.toString();
				String pStr = predString.replaceAll("[()]", " ").replaceAll(",", " ");
				List<String> strList = Arrays.asList(pStr.split(" "));
				String var1 = null;
				String var2 = null;
				String var3 = null;

				Multimap<String, String> var2MethNameMap = ArrayListMultimap.create();
				Multimap<String, String> var2paraPosMap = ArrayListMultimap.create();

				String var2PosWordsStr = null;

				String camelCasePattern = "([a-z]+[A-Z]+\\w+)+";

				for (int u = 0; u < strList.size(); u++) {

					var1 = strList.get(0);
					var2 = strList.get(1);

					if (strList.size() > 2) {
						var3 = strList.get(2);
					}
				}

				List<String> resList = new ArrayList<>();

				for (String methodStr : methodsList) {

					String var2new = "\\b" + var2 + "\\b";
					Pattern p = Pattern.compile(var2new, Pattern.CASE_INSENSITIVE);
					Matcher mt = p.matcher(methodStr);

					if (mt.find()) {

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
									m = m.replace(extractParamStr, value);
								}
							}

							String methStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
							List<String> splitMethList = Arrays.asList(methStr.split(" "));
							String posStr = String.valueOf(splitMethList.indexOf(var2));
							var2MethNameMap.put(var2, m);
							var2paraPosMap.put(var2, posStr);
						}
					}
				}

				List<String> elist = new ArrayList<>();
				List<String> elistt = new ArrayList<>();

				for (Map.Entry<String, String> paraPosentry : var2paraPosMap.entries()) {

					List<String> pe = new ArrayList<>();
					pe = Arrays.asList(paraPosentry.toString().split("="));
					if (pe.get(0).equals(var2)) {
						if (posInWordsMap.containsKey(pe.get(1))) {
							var2PosWordsStr = posInWordsMap.get(pe.get(1));
							Collections.replaceAll(pe, pe.get(1), var2PosWordsStr);
						}
						elist.add(pe.get(1));
					}
				}

				for (Map.Entry<String, String> paraMethentry : var2MethNameMap.entries()) {

					List<String> pe = new ArrayList<>();
					pe = Arrays.asList(paraMethentry.toString().split("="));
					if (pe.get(0).equals(var2)) {
						elistt.add(pe.get(1));
					}
				}

				for (int i = 0; i < elist.size(); i++) {
					resList.add(elist.get(i) + "|" + elistt.get(i));
				}

				for (String rl : resList) {

					String l1 = rl;
					List<String> ls = Arrays.asList(l1.split("\\|"));
					String paraPos = ls.get(0);
					String mname = ls.get(1);

					List<String> msplit = Arrays.asList(ls.get(1).split("\\("));

					if (var1.matches(camelCasePattern)) {

						String str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(var1), ' ');
						List<String> verbOrNounList = Arrays.asList(str.split("\\s"));
						String verb = verbOrNounList.get(0);
						List<String> noun = verbOrNounList.subList(1, verbOrNounList.size());
						String nouns = String.join(" ", noun);
						List<Map<String, List<String>>> ensuresOfThisClassWithVariableName = singleRuleEnsuresMap.get(rule.getClassName());
						String ensures = "";
						for (Map<String, List <String>> maps : ensuresOfThisClassWithVariableName) {
							if (maps.containsKey(var1)) {
								ensures = maps.get(var1).get(0);
							}
						}
						//this links the right class which ensures something for the current rule
						//nouns = "<a href=\"" + ensures +".html\">" + nouns + "</a>";
						for (Map<String, List <String>> maps : ensuresOfThisClassWithVariableName) {
							if (maps.containsKey(var1)) {
								nouns = "<span class=\"tooltip\">" + nouns;
								String tooltiptext = "<span class=\"tooltiptext\">The following classes provide this predicate:\n";
								String classesLinks = htmlLinksClass(singleRuleEnsuresMap.get(rule.getClassName()), var1);
								String end = "</span></span>";

								nouns += tooltiptext + classesLinks + end;
							}
						}


						if (verb.endsWith("ed")) {

							if (msplit.get(0).equals(classnamecheck)) {

								char[] sOne = getTemplatePred1Con();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("position", paraPos);
								valuesMap.put("methodName", mname);
								valuesMap.put("verb", verb);
								valuesMap.put("nouns", nouns);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(sOne);
								composedConstraintsPredicates.add(resolvedString);
								//out.println(resolvedString);
							
							} else {

								char[] sOne = getTemplatePredOne();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("position", paraPos);
								valuesMap.put("methodName", mname);
								valuesMap.put("verb", verb);
								valuesMap.put("nouns", nouns);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(sOne);
								composedConstraintsPredicates.add(resolvedString);
								//out.println(resolvedString);
							}

						} else if (var3 != null && var3.equals("java.lang.String")) {

							if (msplit.get(0).equals(classnamecheck)) {
								// nevertypeof
								char[] sTwo = getTemplatePred2Con();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("position", paraPos);
								valuesMap.put("methodName", mname);
								valuesMap.put("var3", var3);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(sTwo);
								composedConstraintsPredicates.add(resolvedString);
								//out.println(resolvedString);

							} else {

								// nevertypeof
								char[] sTwo = getTemplatePredTwo();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("position", paraPos);
								valuesMap.put("methodName", mname);
								valuesMap.put("var3", var3);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(sTwo);
								composedConstraintsPredicates.add(resolvedString);
								//out.println(resolvedString);
							}

						} else if (var3 == null) {

							if (msplit.get(0).equals(classnamecheck)) {
								// nothardcoded
								char[] sThree = getTemplatePred3Con();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("position", paraPos);
								valuesMap.put("methodName", mname);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(sThree);
								composedConstraintsPredicates.add(resolvedString);
								//out.println(resolvedString);

							} else {

								// nothardcoded
								char[] sThree = getTemplatePredThree();
								Map<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("position", paraPos);
								valuesMap.put("methodName", mname);
								StringSubstitutor sub = new StringSubstitutor(valuesMap);
								String resolvedString = sub.replace(sThree);
								composedConstraintsPredicates.add(resolvedString);
								//out.println(resolvedString);
							}

						}

					} else {
						/*

						String ensures = "";
						for (Map<String, List <String>> maps : ensuresOfThisClassWithVariableName) {
							if (maps.containsKey(var1)) {
								ensures = maps.get(var1).get(0);
							}
						}

						 */
						List<Map<String, List<String>>> ensuresOfThisClassWithVariableName = singleRuleEnsuresMap.get(rule.getClassName());
						for (Map<String, List <String>> maps : ensuresOfThisClassWithVariableName) {
							if (maps.containsKey(var1)) {
								String classToLink = var1;
								var1 = "<span class=\"tooltip\">" + var1;
								String tooltiptext = "<span class=\"tooltiptext\">The following classes provide this predicate:\n";
								String classesLinks = htmlLinksClass(singleRuleEnsuresMap.get(rule.getClassName()), classToLink);
								String end = "</span></span>";

								var1 += tooltiptext + classesLinks + end;
							}
						}

						//var1 = "<a href=\"" + ensures +".html\">" + var1 + "</a>";
						if (msplit.get(0).equals(classnamecheck)) {

							char[] sFour = getTemplatePred4Con();
							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("position", paraPos);
							valuesMap.put("methodName", mname);
							valuesMap.put("var1", var1);
							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							String resolvedString = sub.replace(sFour);
							composedConstraintsPredicates.add(resolvedString);
							//out.println(resolvedString);

						} else {

							char[] sFour = getTemplatePredFour();
							Map<String, String> valuesMap = new HashMap<String, String>();
							valuesMap.put("position", paraPos);
							valuesMap.put("methodName", mname);
							valuesMap.put("var1", var1);
							StringSubstitutor sub = new StringSubstitutor(valuesMap);
							String resolvedString = sub.replace(sFour);
							composedConstraintsPredicates.add(resolvedString);
							//out.println(resolvedString);
						}
					}
				}
			}
		}
		//out.close();
		return composedConstraintsPredicates;
	}

	private String htmlLinksClass(List<Map<String, List<String>>> maps, String var1) {
		StringBuilder sb = new StringBuilder();
		for (Map<String, List <String>> map : maps) {
			if (map.containsKey(var1)) {
				for (String className : map.get(var1)) {
					sb.append("<a href=\"").append(className).append(".html\">").append(className).append("</a>\n");
				}
			}
		}
		return sb.toString();
	}
}