/*this class contains the functions or getting the classname, number of methods and forbidden methods*/

package de.upb.docgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.text.StringSubstitutor;

import crypto.rules.CrySLForbiddenMethod;
import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import crypto.rules.StateMachineGraph;
import crypto.rules.TransitionEdge;
import de.upb.docgen.utils.Constant;
import de.upb.docgen.utils.Utils;


/**
 * @author Ritika Singh
 */

public class ClassEventForb {

	public static PrintWriter out;

	public String getClassName(CrySLRule rule) throws IOException {
/*
		File file = new File(".\\src\\main\\resources\\Templates\\ClassNameClause");

		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		char[] buff = new char[500];
		for (int charsRead; (charsRead = reader.read(buff)) != -1;) {
			stringBuffer.append(buff, 0, charsRead);
		}
		reader.close();
			
		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);
		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));


		char[] buff = Utils.getTemplatesText("ClassNameClause");

		String cName = rule.getClassName();

		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("ClassName", cName);

		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		String resolvedString = sub.replace(buff);
		//out.close();
		return cName;
		//out.println(resolvedString);
*/
		return rule.getClassName();
	}


	public String getLinkOnly(CrySLRule rule) throws IOException {
		return rule.getClassName().replaceAll("\\.","/");
	}

	public String getLink(CrySLRule rule) throws IOException {
/*
		File file = new File(".\\src\\main\\resources\\Templates\\LinkToJavaDoc");

		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		char[] buff = new char[500];
		for (int charsRead; (charsRead = reader.read(buff)) != -1;) {
			stringBuffer.append(buff, 0, charsRead);
		}
		reader.close();
*/
		char[] buff = Utils.getTemplatesText("LinkToJavaDoc");
		String link = rule.getClassName().replace(".", "/");
		String cName = rule.getClassName();
		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("ClassName", cName);
		valuesMap.put("ClassLink", link);

		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		return sub.replace(buff);
		//out.println(resolvedString);

	}

	public String getFullClassName(CrySLRule rule) throws IOException {


		char[] buff = Utils.getTemplatesText("ClassNameClause");
		/*
		File file = new File(".\\src\\main\\resources\\Templates\\ClassNameClause");

		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		char[] buff = new char[500];
		for (int charsRead; (charsRead = reader.read(buff)) != -1;) {
			stringBuffer.append(buff, 0, charsRead);
		}
		reader.close();

		 */

		String cname = rule.getClassName().replace(".", ",");
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);
		/*
		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		 */

