package de.upb.docgen.utils;

import crypto.interfaces.ICrySLPredicateParameter;
import crypto.rules.CrySLCondPredicate;
import crypto.rules.CrySLPredicate;
import crypto.rules.StateNode;
import crypto.rules.TransitionEdge;
import de.upb.docgen.ComposedRule;
import de.upb.docgen.DocSettings;
import de.upb.docgen.Order;
import de.upb.docgen.crysl.CrySLReader;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Sven Feldmann
 */

public class Utils {
	public static File getFileFromResources(String fileName) {
		URL resource = Utils.class.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("File could not be found!");
		} else {
			return new File(resource.getFile());
		}
	}

	public static String replaceLast(String string, String toReplace, String replacement) {
		int pos = string.lastIndexOf(toReplace);
		if (pos > -1) {
			return string.substring(0, pos)
					+ replacement
					+ string.substring(pos + toReplace.length());
		} else {
			return string;
		}
	}

	public static List<TransitionEdge> getOutgoingEdges(Collection<TransitionEdge> collection, final StateNode curNode, final StateNode notTo) {
		final List<TransitionEdge> outgoingEdges = new ArrayList<>();
		for (final TransitionEdge comp : collection) {
			if (comp.getLeft().equals(curNode) && !(comp.getRight().equals(curNode) || comp.getRight().equals(notTo))) {
				outgoingEdges.add(comp);
			}
		}
		return outgoingEdges;
	}

	/**
	 * This method extracts and maps the corresponding predicates by classname
	 * @param mappedPredicates
	 * @return Map(k: className, v: Set(Corresponding classnames)
	 */
	public static Map<String, Set<String>> toOnlyClassNames(Map<String, List<Map<String, List<String>>>> mappedPredicates) {
		Map<String, Set<String>> onlyClassNamesMap = new HashMap<>();
		for (String classname : mappedPredicates.keySet()) {
			Set<String> setToRemoveDuplicates = new HashSet<>();
			for (Map<String, List<String>> stringListMap : mappedPredicates.get(classname)) {
				List<String> temp = new ArrayList<>();
				for (String predicate : stringListMap.keySet()) {
					List<String> dependingClassNames = new ArrayList<>();
					for (String classnameToAdd : stringListMap.get(predicate)) {
						dependingClassNames.add(classnameToAdd);
					}
					temp.addAll(dependingClassNames);
				}
				setToRemoveDuplicates.addAll(temp);
			}
			onlyClassNamesMap.putIfAbsent(classname,setToRemoveDuplicates);
		}
		return onlyClassNamesMap;
	}

	public static Map<String, Set<String>> getConstraintPredicateAndVarnameMap(List<ComposedRule> composedRuleList, Map<String, List<CrySLPredicate>> mapEnsures) {
		Map<String, Set<String>> RuleMappedToEnsures = new HashMap<>();
		for (ComposedRule rule : composedRuleList) {
			List<String> requiredPredicates = rule.getConstrainedPredicates();
			Set<String> valuesToAdd = new HashSet<>();
			String temp ="";
			for (String rp : requiredPredicates) {
				String predicateName = rp.substring(rp.lastIndexOf(' ') + 1).trim();
				String predicated = predicateName.substring(0, predicateName.length()-1);
				temp = predicated;
				for (Map.Entry<String, List<CrySLPredicate>> entry : mapEnsures.entrySet()) {
					List<CrySLPredicate> rulePredicate = entry.getValue();
					for (CrySLPredicate singlePredicate : rulePredicate) {
						if (singlePredicate.getPredName().contains(predicated) && !Objects.equals(rule.getComposedClassName(), entry.getKey())) {
							valuesToAdd.add(entry.getKey()   + "-" + temp);
						}
					}
				}
			}
			RuleMappedToEnsures.put(rule.getComposedClassName(),valuesToAdd);
		}
		return RuleMappedToEnsures;
	}


	/**
	 * This methods constructs a Map, out of the given 2 CryslPredicate Maps.
	 * @param keyMap
	 * @param dependingMap
	 * @return Map(String, Map(List(String, List(String))))
	 */
	public static Map<String, List<Map<String, List<String>>>> mapPredicates(Map<String, List<CrySLPredicate>> dependingMap, Map<String, List<CrySLPredicate>> keyMap) {
		Map<String, List<Map<String,List<String>>>> dependingPredicatesMap = new HashMap<>();
		for (String className : keyMap.keySet()) {
			List<Map<String,List<String>>> predicateList = new ArrayList<>();
			for (CrySLPredicate predicate : keyMap.get(className)) {
				if (predicate.isNegated()) continue;
				String predicateName = predicate.getPredName();
				List<ICrySLPredicateParameter> predicateParameters = predicate.getParameters();
				Map<String,List<String>> keyToDependingMap = new HashMap<>();
				for (String dependingClassName: dependingMap.keySet() ) {
					Set<String> dependingClasses = new LinkedHashSet<>();

					for (CrySLPredicate dependingPredicates :  dependingMap.get(dependingClassName)) {
						if (dependingPredicates.getPredName().equals(predicateName) && !(dependingPredicates.isNegated() && !(dependingPredicates instanceof CrySLCondPredicate))) {
							if (dependingPredicates.getParameters().size() == predicateParameters.size()) {
								dependingClasses.add(dependingClassName);

							}
						}
					}

					if (dependingClasses.size() != 0 ) {
						if (keyToDependingMap.containsKey(predicateName)) {
							keyToDependingMap.get(predicateName).addAll(dependingClasses);
						} else {
							List<String> dc = new ArrayList<>(dependingClasses);
							keyToDependingMap.put(predicateName, dc);
						}
					}
				}
				predicateList.add(keyToDependingMap);
			}
			dependingPredicatesMap.put(className,predicateList);
		}
		return dependingPredicatesMap;
	}


	public static char[] getTemplatesText(String templateName) throws IOException {
		File file;
		String pathToLangTemplates;
		if (DocSettings.getInstance().getLangTemplatesPath() == null) {
			pathToLangTemplates = Order.class.getResource("/Templates").getPath();
			String folderName = pathToLangTemplates.substring(pathToLangTemplates.lastIndexOf("/") + 1);
			file = Utils.extract(folderName + "/" + templateName);
		} else {
			pathToLangTemplates = DocSettings.getInstance().getLangTemplatesPath();
			file = new File(pathToLangTemplates+ "/" + templateName);
		}
		StringBuilder stringBuffer = new StringBuilder();
		Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		char[] buff = new char[500];
		for (int charsRead; (charsRead = reader.read(buff)) != -1;) {
			stringBuffer.append(buff, 0, charsRead);
		}
		reader.close();
		return buff;

	}

	public static String getTemplatesTextString(String templateName) throws IOException {
		File file;
		String pathToLangTemplates;
		if (DocSettings.getInstance().getLangTemplatesPath() == null) {
			pathToLangTemplates = Order.class.getResource("/Templates").getPath();
			String folderName = pathToLangTemplates.substring(pathToLangTemplates.lastIndexOf("/") + 1);
			file = Utils.extract(folderName + "/" + templateName);
		} else {
			pathToLangTemplates = DocSettings.getInstance().getLangTemplatesPath();
			file = new File(pathToLangTemplates+ "/" + templateName);
		}
		BufferedReader br = new BufferedReader(new FileReader(file));
		String strLine = "";
		String strD = "";

		while ((strLine = br.readLine()) != null) {
			strD += strLine;
			strLine = br.readLine();
		}
		br.close();
		return strD + "\n";
	}

	public static String pathForTemplates(String path) {
		return path.replaceAll("\\\\","/");
	}



	public static File extract(String filePath) {
		try {
			File f = File.createTempFile(filePath, null);
			f.deleteOnExit();
			FileOutputStream resourceOS = new FileOutputStream(f);
			byte[] byteArray = new byte[1024];
			int i;
			InputStream classIS = Utils.class.getClassLoader().getResourceAsStream(filePath);
//While the input stream has bytes
			while ((i = classIS.read(byteArray)) > 0) {
//Write the bytes to the output stream
				resourceOS.write(byteArray, 0, i);
			}
//Close streams to prevent errors
			classIS.close();
			resourceOS.close();
			return f;
		} catch (Exception e) {
			System.out.println("Error during the file creation process from jar: " + e.getMessage());
			return null;
		}
	}



}
