/*This class contains the implementation for the ORDER clause.
 * The implementation here parses the CrySL rule file directly and maps the order labels with their respective method names in the EVENT clause
 * */
package de.upb.docgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.file.Files;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import crypto.rules.CrySLRule;

/**
 * @author Ritika Singh
 * @author Sven Feldmann
 */

public class Order {

	private static final List<String> clauseNames = Arrays.asList("EVENTS", "ORDER", "OBJECTS", "FORBIDDEN");
	static Map<String, String> processedresultMap = new LinkedHashMap<>();
	static Map<String, String> symbolMap = new LinkedHashMap<>();
	static Map<String, String> objectMap = new LinkedHashMap<>();
	public static PrintWriter out;

	// reading the file and adding it to map(k,v), k- event , v- content inside it
	private static Map<String, List<String>> readCryslFile(String filePath) throws IOException {
		Map<String, List<String>> cryslFileContentMap = new LinkedHashMap<>();
		String contentCategory = null;

		List<String> fileContent = Files.lines(Paths.get(filePath)).filter(x -> x != null && x.trim().length() > 0)
				.map(x -> x.trim()).map(x -> {
					if (x.endsWith(";")) {
						x = x.substring(0, x.length() - 1);
					}
					return x;
				}).collect(Collectors.toList());

		for (String line : fileContent) {

			if (clauseNames.contains(line)) {
				contentCategory = line;
				cryslFileContentMap.put(contentCategory, new ArrayList<>());
				continue;
			}
			if (contentCategory == null || contentCategory.trim().length() == 0) {
				continue;
			}
			cryslFileContentMap.get(contentCategory).add(line);
		}
		return cryslFileContentMap;// contains the sections with their details
	}

