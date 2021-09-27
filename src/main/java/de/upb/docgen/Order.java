/*This class contains the implementation for the ORDER clause.
 * The implementation here parses the CrySL rule file directly and maps the order labels with their respective method names in the EVENT clause
 * */
package de.upb.docgen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import crypto.rules.CrySLRule;

import static org.apache.commons.lang3.StringUtils.substringBetween;

/**
 * @author Ritika Singh
 */

public class Order {

	private static final String FOLDER_PATH = ".\\src\\main\\resources\\CrySLRules";
	private static final List<String> clauseNames = Arrays.asList("EVENTS", "ORDER", "OBJECTS", "FORBIDDEN");
	static Map<String, String> processedresultMap = new LinkedHashMap<>();
	static Map<String, String> symbolMap = new LinkedHashMap<>();
	static Map<String, String> objectMap = new LinkedHashMap<>();
	public static PrintWriter out;
	static ArrayList<String> symbols = new ArrayList<>(Arrays.asList("+", "*", "?", "|"));
	List<String> ans = new ArrayList<String>();
	StringBuilder forStringTest = new StringBuilder();
	int identLevel = 0;
	String[] orderArr;
	ArrayList<String> orderList;
	Iterator<String> iter;
	int bracketCounter;


	// retrieve a list of the crysl rule files in the Cryslrules folder
	private static List<File> getCryslFiles(String folderPath) throws IOException {
		List<File> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(folderPath))) {
			for (Path path : directoryStream) {
				fileNames.add(path.toAbsolutePath().toFile());
			}
		} catch (IOException ex) {
			System.out.println("Error reading files");
			ex.printStackTrace();
		}
		return fileNames;
	}

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
			//File fileone = new File(".\\src\\main\\resources\\symbol.properties");
			//todo: change this
			File fileone = new File("C:\\Uni\\BA\\cognidoc\\src\\main\\resources\\symbol.properties");
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

	private static String getTemplateOrder() throws IOException {
		//String strD = Utils.getTemplatesTextString("OrderClause");

		//File file = new File(".\\src\\main\\resources\\Templates\\OrderClause");
		File file = new File(DocSettings.getInstance().getLangTemplatesPath() + "\\" + "OrderClause");
		BufferedReader br = new BufferedReader(new FileReader(file));
		String strLine = br.readLine();
		String strD = "";

		while (strLine != null) {
			strD += strLine + "\n";
			strLine = br.readLine();
		}
		br.close();


		return strD;
	}

	// function to map labels with their corresponding method names
	private static List<Event> processEvents(List<String> lines) {
		//todo: under construction
		List<Event> eventList = new ArrayList<>();
		Map<String, String> methodIdentifiersmap = new LinkedHashMap<>();
		Map<String, List<String>> labelIdentifiersmap = new LinkedHashMap<>();

		for (String line : lines) {
			if (!line.contains(":")) {
				throw new RuntimeException("Unexpected line found: " + line);
			}
			if (line.contains(":=")) {
				String[] temp1 = line.split(":=");
				String labelName = temp1[0].trim();
				String replacelabelName = null;
				String[] keyval = temp1[1].split("\\|");

				for (int i = 0; i < keyval.length; i++) {
					keyval[i] = temp1[0].trim() + ":" + keyval[i].trim();
				}

				List<String> labelNameList = Arrays.asList(temp1[1].split("\\|")).stream().map(x -> x.trim())
						.collect(Collectors.toList());

				List<String> result = new ArrayList<>();

				for (String labelList : labelNameList) {
					if (labelIdentifiersmap.containsKey(labelList)) {
						List<String> resultnew = (labelIdentifiersmap.get(labelList));
						result.addAll(resultnew);
						replacelabelName = labelName;
					} else {
						result.add(labelList);
					}
				}
				if (labelName.equalsIgnoreCase(replacelabelName)) {
					labelIdentifiersmap.put(labelName, result);
				} else {
					labelIdentifiersmap.put(labelName, labelNameList); // Gets - g1
				}
			} else {
				String[] temp1 = line.split(":");
				String methodLabel = temp1[0].trim();
				String methodName = temp1[1].trim();

				if (methodName.contains("=")) {
					String[] methodname = methodName.split("=");
					methodname[1].trim();
					methodName = methodname[1].trim();
				}
				methodIdentifiersmap.put(methodLabel, methodName);// maps g1 to getInstance()...
			}
		}

		labelIdentifiersmap.forEach((key, idList) -> {
			Event event = new Event(key);
			for (String id : idList) {
				String method = methodIdentifiersmap.get(id);
				List<String> methodLabelList = new ArrayList<>();
				methodLabelList.add(method);

				for (String methodLabelStr : methodLabelList) {
					List<String> extractParamList = new ArrayList<>();
					int startIndex = methodLabelStr.indexOf("(");
					int endIndex = methodLabelStr.indexOf(")");
					String bracketExtractStr = methodLabelStr.substring(startIndex + 1, endIndex);

					if (bracketExtractStr.contains(",")) {
						String[] elements = bracketExtractStr.split(",");
						for (int a = 0; a < elements.length; a++) {
							extractParamList.add(elements[a].replace(" ", ""));
						}
					} else {
						extractParamList.add(bracketExtractStr);
					}

					for (int y = 0; y < extractParamList.size(); y++) {

						int startInd = 0;
						int endInd = 0;
						String dataTypevalue = "";

						if (y > 0) {

							if (!objectMap.containsKey(extractParamList.get(y))) {
							} else {

								dataTypevalue = objectMap.get(extractParamList.get(y)).toString();

								Pattern word = Pattern.compile(extractParamList.get(y));
								Matcher match = word.matcher(methodLabelStr);

								while (match.find()) {
									startInd = match.start();
									endInd = match.end() - 1;
									if (startInd > startIndex) {
										if (methodLabelStr.charAt(startInd - 1) == ' ') {
											break;
										}

									}
								}
							}
						} else {

							if (!objectMap.containsKey(extractParamList.get(y))) {
							} else {

								dataTypevalue = objectMap.get(extractParamList.get(y)).toString();

								Pattern word = Pattern.compile(extractParamList.get(y));
								Matcher match = word.matcher(methodLabelStr);

								while (match.find()) {
									startInd = match.start();
									endInd = match.end() - 1;
									if (startInd > startIndex) {
										break;
									}
								}
							}
						}

						String strDiv = methodLabelStr.substring(startInd, endInd + 1);
						if (strDiv.equals(extractParamList.get(y))) {
							StringBuilder sDB = new StringBuilder(methodLabelStr);
							sDB.replace(startInd, endInd + 1, dataTypevalue);
							methodLabelStr = sDB.toString();
						}

						event.addIdentifierAndMethod(id, methodLabelStr);
					}
				}
			}
			eventList.add(event);

		});

		{
			methodIdentifiersmap.forEach((key, methodList) -> {
				Event event = new Event(key);
				String method = methodIdentifiersmap.get(key);
				List<String> methodLabelList = new ArrayList<>();
				methodLabelList.add(method);

				for (String methodLabelStr : methodLabelList) {
					List<String> extractParamList = new ArrayList<>();
					int startIndex = methodLabelStr.indexOf("(");
					int endIndex = methodLabelStr.indexOf(")");
					String bracketExtractStr = methodLabelStr.substring(startIndex + 1, endIndex);

					if (bracketExtractStr.contains(",")) {
						String[] elements = bracketExtractStr.split(",");
						for (int a = 0; a < elements.length; a++) {
							extractParamList.add(elements[a].replace(" ", ""));
						}
					} else {
						extractParamList.add(bracketExtractStr);
					}

					for (int y = 0; y < extractParamList.size(); y++) {

						int startInd = 0;
						int endInd = 0;
						String dataTypevalue = "";

						if (y > 0) {

							if (!objectMap.containsKey(extractParamList.get(y))) {
							} else {

								dataTypevalue = objectMap.get(extractParamList.get(y)).toString();

								Pattern word = Pattern.compile(extractParamList.get(y));
								Matcher match = word.matcher(methodLabelStr);

								while (match.find()) {
									startInd = match.start();
									endInd = match.end() - 1;
									if (startInd > startIndex) {
										if (methodLabelStr.charAt(startInd - 1) == ' ') {
											break;
										}
									}
								}
							}

						} else {

							if (!objectMap.containsKey(extractParamList.get(y))) {
							} else {

								dataTypevalue = objectMap.get(extractParamList.get(y)).toString();

								Pattern word = Pattern.compile(extractParamList.get(y));
								Matcher match = word.matcher(methodLabelStr);

								while (match.find()) {
									startInd = match.start();
									endInd = match.end() - 1;
									if (startInd > startIndex) {
										break;
									}
								}
							}

						}

						String strDiv = methodLabelStr.substring(startInd, endInd + 1);
						if (strDiv.equals(extractParamList.get(y))) {
							StringBuilder sDB = new StringBuilder(methodLabelStr);
							sDB.replace(startInd, endInd + 1, dataTypevalue);
							methodLabelStr = sDB.toString();
						}

						event.addIdentifierAndMethod(key, methodLabelStr);
					}
				}
				eventList.add(event);
			});
		}

		getProcessedMap(eventList);
		return eventList;
	}

	private static void getProcessedMap(List<Event> eventList) {
		eventList.forEach(event -> {
			processedresultMap.put(event.getEvent(), event.getMethodIdentifierMap());
		});
	}

	public List<String> runOrder(CrySLRule rule, File file) throws IOException {
		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		List<File> fileNames = getCryslFiles(FOLDER_PATH);

		// for (File file : fileNames)

		String classnamecheck = strArray.get((strArray.size()) - 1);
		/*

		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		 */

		Map<String, List<String>> fileContent = readCryslFile(file.toString());
		List<String> objectList = fileContent.get("OBJECTS");

		for (String pair : objectList) {
			String[] entry = pair.split(" ");
			objectMap.put(entry[1], entry[0]);
		}

		List<Event> eventList = processEvents(fileContent.get("EVENTS"));
		List<String> originalOrder = Arrays
				.asList(fileContent.get("ORDER").get(0).replaceAll("\\(", "\\( ").split(","));
		getSymValues();
		List<String> orderSplittedWithBrackets = connectBrackets(originalOrder);

		/* old order parsing
		for (String orderStr : orderSplittedWithBrackets) {
			identLevel = 0;
			bracketCounter = 0;
			orderArr = orderStr.split("[\\s,]+|(?<![\\s,])(?![a-zA-Z0-9\\s,])");

			if (orderArr.length > 1 && orderArr[0].isEmpty()) {
				orderArr = Arrays.copyOfRange(orderArr, 1, orderArr.length);
			}
			int orderArrlength = orderArr.length;
			int delete;
			String control = "n";
			orderList = new ArrayList<>(Arrays.asList(orderArr));
			int number = 0;
			
			for (int i = 0; i < orderList.size(); i++) {
				String s = orderList.get(i);
				int flag = 0;
				// control = "n";
				for (Map.Entry<String, String> entry : symbolMap.entrySet()) {

					if (entry.getKey().equals(s)) { // symbol keys
						if (s.equals(")")) identLevel--;
						String symbolSearchstr = s.replace(s, entry.getValue()); // symbol values
						if (symbolSearchstr.equals("The next block ")) {
							symbolSearchstr += decideSymbolOfBracket(orderStr, number);
							number++;
							bracketCounter--;
							control = "n";
						}

						if (fl.size() - 1 < 0) {
						} else {
							if (symbolSearchstr.equals("]")) {
								fl.add(symbolSearchstr);
								continue;
							}

							if (control.equals("y") && number == 0) {
								control = "n";
								delete = fl.size() - 1;
								fl.remove(delete);
							}
						}

						fl.add(symbolSearchstr);
						flag++;
					}
				}
				if (flag == 0) {
					control = "y";
					fl.add(s);
					fl.add("must be called exactly once.");
				}
				flag = 0;
			}
		}
		
		 */

		ArrayList<String> allNLsentences = parseOrderToNL(orderSplittedWithBrackets);

		List<String> resolvedSentences = aggrgatesToMethods(allNLsentences);

		List<String> orderConstructed = combineAndIndentation(resolvedSentences);

		/*
		String strTemp = getTemplateOrder();
		List<String> lines = Arrays.asList(strTemp.split("\\r?\\n"));
		String d = lines.get(1);
		String c = lines.get(0);
		String b = lines.get(3);
		String finalresult = "";
		finalresult = c + "\n" + "\n";

		for (String ftr : orderConstructed) {
			Map<String, String> valuesMap = new HashMap<String, String>();
			valuesMap.put("methodName+Cardinality", ftr);
			//arrayList.add(ftr);
			StringSubstitutor sub = new StringSubstitutor(valuesMap);
			String resolvedString = sub.replace(d);
			arrayList.add(resolvedString);
			finalresult += resolvedString + "\n";
		}
		finalresult += "\n" + b + "\n";

		 */

		//out.println(finalresult);


		// }
		//out.close();
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
				if (n.get(i).startsWith("The next") ) {
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i));
					identlevel++;
					n.remove(i);
					i -= 2;
				} else if  (n.get(i).startsWith("or") && (n.get(i+1).startsWith("The next"))){
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i));
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i+1));
					identlevel++;
					n.remove(i);
					n.remove(i);
					i -= 2;
				} else if  (n.get(i).startsWith("or")){
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i));
					fo.add(StringUtils.repeat("\t", identlevel) + n.get(i+1) + n.get(i+2));
					n.remove(i);
					n.remove(i);
					n.remove(i);
					i -=2;
				} else if (n.get(i).startsWith("]")) {
					//removing closing brackets and lowering the le
					while (n.get(i).startsWith("]")) {
						identlevel--;
						n.remove(i);
					}
					//remove already processes sentence
					if (!n.get(i).startsWith("or")) {
						n.remove(i);
					}
					i -= 2;
				} else if (n.get(i+1).startsWith("or")) {
					a = StringUtils.repeat("\t", identlevel) + n.get(i);
					fo.add(a);
					n.remove(i);
					i -= 2;
				} else {
 					a = StringUtils.repeat("\t", identlevel) + n.get(i) + " " + n.get(i + 1);
					fo.add(a);
				}
			}} else{

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
						if (symbolSearchStr.startsWith("The next")) {
							symbolSearchStr += decideSymbolOfBracket(orderstr, number);
							number++;
						}
						fl.add(symbolSearchStr);
						added = true;
						break;
					}
				}
				if (added) continue;
				fl.add(s);
				String next = "";
				int orderListSize = orderList.size();
				if (i+1 < orderListSize) {
					next = orderList.get(i+1);
				}
				if (!symbolMap.containsKey(next)) {
					fl.add("has to be called once.");

				}
				if (next.equals(")") || next.equals("(") || next.equals("|") ) {
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
			if (decided.charAt(i)=='(') {
				if (toIgnore > 0) {
					toIgnore--;
					continue;
				}
				totalCounter++;
				if (totalCounter > 0) breakof = true;
			}
			if (decided.charAt(i)==')') {
				if (totalCounter-1 <0) continue;
				totalCounter--;
			}
			if (totalCounter == 0 && breakof) {
				//check if i+1 is empty
				if (i+1 == decided.length()) {
					decided = "has to be called once.";
				} else {
					switch (decided.charAt(i+1)) {
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
				for (;j < fo.size();j++){
					bracketcounter += fo.get(j).chars().filter(ch -> ch == '(').count();
				}
				for (j=i; j < fo.size(); ++j) {
					if (bracketcounter == 0) {

						break;
					}
					bracketcounter -= fo.get(j).chars().filter(ch -> ch == ')').count();
					sb.append(fo.get(j) +" ");
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
