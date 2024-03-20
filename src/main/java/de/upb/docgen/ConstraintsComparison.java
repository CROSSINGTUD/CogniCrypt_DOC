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

import crypto.interfaces.ICrySLPredicateParameter;
import crypto.rules.*;
import de.upb.docgen.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import crypto.interfaces.ISLConstraint;

/**
 * @author Ritika Singh
 */

public class ConstraintsComparison {

	static PrintWriter out;


	private static String mapToOperator(String arithOp) {
		switch (arithOp) {
			case "p":
				return "+";
			case "n":
				return "-";
			case "m":
				return "%";
			case "l":
				return "<";
			case "le":
				return "<=";
			case "g":
				return ">";
			case "ge":
				return ">=";
			case "neq":
				return "!=";
			case "eq":
				return "=";
			default:
				throw new IllegalArgumentException("Unsupported ArithOp: " + arithOp);
		}
	}

	private static char[] getTemplateCompOne() throws IOException {
		char[] buffOne = Utils.getTemplatesText("CompConstraint_lengthgreaterequal");
		/*
		File fileOne = new File(".\\src\\main\\resources\\Templates\\CompConstraint_lengthgreaterequal");
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

	private static char[] getTemplateComptwo() throws IOException {
		char[] buffTwo = Utils.getTemplatesText("CompConstraint_lengthlesssum");
		/*
		File fileTwo = new File(".\\src\\main\\resources\\Templates\\CompConstraint_lengthlesssum");
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

	private static char[] getTemplateCompThree() throws IOException {
		char[] buffThree = Utils.getTemplatesText("CompConstraint_lengthless");
		/*
		File fileThree = new File(".\\src\\main\\resources\\Templates\\CompConstraint_lengthless");
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

	private static char[] getTemplateCompFour() throws IOException {
		char[] buffFour = Utils.getTemplatesText("CompConstraint_greaterequal");
		/*
		File fileFour = new File(".\\src\\main\\resources\\Templates\\CompConstraint_greaterequal");
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

	private static char[] getTemplateCompFive() throws IOException {
		char[] buffFive = Utils.getTemplatesText("CompConstraint_greater");
		/*
		File fileFive = new File(".\\src\\main\\resources\\Templates\\CompConstraint_greater");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileFive), StandardCharsets.UTF_8);
		char[] buffFive = new char[500];
		for (int charsRead; (charsRead = reader.read(buffFive)) != -1;) {
			stringBuffer.append(buffFive, 0, charsRead);
		}
		reader.close();
		 */
		return buffFive;
	}

	private static char[] getTemplateCompSix() throws IOException {
		char[] buffSix = Utils.getTemplatesText("CompConstraint_less");
		/*
		File fileSix = new File(".\\src\\main\\resources\\Templates\\CompConstraint_less");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileSix), StandardCharsets.UTF_8);
		char[] buffSix = new char[500];
		for (int charsRead; (charsRead = reader.read(buffSix)) != -1;) {
			stringBuffer.append(buffSix, 0, charsRead);
		}
		reader.close();

		 */
		return buffSix;
	}

	private static char[] getTemplateCompSeven() throws IOException {
		char[] buffseven = Utils.getTemplatesText("CompConstraint_lengthgreater");
		/*
		File fileseven = new File(".\\src\\main\\resources\\Templates\\CompConstraint_lengthgreater");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileseven), StandardCharsets.UTF_8);
		char[] buffseven = new char[500];
		for (int charsRead; (charsRead = reader.read(buffseven)) != -1;) {
			stringBuffer.append(buffseven, 0, charsRead);
		}
		reader.close();

		 */
		return buffseven;
	}

	private static char[] getTemplateCompCons1() throws IOException {
		char[] buffSeven = Utils.getTemplatesText("CompConstraint_lengthgreaterequalCon");
		/*
		File fileSeven = new File(".\\src\\main\\resources\\Templates\\CompConstraint_lengthgreaterequalCon");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileSeven), StandardCharsets.UTF_8);
		char[] buffSeven = new char[500];
		for (int charsRead; (charsRead = reader.read(buffSeven)) != -1;) {
			stringBuffer.append(buffSeven, 0, charsRead);
		}
		reader.close();

		 */
		return buffSeven;
	}