	private static Map<String, String> getSymValues() throws IOException {
		Properties properties = new Properties();

		try {
			File fileone = new File(DocSettings.getInstance().getLangTemplatesPath() + "/symbol.properties");
			FileInputStream fileInput = new FileInputStream(fileone);
			properties.load(fileInput);
			fileInput.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		symbolMap.putAll(properties.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString())));
		return symbolMap;
	}

	public List<String> runOrder(CrySLRule rule, File file) throws IOException {
		Map<String, List<String>> fileContent = readCryslFile(file.toString());
		List<String> objectList = fileContent.get("OBJECTS");

		for (String pair : objectList) {
			String[] entry = pair.split(" ");
			objectMap.put(entry[1], entry[0]);
		}

		List<String> originalOrder = Arrays
				.asList(fileContent.get("ORDER").get(0).replaceAll("\\(", "\\( ").split(","));
		getSymValues();
		List<String> orderSplittedWithBrackets = connectBrackets(originalOrder);

		ArrayList<String> allNLsentences = parseOrderToNL(orderSplittedWithBrackets);

		List<String> resolvedSentences = aggrgatesToMethods(allNLsentences);

		List<String> orderConstructed = combineAndIndentation(resolvedSentences);

		objectMap.clear();
		processedresultMap.clear();
		symbolMap.clear();

		return orderConstructed;
	}

	private List<String> aggrgatesToMethods(ArrayList<String> allNLsentences) {
		List<String> n = new ArrayList<>();
		for (String ff : allNLsentences) {
			int flag = 0;
			for (Map.Entry<String, String> en : processedresultMap.entrySet()) {
				if (ff.equals(en.getKey())) {
					String ddd = ff.replace(ff, en.getValue()); // method names
					n.add(ddd);
					flag++;
				}
			}
			if (flag == 0) {
				n.add(ff);
			}
			flag = 0;
		}
		return n;
	}

	private List<String> combineAndIndentation(List<String> n) {
		List<String> fo = new ArrayList<>();
		String a = "";
		int identlevel = 0;
		if (n.size() > 2) {
			for (int i = 0; i <= n.size() - 2; i += 2) {
				if (n.get(i).startsWith(symbolMap.get("("))) {
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i));
					identlevel++;
					n.remove(i);
					i -= 2;
				} else if (n.get(i).startsWith(symbolMap.get("|")) && (n.get(i + 1).startsWith(symbolMap.get("(")))) {
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i));
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i + 1));
					identlevel++;
					n.remove(i);
					n.remove(i);
					i -= 2;
				} else if (n.get(i).startsWith(symbolMap.get("|"))) {
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i));
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i + 1) + n.get(i + 2));
					n.remove(i);
					n.remove(i);
					n.remove(i);
					i -= 2;
				} else if (n.get(i).startsWith(symbolMap.get(")"))) {
					// removing closing brackets and lowering the level
					while (n.size() > i && n.get(i).startsWith(symbolMap.get(")"))) {
						identlevel--;
						n.remove(i);
					}
					// remove already processed sentences
					if (n.size() > i && !n.get(i).startsWith(symbolMap.get("|"))) {
						n.remove(i);
					}
					i -= 2;
				} else if (n.get(i + 1).startsWith(symbolMap.get("|"))) {
					a = StringUtils.repeat("\t", identlevel) + n.get(i);
					fo.add(a);
					n.remove(i);
					i -= 2;
				} else {
					a = StringUtils.repeat("\t", identlevel) + n.get(i) + " " + n.get(i + 1);
					fo.add(a);
				}
			}
		} else {

			a = StringUtils.repeat("\t", identlevel) + n.get(0) + " " + n.get(1);
			fo.add(a);
		}
		return fo;
	}

	private ArrayList<String> parseOrderToNL(List<String> test) {
		ArrayList<String> fl = new ArrayList<>();
		boolean added = false;
		for (String orderstr : test) {
			String[] orderArray = orderstr.split("[\\s,]+|(?<![\\s,])(?![a-zA-Z0-9\\s,])");
			if (orderArray.length > 1 && orderArray[0].isEmpty()) {
				orderArray = Arrays.copyOfRange(orderArray, 1, orderArray.length);
			}
			ArrayList<String> orderList = new ArrayList<>(Arrays.asList(orderArray));
			int number = 0;
			for (int i = 0; i < orderList.size(); i++) {
				added = false;
				String s = orderList.get(i);
				for (Map.Entry<String, String> entry : symbolMap.entrySet()) {
					if (entry.getKey().equals(s)) {
						String symbolSearchStr = s.replace(s, entry.getValue());
						if (symbolSearchStr.startsWith(symbolMap.get("("))) {
							symbolSearchStr += decideSymbolOfBracket(orderstr, number);
							number++;
						}
						fl.add(symbolSearchStr);
						added = true;
						break;
					}
				}
				if (added)
					continue;
				fl.add(s);
				String next = "";
				int orderListSize = orderList.size();
				if (i + 1 < orderListSize) {
					next = orderList.get(i + 1);
				}
				if (!symbolMap.containsKey(next)) {
					fl.add("has to be called once.");

				}
				if (next.equals(")") || next.equals("(") || next.equals("|")) {
					fl.add("has to be called once.");

				}
			}
		}
		return fl;
	}

	private String decideSymbolOfBracket(String decided, int toIgnore) {
		int totalCounter = 0;
		boolean breakof = false;
		for (int i = 0; i < decided.length(); i++) {
			if (decided.charAt(i) == '(') {
				if (toIgnore > 0) {
					toIgnore--;
					continue;
				}
				totalCounter++;
				if (totalCounter > 0)
					breakof = true;
			}
			if (decided.charAt(i) == ')') {
				if (totalCounter - 1 < 0)
					continue;
				totalCounter--;
			}
			if (totalCounter == 0 && breakof) {
				// check if i+1 is empty
				if (i + 1 == decided.length()) {
					decided = "has to be called once.";
				} else {
					switch (decided.charAt(i + 1)) {
						case '*':
							decided = "can be called arbitary times.";
							break;
						case '+':
							decided = "has to be called atleast once.";
							break;
						case '?':
							decided = "is optional to call.";
							break;
						default:
							decided = "has to be called once.";

					}
					break;
				}
			}

		}
		return decided;
	}

	private List<String> connectBrackets(List<String> fo) {
		StringBuilder sb = new StringBuilder();
		List<String> connected = new ArrayList<>();
		for (int i = 0; i < fo.size(); i++) {
			if (fo.get(i).contains("(")) {

				int bracketcounter = 0;
				int j = i;
				for (; j < fo.size(); j++) {
					bracketcounter += fo.get(j).chars().filter(ch -> ch == '(').count();
				}
				for (j = i; j < fo.size(); ++j) {
					if (bracketcounter == 0) {

						break;
					}
					bracketcounter -= fo.get(j).chars().filter(ch -> ch == ')').count();
					sb.append(fo.get(j) + " ");
				}
				connected.add(sb.toString());
				i = j;

			} else {
				connected.add(fo.get(i));
			}
		}
		return connected;
	}
}

class Event {
	public String event;
	public Map<String, String> methodIdentifierMap = new HashMap<>();

	public Event(String event) {
		this.event = event;
	}

	public void addIdentifierAndMethod(String id, String method) {
		methodIdentifierMap.put(id, method);
	}

	public String getEvent() {
		return event;
	}

	public String getMethodIdentifierMap() {
		return methodIdentifierMap.values().toString().replaceAll(",(?=[^\\)]*(?:\\(|$))", " or")
				.replaceFirst("[\\[\\]]", "").replaceFirst("\\]$", "");
	}

	@Override
	public String toString() {
		return "Event{" + "event='" + event + '\'' + ", methodIdentifierMap=" + methodIdentifierMap + '}';
	}
}
