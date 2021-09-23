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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLRule;

/**
 * @author Ritika Singh
 */

public class ConstraintsVc {

	PrintWriter out;

	private static char[] getTemplateVC() throws IOException {
		char[] buff = Utils.getTemplatesText("ConstraintsVcClause");
		/*
		File file = new File(".\\src\\main\\resources\\Templates\\ConstraintsVcClause");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		char[] buff = new char[500];
		for (int charsRead; (charsRead = reader.read(buff)) != -1;) {
			stringBuffer.append(buff, 0, charsRead);
		}
		reader.close();

		 */
		return buff;
	}

	private static char[] getTemplateVCCon() throws IOException {
		char[] buff = Utils.getTemplatesText("ConstraintsVcClauseCon");
		/*
		File file = new File(".\\src\\main\\resources\\Templates\\ConstraintsVcClauseCon");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		char[] buff = new char[500];
		for (int charsRead; (charsRead = reader.read(buff)) != -1;) {
			stringBuffer.append(buff, 0, charsRead);
		}
		reader.close();

		 */
		return buff;
	}

	public ArrayList<String> getConstraintsVc(CrySLRule rule) throws IOException {

		ArrayList<String> composedConstraints = new ArrayList<>();
		Map<String, String> constraintVCMap = new LinkedHashMap<>();
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

		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		String paraConVCMapValStr = null;
		String paraPosInWordValStr = null;

		List<ISLConstraint> constraintVCList = rule.getConstraints().stream()
				.filter(e -> e.getClass().getSimpleName().toString().contains("CrySLValueConstraint"))
				.collect(Collectors.toList());

		if (constraintVCList.size() > 0) {

			List<String> firstConVCList = new ArrayList<>();

			Multimap<String, String> paraMethNameMMap = ArrayListMultimap.create();
			Multimap<String, String> paraPosMMap = ArrayListMultimap.create();

			for (ISLConstraint vclist : constraintVCList) {

				String vclistStr = vclist.toString();
				String sc = ":";
				String d = " - ";
				String substringBetween = StringUtils.substringBetween(vclistStr, sc, d);
				firstConVCList.add(substringBetween);

			}
			constraintVCMap = constraintVCList.stream()
					.map(s -> s.toString().replaceAll("VC:", "").replaceAll(",$", "").split(" - "))
					.collect(Collectors.toMap(a -> a[0], a -> a[1]));

			for (String firstConVCStr : firstConVCList) {

				for (String methodStr : methodsList) {

					if (methodStr.contains(firstConVCStr)) {

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

							String mStr = methodStr.replaceAll("[()]", " ").replaceAll(",", " ");
							List<String> strList = Arrays.asList(mStr.split(" "));
							String posStr = String.valueOf(strList.indexOf(firstConVCStr));
							paraMethNameMMap.put(firstConVCStr, m);
							paraPosMMap.put(firstConVCStr, posStr);
						}
					}
				}
			}

			List<String> resList = new ArrayList<>();

			for (String firstConVCStr : firstConVCList) {

				List<String> elist = new ArrayList<>();
				List<String> elistt = new ArrayList<>();

				for (Map.Entry<String, String> paraPosentry : paraPosMMap.entries()) {

					List<String> pe = new ArrayList<>();
					pe = Arrays.asList(paraPosentry.toString().split("="));
					if (pe.get(0).equals(firstConVCStr)) {
						if (posInWordsMap.containsKey(pe.get(1))) {
							paraPosInWordValStr = posInWordsMap.get(pe.get(1));
							Collections.replaceAll(pe, pe.get(1), paraPosInWordValStr);
						}
						elist.add(pe.get(1));
					}
				}

				for (Map.Entry<String, String> paraMethentry : paraMethNameMMap.entries()) {

					List<String> pe = new ArrayList<>();
					pe = Arrays.asList(paraMethentry.toString().split("="));
					if (pe.get(0).equals(firstConVCStr)) {
						elistt.add(pe.get(1));
					}
				}

				if (constraintVCMap.containsKey(firstConVCStr)) {
					paraConVCMapValStr = constraintVCMap.get(firstConVCStr);
				}

				for (int i = 0; i < elist.size(); i++) {
					resList.add(elist.get(i) + "|" + elistt.get(i) + "|" + paraConVCMapValStr);
				}
			}

			for (String fl : resList) {

				String l1 = fl;
				List<String> ls = Arrays.asList(l1.split("\\|"));
				String paraPos = ls.get(0);
				String mname = ls.get(1);
				String var = ls.get(2);

				List<String> msplit = Arrays.asList(ls.get(1).split("\\("));

				if (msplit.get(0).contains(classnamecheck)) {

					char[] str = getTemplateVCCon();
					Map<String, String> valuesMap = new HashMap<String, String>();
					valuesMap.put("position", paraPos);
					valuesMap.put("methodname", mname);
					valuesMap.put("var2", var);
					StringSubstitutor sub = new StringSubstitutor(valuesMap);
					String resolvedString = sub.replace(str);
					composedConstraints.add(resolvedString);
					out.println(resolvedString);

				} else {

					char[] str = getTemplateVC();
					Map<String, String> valuesMap = new HashMap<String, String>();
					valuesMap.put("position", paraPos);
					valuesMap.put("methodname", mname);
					valuesMap.put("var2", var);
					StringSubstitutor sub = new StringSubstitutor(valuesMap);
					String resolvedString = sub.replace(str);
					composedConstraints.add(resolvedString);
					out.println(resolvedString);
				}
			}
		}
		out.close();
		return composedConstraints;
	}
}