	private static char[] getTemplateCompCons2() throws IOException {
		char[] buffEight = Utils.getTemplatesText("CompConstraint_greaterequalCon");
		/*
		File fileEight = new File(".\\src\\main\\resources\\Templates\\CompConstraint_greaterequalCon");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(fileEight), StandardCharsets.UTF_8);
		char[] buffEight = new char[500];
		for (int charsRead; (charsRead = reader.read(buffEight)) != -1;) {
			stringBuffer.append(buffEight, 0, charsRead);
		}
		reader.close();

		 */
		return buffEight;
	}

	public ArrayList<String> getConstriantsComp(CrySLRule rule) throws IOException {
		ArrayList<String> composedComparsionConstraint =  new ArrayList<>();
		List<ISLConstraint> constraintCompConList = rule.getConstraints().stream()
				.filter(e -> e.getClass().getSimpleName().toString().contains("CrySLComparisonConstraint"))
				.collect(Collectors.toList());
		List<Entry<String, String>> dataTypes = rule.getObjects();
		Map<String, String> DTMap = new LinkedHashMap<>();

		for (Entry<String, String> dt : dataTypes) {
			DTMap.put(dt.getKey(), dt.getValue());
		}

		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);
		/*
		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		 */
		if (constraintCompConList.size() > 0) {

			List<String> subListLHS = new ArrayList<>();
			List<String> subListRHS = new ArrayList<>();
			String symbolStr;

			List<String> methods = FunctionUtils.getEventNamesKey(rule);
			Map<String, String> posInWordsMap = FunctionUtils.getPosWordMap(rule);

			for (ISLConstraint compCon : constraintCompConList) {

				Multimap<String, String> paraMethNameMap = ArrayListMultimap.create();
				Multimap<String, String> paraPosMap = ArrayListMultimap.create();

				String compStr = compCon.toString();

				CrySLComparisonConstraint crySLComparisonConstraint = (CrySLComparisonConstraint) compCon;
				CrySLArithmeticConstraint leftArit = crySLComparisonConstraint.getLeft();
				String compStrReal;

// Check if getLeft is a CrySLPredicate
				if (leftArit.getLeft() instanceof CrySLPredicate) {
					CrySLPredicate leftAritPrd = (CrySLPredicate) leftArit.getLeft();

					// Your existing code for processing CrySLPredicate
					List<String> leftAritPrdVarNames = new ArrayList<>();
					for (ICrySLPredicateParameter e : leftAritPrd.getParameters()) {
						if (e instanceof CrySLObject) {
							CrySLObject crySLObject = (CrySLObject) e;
							leftAritPrdVarNames.add(crySLObject.getVarName());
						} else {
							// Handle other cases if needed
						}
					}

					compStrReal = leftAritPrd.getPredName() + "(" + StringUtils.join(leftAritPrdVarNames, ",") + ")";
				} else if (leftArit.getLeft() instanceof CrySLObject) {
					// Handle the case where getLeft is a CrySLObject
					CrySLObject leftArtObj = (CrySLObject) leftArit.getLeft();
					compStrReal = leftArtObj.getVarName();
				} else {
					// Handle other cases if needed
					compStrReal = ""; // Update this with appropriate handling
				}

// Continue with the operator and rightArit
				compStrReal += " " + mapToOperator(leftArit.getOperator().toString()) + " ";

// Repeat the similar logic for getRight
				if (leftArit.getRight() instanceof CrySLPredicate) {
					CrySLPredicate rightAritPrd = (CrySLPredicate) leftArit.getRight();

					// Your existing code for processing CrySLPredicate
					List<String> rightAritPrdVarNames = new ArrayList<>();
					for (ICrySLPredicateParameter e : rightAritPrd.getParameters()) {
						if (e instanceof CrySLObject) {
							CrySLObject crySLObject = (CrySLObject) e;
							rightAritPrdVarNames.add(crySLObject.getVarName());
						} else {
							// Handle other cases if needed
						}
					}

					compStrReal += rightAritPrd.getPredName() + "(" + StringUtils.join(rightAritPrdVarNames, ",") + ")";
				} else if (leftArit.getRight() instanceof CrySLObject) {
					// Handle the case where getRight is a CrySLObject
					CrySLObject rightArtObj = (CrySLObject) leftArit.getRight();
					compStrReal += rightArtObj.getVarName();
				} else {
					// Handle other cases if needed
					compStrReal += ""; // Update this with appropriate handling
				}

// Now compStrReal contains the fully constructed string


				CrySLArithmeticConstraint rightArit = crySLComparisonConstraint.getRight();




				// length + ( + varnames + ) + ooperator + int
				// add operator
				compStrReal += " "+ mapToOperator(crySLComparisonConstraint.getOperator().toString()) +" ";
				// add rightside

//


				List<LeafNodeWithOperator> rightOperations = new ArrayList<>();
				ArithmeticNode root = buildTree(rightArit);
				String equation = root.buildEquation();
				System.out.println("Reconstructed Equation: " + equation);


				collectLeafNodes(rightArit, rightOperations);



				//compStrReal += ((CrySLArithmeticConstraint)rightArit.getLeft()).() + mapToOperator(rightArit.getOperator().toString()) + ((CrySLArithmeticConstraint)rightArit.getRight()).getVarName();

				compStrReal += equation;

				compStr = compStrReal;





				if (compStr.contains("length")) {

					List<String> splitCompList = Arrays
							.asList(compStr.replaceAll("[()]", " ").replaceAll("\\s+", " ").split(" "));
					// length(pre_plaintext) + 0 >= pre_plain_off + len

					for (String complistStr : splitCompList) {

						for (String methodStr : methods) {

							String result = StringUtils.substringBetween(methodStr, "(", ")");
							List<String> resList = Arrays.asList(result.split(","));

							for (String r : resList) {

								if (r.equals(complistStr)) {

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
											String value = DTMap.get(extractParamStr).toString();
											m = m.replaceFirst(extractParamStr, value);
										}
									}

									paraMethNameMap.put(complistStr, m);

									String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
									List<String> strList = Arrays.asList(mStr.split(" "));
									String posStr = String.valueOf(strList.indexOf(complistStr));
									paraPosMap.put(complistStr, posStr);
								}
							}
						}
					}

					for (int index = 0; index < splitCompList.size(); index++) {

						if (splitCompList.contains(">=")) {

							int indexOp = splitCompList.indexOf(">=");
							subListLHS = splitCompList.subList(0, indexOp);
							subListRHS = splitCompList.subList(indexOp, splitCompList.size());

						} else if (splitCompList.contains(">")) {
							int indexOp = splitCompList.indexOf(">");
							subListLHS = splitCompList.subList(0, indexOp);
							subListRHS = splitCompList.subList(indexOp, splitCompList.size());

						} else {
							int indexOp = splitCompList.indexOf("<");
							subListLHS = splitCompList.subList(0, indexOp);
							subListRHS = splitCompList.subList(indexOp, splitCompList.size());
						}
					}

					String parLHSstr = subListLHS.get(1);
					String parLHSPosStr = null;

					List<String> elistposLHS = new ArrayList<>();
					List<String> elisttmethLHS = new ArrayList<>();
					List<String> resListLHS = new ArrayList<>();

					for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraPosentry.toString().split("="));
						if (pe.get(0).equals(parLHSstr)) {
							if (posInWordsMap.containsKey(pe.get(1))) {
								parLHSPosStr = posInWordsMap.get(pe.get(1));
								Collections.replaceAll(pe, pe.get(1), parLHSPosStr);
							}
							elistposLHS.add(pe.get(1));
						}
					}

					for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraMethentry.toString().split("="));
						if (pe.get(0).equals(parLHSstr)) {
							elisttmethLHS.add(pe.get(1));
						}
					}

					for (int i = 0; i < elistposLHS.size(); i++) {
						resListLHS.add(elistposLHS.get(i) + "|" + elisttmethLHS.get(i));
					}

					String parRHSstrOne = subListRHS.get(1);
					String parRHSstrTwo = null;
					String parRHSstrOnePos = null;
					String parRHSstrTwoPos = null;

					if (!subListRHS.get(subListRHS.size() - 3).equals("0")) {
						parRHSstrTwo = subListRHS.get(subListRHS.size() - 3);
					} else {

					}

					List<String> elistposRHSOne = new ArrayList<>();
					List<String> elisttmethRHSOne = new ArrayList<>();
					List<String> resListRHSOne = new ArrayList<>();

					for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraPosentry.toString().split("="));
						if (pe.get(0).equals(parRHSstrOne)) {
							if (posInWordsMap.containsKey(pe.get(1))) {
								parRHSstrOnePos = posInWordsMap.get(pe.get(1));
								Collections.replaceAll(pe, pe.get(1), parRHSstrOnePos);
							}
							elistposRHSOne.add(pe.get(1));
						}
					}

					for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraMethentry.toString().split("="));
						if (pe.get(0).equals(parRHSstrOne)) {
							elisttmethRHSOne.add(pe.get(1));
						}
					}

					for (int i = 0; i < elistposRHSOne.size(); i++) {
						resListRHSOne.add(elistposRHSOne.get(i) + "|" + elisttmethRHSOne.get(i));
					}

					List<String> elistposRHSTwo = new ArrayList<>();
					List<String> elisttmethRHSTWo = new ArrayList<>();
					List<String> resListRHSTwo = new ArrayList<>();

					for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraPosentry.toString().split("="));
						if (pe.get(0).equals(parRHSstrTwo)) {
							if (posInWordsMap.containsKey(pe.get(1))) {
								parRHSstrTwoPos = posInWordsMap.get(pe.get(1));
								Collections.replaceAll(pe, pe.get(1), parRHSstrTwoPos);
							}
							elistposRHSTwo.add(pe.get(1));
						}
					}

					for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraMethentry.toString().split("="));
						if (pe.get(0).equals(parRHSstrTwo)) {
							elisttmethRHSTWo.add(pe.get(1));
						}
					}

					for (int i = 0; i < elistposRHSTwo.size(); i++) {
						resListRHSTwo.add(elistposRHSTwo.get(i) + "|" + elisttmethRHSTWo.get(i));
					}

					// template replacement
					symbolStr = subListRHS.get(0);

					if (resListRHSTwo.size() > 0) {

						for (int i = 0; i < resListLHS.size(); i++) {

							List<String> newLHSList = Arrays.asList(resListLHS.get(i).split("\\|"));
							String varposLHS = newLHSList.get(0);
							String methLHS = newLHSList.get(1);
							List<String> msplit = Arrays.asList(methLHS.split("\\("));

							for (int j = 0; j < resListRHSOne.size(); j++) {

								List<String> newRHSListOne = Arrays.asList(resListRHSOne.get(j).split("\\|"));
								List<String> newRHSListTwo = Arrays.asList(resListRHSTwo.get(j).split("\\|"));

								String varposRHSOne = newRHSListOne.get(0);
								String methRHSOne = newRHSListOne.get(1);

								String varposRHSTwo = newRHSListTwo.get(0);
								String methRHSTwo = newRHSListTwo.get(1);

								if (symbolStr.equals(">=")) {

									if (msplit.get(0).contains(classnamecheck)) {

										char[] strOne = getTemplateCompCons1();
										Map<String, String> valuesMap = new HashMap<String, String>();
										valuesMap.put("positionLHS", varposLHS);
										valuesMap.put("LHSMethod", methLHS);
										valuesMap.put("positionRHSOne", varposRHSOne);
										valuesMap.put("RHSOnemethodName", methRHSOne);
										valuesMap.put("positionRHSTwo", varposRHSTwo);
										valuesMap.put("RHSTwomethodName", methRHSTwo);
										StringSubstitutor sub = new StringSubstitutor(valuesMap);
										String resolvedString = sub.replace(strOne);
										composedComparsionConstraint.add(resolvedString);
										//out.println(resolvedString);

									} else {

										char[] strOne = getTemplateCompOne();
										Map<String, String> valuesMap = new HashMap<String, String>();
										valuesMap.put("positionLHS", varposLHS);
										valuesMap.put("LHSMethod", methLHS);
										valuesMap.put("positionRHSOne", varposRHSOne);
										valuesMap.put("RHSOnemethodName", methRHSOne);
										valuesMap.put("positionRHSTwo", varposRHSTwo);
										valuesMap.put("RHSTwomethodName", methRHSTwo);
										StringSubstitutor sub = new StringSubstitutor(valuesMap);
										String resolvedString = sub.replace(strOne);
										composedComparsionConstraint.add(resolvedString);

										//out.println(resolvedString);

									}

								} else if (symbolStr.equals("<")) {

									char[] strTwo = getTemplateComptwo();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("positionLHS", varposLHS);
									valuesMap.put("LHSMethod", methLHS);
									valuesMap.put("positionRHSOne", varposRHSOne);
									valuesMap.put("RHSOnemethodName", methRHSOne);
									valuesMap.put("positionRHSTwo", varposRHSTwo);
									valuesMap.put("RHSTwomethodName", methRHSTwo);
									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(strTwo);
									composedComparsionConstraint.add(resolvedString);

									//out.println(resolvedString);
								}
							}
						}
					}
					// rhstwo list == null

					else {

						for (int i = 0; i < resListLHS.size(); i++) {

							List<String> newLHSList = Arrays.asList(resListLHS.get(i).split("\\|"));
							String varposLHS = newLHSList.get(0);
							String methLHS = newLHSList.get(1);

							for (int j = 0; j < resListRHSOne.size(); j++) {

								List<String> newRHSList = Arrays.asList(resListRHSOne.get(j).split("\\|"));
								String varposRHS = newRHSList.get(0);
								String methRHS = newRHSList.get(1);

								if (symbolStr.equals("<")) {

									char[] strThree = getTemplateCompThree();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("positionLHS", varposLHS);
									valuesMap.put("LHSMethod", methLHS);
									valuesMap.put("positionRHSOne", varposRHS);
									valuesMap.put("RHSOnemethodName", methRHS);
									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(strThree);
									composedComparsionConstraint.add(resolvedString);

									//out.println(resolvedString);

								}




								else if (symbolStr.equals(">")) {

									char[] strThree = getTemplateCompSeven();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("positionLHS", varposLHS);
									valuesMap.put("LHSMethod", methLHS);
									valuesMap.put("positionRHSOne", varposRHS);
									valuesMap.put("RHSOnemethodName", methRHS);
									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(strThree);
									composedComparsionConstraint.add(resolvedString);

									//out.println(resolvedString);
								}
							}
						}
					}
				}

				// length ends
				else {

					/* remaining sublist, check second par - numeric or alpha */
					List<String> splitCompListTwo = Arrays.asList(compStr.replaceFirst("^0+(?!$)", "").split(" "));

					for (String splitCompTwoStr : splitCompListTwo) {

						for (String methodStr : methods) {

							String result = StringUtils.substringBetween(methodStr, "(", ")");
							List<String> resList = Arrays.asList(result.split(",")); // params

							for (String r : resList) {

								if (r.equals(splitCompTwoStr)) {

									String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
									List<String> strList = Arrays.asList(mStr.split(" "));
									String posStr = String.valueOf(strList.indexOf(splitCompTwoStr));

									paraPosMap.put(splitCompTwoStr, posStr);

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

									paraMethNameMap.put(splitCompTwoStr, m);
									break;
								}
							}
						}
					}

					for (int index = 0; index < splitCompListTwo.size(); index++) {

						if (splitCompListTwo.contains(">")) {

							int indexOp = splitCompListTwo.indexOf(">");
							subListLHS = splitCompListTwo.subList(0, indexOp);
							subListRHS = splitCompListTwo.subList(indexOp, splitCompListTwo.size());

						} else if (splitCompListTwo.contains("<")) {

							int indexOp = splitCompListTwo.indexOf("<");
							subListLHS = splitCompListTwo.subList(0, indexOp);
							subListRHS = splitCompListTwo.subList(indexOp, splitCompListTwo.size());
						} else if (splitCompListTwo.contains(">=")) {

							int indexOp = splitCompListTwo.indexOf(">=");
							subListLHS = splitCompListTwo.subList(0, indexOp);
							subListRHS = splitCompListTwo.subList(indexOp, splitCompListTwo.size());
						} else {
							int indexOp = splitCompListTwo.indexOf("!=");
							subListLHS = splitCompListTwo.subList(0, indexOp);
							subListRHS = splitCompListTwo.subList(indexOp, splitCompListTwo.size());
						}
					}

					String paramLhsStr = subListLHS.get(0);
					String paramLhsPosStr = null;

					List<String> elistposLhs = new ArrayList<>();
					List<String> elisttmethLhs = new ArrayList<>();
					List<String> resListLhs = new ArrayList<>();

					for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraPosentry.toString().split("="));
						if (pe.get(0).equals(paramLhsStr)) {
							if (posInWordsMap.containsKey(pe.get(1))) {
								paramLhsPosStr = posInWordsMap.get(pe.get(1));
								Collections.replaceAll(pe, pe.get(1), paramLhsPosStr);
							}
							elistposLhs.add(pe.get(1));
						}
					}

					for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraMethentry.toString().split("="));
						if (pe.get(0).equals(paramLhsStr)) {
							elisttmethLhs.add(pe.get(1));
						}
					}

					for (int i = 0; i < elistposLhs.size(); i++) {
						resListLhs.add(elistposLhs.get(i) + "|" + elisttmethLhs.get(i));
					}

					String paramRhsNumStr = null;
					String paramRhsAphaStr = null;
					String paramRhsAphaPosStr = null;

					// checks for numbers
					if (subListRHS.get(1).matches(".*[0-9].*")) {
						paramRhsNumStr = subListRHS.get(1);
					} else {
						paramRhsAphaStr = subListRHS.get(1);
					}

					List<String> elistposRhsAlpha = new ArrayList<>();
					List<String> elisttmethRhsAlpha = new ArrayList<>();
					List<String> resListRhsAlpha = new ArrayList<>();

					for (Entry<String, String> paraPosentry : paraPosMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraPosentry.toString().split("="));
						if (pe.get(0).equals(paramRhsAphaStr)) {
							if (posInWordsMap.containsKey(pe.get(1))) {
								paramRhsAphaPosStr = posInWordsMap.get(pe.get(1));
								Collections.replaceAll(pe, pe.get(1), paramRhsAphaPosStr);
							}
							elistposRhsAlpha.add(pe.get(1));
						}
					}

					for (Entry<String, String> paraMethentry : paraMethNameMap.entries()) {

						List<String> pe = new ArrayList<>();
						pe = Arrays.asList(paraMethentry.toString().split("="));
						if (pe.get(0).equals(paramRhsAphaStr)) {
							elisttmethRhsAlpha.add(pe.get(1));
						}
					}

					for (int i = 0; i < elistposRhsAlpha.size(); i++) {
						resListRhsAlpha.add(elistposRhsAlpha.get(i) + "|" + elisttmethRhsAlpha.get(i));
					}

					symbolStr = subListRHS.get(0);

					if (resListRhsAlpha.size() > 0) {

						for (int i = 0; i < resListLhs.size(); i++) {

							List<String> Lhslist = Arrays.asList(resListLhs.get(i).split("\\|"));
							String Lhspos = Lhslist.get(0);
							String Lhsmeth = Lhslist.get(1);

							for (int j = 0; j < resListRhsAlpha.size(); j++) {

								List<String> RhsAlpha = Arrays.asList(resListRhsAlpha.get(j).split("\\|"));
								String Rhsposalpha = RhsAlpha.get(0);
								String Rhsmethalpha = RhsAlpha.get(1);

								if (symbolStr.equals(">")) {

									char[] strFive = getTemplateCompFive();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("paramLhsPosWordStr", Lhspos);
									valuesMap.put("paramLhsMethStr", Lhsmeth);
									valuesMap.put("paramRhsAlphaPosWordStr", Rhsposalpha);
									valuesMap.put("paramRhsAphaMethStr", Rhsmethalpha);
									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(strFive);
									composedComparsionConstraint.add(resolvedString);

									//out.println(resolvedString);

								} else if (symbolStr.equals("<")) {

									char[] strSix = getTemplateCompSix();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("paramLhsPosWordStr", Lhspos);
									valuesMap.put("paramLhsMethStr", Lhsmeth);
									valuesMap.put("paramRhsAlphaPosWordStr", Rhsposalpha);
									valuesMap.put("paramRhsAphaMethStr", Rhsmethalpha);

									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(strSix);
									composedComparsionConstraint.add(resolvedString);

									//out.println(resolvedString);
								}
							}
						}
					} else {

						for (int i = 0; i < resListLhs.size(); i++) {

							List<String> Lhselement = Arrays.asList(resListLhs.get(i).split("\\|"));
							String posLhs = Lhselement.get(0);
							String methLhs = Lhselement.get(1);
							List<String> msplit = Arrays.asList(methLhs.split("\\("));

							if ((symbolStr.equals(">=") ||symbolStr.equals(">")) && paramRhsNumStr != null) {

								if (msplit.get(0).contains(classnamecheck)) {

									char[] strFour = getTemplateCompCons2();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("paramLhsPosWordStr", posLhs);
									valuesMap.put("paramLhsMethStr", methLhs);
									valuesMap.put("paramRhsNumStr", paramRhsNumStr);
									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(strFour);
									composedComparsionConstraint.add(resolvedString);

									//out.println(resolvedString);

								} else {

									char[] strFour = getTemplateCompFour();
									Map<String, String> valuesMap = new HashMap<String, String>();
									valuesMap.put("paramLhsPosWordStr", posLhs);
									valuesMap.put("paramLhsMethStr", methLhs);
									valuesMap.put("paramRhsNumStr", paramRhsNumStr);
									StringSubstitutor sub = new StringSubstitutor(valuesMap);
									String resolvedString = sub.replace(strFour);
									composedComparsionConstraint.add(resolvedString);

									//out.println(resolvedString);

								}
							}
						}
					}
				}
			}
		}

		//out.close();
		return composedComparsionConstraint;
	}


	private void collectLeafNodes(CrySLArithmeticConstraint rightArit, List<LeafNodeWithOperator> rightOperations) {
			if (rightArit.getLeft() instanceof CrySLObject && rightArit.getRight() instanceof CrySLObject) {
				// Both left and right are leaf nodes
				rightOperations.add(new LeafNodeWithOperator((CrySLObject) rightArit.getLeft(), rightArit.getOperator().toString()));
				rightOperations.add(new LeafNodeWithOperator((CrySLObject) rightArit.getRight(), rightArit.getOperator().toString()));
			} else {
				// Recursively explore left and right nodes, checking for leaf nodes
				if (rightArit.getLeft() instanceof ISLConstraint) {
					ISLConstraint left = (ISLConstraint) rightArit.getLeft();
					if (left instanceof CrySLArithmeticConstraint) {
						collectLeafNodes((CrySLArithmeticConstraint) left, rightOperations);
					} else {
						// Handle if left is a leaf node (CrySLObject or other leaf types)
						rightOperations.add(new LeafNodeWithOperator((CrySLObject) left, rightArit.getOperator().toString()));
					}
				}

				if (rightArit.getRight() instanceof ISLConstraint) {
					ISLConstraint right = (ISLConstraint) rightArit.getRight();
					if (right instanceof CrySLArithmeticConstraint) {
						collectLeafNodes((CrySLArithmeticConstraint) right, rightOperations);
					} else {
						// Handle if right is a leaf node (CrySLObject or other leaf types)
						rightOperations.add(new LeafNodeWithOperator((CrySLObject) right, rightArit.getOperator().toString()));
					}
				}
			}
	}

	public class LeafNodeWithOperator {
		private CrySLObject leafNode;
		private String operator;

		public LeafNodeWithOperator(CrySLObject leafNode, String operator) {
			this.leafNode = leafNode;
			this.operator = operator;
		}

		public CrySLObject getLeafNode() {
			return leafNode;
		}

		public String getOperator() {
			return operator;
		}
	}

	public class ArithmeticNode {
		private String operator;
		private CrySLObject leftLeafNode;
		private CrySLObject rightLeafNode;
		private ArithmeticNode left;
		private ArithmeticNode right;

		public ArithmeticNode(String operator, CrySLObject leftLeafNode, CrySLObject rightLeafNode) {
			this.operator = operator;
			this.leftLeafNode = leftLeafNode;
			this.rightLeafNode = rightLeafNode;
		}

		public void setLeft(ArithmeticNode left) {
			this.left = left;
		}

		public void setRight(ArithmeticNode right) {
			this.right = right;
		}

		public String buildEquation() {
			StringBuilder equationBuilder = new StringBuilder();

			if (left != null) {
				String leftEquation = left.buildEquation().trim();
				if (!leftEquation.isEmpty()) {
					equationBuilder.append(leftEquation).append(" ");
				}
			} else if (leftLeafNode != null) {
				equationBuilder.append(leftLeafNode.getVarName()).append(" ");
			}

			equationBuilder.append(mapToOperator(operator)).append(" ");

			if (right != null) {
				String rightEquation = right.buildEquation().trim();
				if (!rightEquation.isEmpty()) {
					equationBuilder.append(rightEquation).append(" ");
				}
			} else if (rightLeafNode != null) {
				equationBuilder.append(rightLeafNode.getVarName()).append(" ");
			}

			return equationBuilder.toString().trim();
		}

	}

		public ArithmeticNode buildTree(CrySLArithmeticConstraint rightArit) {
			if (rightArit.getLeft() instanceof CrySLObject && rightArit.getRight() instanceof CrySLObject) {
				// Both left and right are leaf nodes
				return new ArithmeticNode(rightArit.getOperator().toString(),
						(CrySLObject) rightArit.getLeft(), (CrySLObject) rightArit.getRight());
			} else {
				// Recursively explore left and right nodes, checking for leaf nodes
				ArithmeticNode node = new ArithmeticNode(rightArit.getOperator().toString(), null, null);

				if (rightArit.getLeft() instanceof ISLConstraint) {
					ISLConstraint left = (ISLConstraint) rightArit.getLeft();
					if (left instanceof CrySLArithmeticConstraint) {
						node.setLeft(buildTree((CrySLArithmeticConstraint) left));
					} else {
						// Handle if left is a leaf node (CrySLObject or other leaf types)
						node.setLeft(new ArithmeticNode("", (CrySLObject) left, null));
					}
				}

				if (rightArit.getRight() instanceof ISLConstraint) {
					ISLConstraint right = (ISLConstraint) rightArit.getRight();
					if (right instanceof CrySLArithmeticConstraint) {
						node.setRight(buildTree((CrySLArithmeticConstraint) right));
					} else {
						// Handle if right is a leaf node (CrySLObject or other leaf types)
						node.setRight(new ArithmeticNode("", null, (CrySLObject) right));
					}
				}

				return node;
			}
		}

}