		String cName = rule.getClassName();
		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("ClassName", cName);

		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		String resolvedString = sub.replace(buff);
		//out.close();
		return resolvedString;
		//out.println(resolvedString);

	}

	public String getEventNumbers(CrySLRule rule) throws IOException {

		char[] buff1 = Utils.getTemplatesText("EventNumClause");
		char[] buff2 = Utils.getTemplatesText("EventNumClause2");

		/*
		File file = new File(".\\src\\main\\resources\\Templates\\EventNumClause");

		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		char[] buff = new char[500];
		for (int charsRead; (charsRead = reader.read(buff)) != -1;) {
			stringBuffer.append(buff, 0, charsRead);
		}
		reader.close();
*/
		String cname = rule.getClassName().replace(".", ",");
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);

		//String path = "./Output/" + classnamecheck + "_doc.txt";
		//out = new PrintWriter(new FileWriter(path, true));

		ArrayList<CrySLMethod> methodNames = new ArrayList<CrySLMethod>();
		ArrayList<CrySLMethod> listWithoutDuplicates = new ArrayList<CrySLMethod>();
		List<TransitionEdge> graph = rule.getUsagePattern().getEdges();
		String methodNumber = null;
		for (int i = 0; i < graph.size(); i++) {
			for (int j = 0; j < graph.get(i).getLabel().size(); j++) {
				CrySLMethod methods = (graph.get(i).getLabel().get(j));
				methodNames.add(methods);
				listWithoutDuplicates = (ArrayList<CrySLMethod>) methodNames.stream().distinct()
						.collect(Collectors.toList());
				methodNumber = String.valueOf(listWithoutDuplicates.size());
			}
		}

		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("number", methodNumber);

		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		String resolvedString = "";
		if (methodNumber.equals("1")) resolvedString = sub.replace(buff1);
		else resolvedString = sub.replace(buff2);
		//out.close();
		//return methodNumber;
		//out.println(resolvedString);
		return resolvedString;

	}

	public List<String> getForb(CrySLRule rule) throws IOException {
		char[] buff1 = Utils.getTemplatesText("ForbiddenMethodClause");
		char[] buff2 = Utils.getTemplatesText("ForbiddenMethodClauseCon");
		char[] buff3 = Utils.getTemplatesText("ForbiddenMethodClauseAlt");
		char[] buff4 = Utils.getTemplatesText("ForbiddenMethodClauseConAlt");
		StringBuilder sb = new StringBuilder();
		ArrayList<String> composedForbs = new ArrayList<>();
		ArrayList<String> alternatives = new ArrayList<>();
		if (rule.getForbiddenMethods().size() > 0) {
			List<CrySLForbiddenMethod> forbMethods = rule.getForbiddenMethods();
			for (CrySLForbiddenMethod forMethod: forbMethods) {
				sb.setLength(0);
				sb.append(resolveMethod(forMethod.getMethod()));
				if (forMethod.getAlternatives().size() > 0) {
					for (CrySLMethod altMethod : forMethod.getAlternatives()) {
						alternatives.add(resolveMethod(altMethod));
					}
				}
				if (alternatives.size() > 0) {


					if (!checkForConstructor(forMethod.getMethod().getMethodName())) {
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("ForbMethodName", sb.toString());
						String resolvedAlternates = String.join(" or ", alternatives);
						valuesMap.put("Alternate", resolvedAlternates);
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(buff3);
						composedForbs.add(resolvedString);
					} else { //constructor
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("ForbMethodName", sb.toString());
						String resolvedAlternates = String.join(" or ", alternatives);
						valuesMap.put("Alternate", resolvedAlternates);
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(buff4);
						composedForbs.add(resolvedString);
					}
				} else {
					if (!checkForConstructor(forMethod.getMethod().getMethodName())) {
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("ForbMethodName", sb.toString());
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(buff1);
						composedForbs.add(resolvedString);
					} else {
						Map<String, String> valuesMap = new HashMap<String, String>();
						valuesMap.put("ForbMethodName", sb.toString());
						StringSubstitutor sub = new StringSubstitutor(valuesMap);
						String resolvedString = sub.replace(buff2);
						composedForbs.add(resolvedString);
					}
				}




			}


		}
		return composedForbs;
	}

	private boolean checkForConstructor(String fullname) {
		String shortname = fullname.substring(fullname.lastIndexOf('.') + 1);
		int index = fullname.indexOf(shortname);
		int otherIndex = fullname.lastIndexOf(shortname);
		return index != otherIndex;
	}

	private String resolveMethod(CrySLMethod forMethod) {
		StringBuilder sb = new StringBuilder();
		String withoutPackageName = forMethod.getShortMethodName();
		sb.append(withoutPackageName);
		sb.append("(");
		Iterator entryIterator = forMethod.getParameters().iterator();
		while(entryIterator.hasNext()) {
			Entry<String, String> par = (Entry)entryIterator.next();

			sb.append(par.getValue());
			if (entryIterator.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(")");

		return sb.toString();
	}

	public List<String> getForbiddenMethods(CrySLRule rule) throws IOException {
		ArrayList<String> arrayList = new ArrayList<>();
		char[] buff = Utils.getTemplatesText("ForbiddenMethodClause");
		char[] buffCon = Utils.getTemplatesText("ForbiddenMethodClauseCon");

		/*
		File file = new File(".\\src\\main\\resources\\Templates\\ForbiddenMethodClause");
		File fileCon = new File(".\\src\\main\\resources\\Templates\\ForbiddenMethodClauseCon");
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		char[] buff = new char[500];
		for (int charsRead; (charsRead = reader.read(buff)) != -1;) {
			stringBuffer.append(buff, 0, charsRead);
		}

		StringBuilder stringBufferCon = new StringBuilder();
		Reader readerCon = new InputStreamReader(new FileInputStream(fileCon), StandardCharsets.UTF_8);
		char[] buffCon = new char[500];
		for (int charsRead; (charsRead = readerCon.read(buffCon)) != -1;) {
			stringBufferCon.append(buffCon, 0, charsRead);
		}
		reader.close();
		readerCon.close();
*/
		String cname = rule.getClassName().replace(".", ",");
		List<String> strArray = Arrays.asList(cname.split(","));
		String classnamecheck = strArray.get((strArray.size()) - 1);
		/*
		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		 */

		List<CrySLForbiddenMethod> forbname = rule.getForbiddenMethods().stream().filter(e -> !e.getSilent())
				.collect(Collectors.toList());
		String joinedStr = "";

		if (!forbname.isEmpty()) {
			Map<String, String> forbMap = new LinkedHashMap<>();
			List<String> fList = new ArrayList<>();

			for (CrySLForbiddenMethod f : forbname) {
				CrySLMethod cm = f.getMethod();
				List<Entry<String, String>> forbPar = cm.getParameters();
				for (Entry<String, String> fP : forbPar) {
					forbMap.put(fP.getKey(), fP.getValue());
				}

				String[] fName = cm.toString().split("\\.");
				fList.add(fName[fName.length - 1].replaceAll("\\( ", "\\(").replaceAll(" ", ",").replaceAll(";", ""));

				for (String fStr : fList) {
					List<String> extractParamList = new ArrayList<>();
					List<String> nameList = new ArrayList<>();
					int startIndex = fStr.indexOf("(");
					int endIndex = fStr.indexOf(")");
					String bracketExtractStr = fStr.substring(startIndex + 1, endIndex);

					if (bracketExtractStr.contains(",")) {
						String[] elements = bracketExtractStr.split(",");
						for (int a = 0; a < elements.length; a++) {
							extractParamList.add(elements[a]);
						}
					} else {
						extractParamList.add(bracketExtractStr);
					}
					for (String extractParamStr : extractParamList) {
						if (!forbMap.containsKey(extractParamStr)) {
						} else {
							String value = forbMap.get(extractParamStr);
							fStr = fStr.replace(extractParamStr, value);
						}
					}
					nameList.add(fStr);
					joinedStr = String.join(",", nameList);
				}

				List<String> msplit = Arrays.asList(joinedStr.split("\\("));
				if (msplit.get(0).contains(classnamecheck)) {

					Map<String, String> valuesMap = new HashMap<String, String>();
					valuesMap.put("ForbMethodName", joinedStr);
					StringSubstitutor sub = new StringSubstitutor(valuesMap);
					String resolvedString = sub.replace(buffCon);
					arrayList.add(resolvedString);
					//out.println(resolvedString);
				} else {

					Map<String, String> valuesMap = new HashMap<String, String>();
					valuesMap.put("ForbMethodName", joinedStr);
					StringSubstitutor sub = new StringSubstitutor(valuesMap);
					String resolvedString = sub.replace(buff);
					arrayList.add(resolvedString);
					//out.println(resolvedString);

				}
			}
		}
		//out.close();
		return arrayList;
	}

}
