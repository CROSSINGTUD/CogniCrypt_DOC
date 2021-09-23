/*This class contains the implementation for the ORDER clause.
 * The implementation here parses the CrySL rule file directly and maps the order labels with their respective method names in the EVENT clause
 * */
package de.upb.docgen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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

import de.upb.docgen.utils.TreeNode;
import de.upb.docgen.utils.Utils;
import org.apache.commons.text.StringSubstitutor;

import crypto.rules.CrySLRule;
import org.xmlet.htmlapifaster.Li;
import org.xmlet.htmlapifaster.S;

import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.substringsBetween;

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
	static ArrayList<String> symbols = new ArrayList<>(Arrays.asList("+","*","?","|"));
	List<String> ans = new ArrayList<String>();
	StringBuilder forStringTest = new StringBuilder();



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
		File file = new File(DocSettings.getInstance().getLangTemplatesPath()+"\\"+"OrderClause");
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
		ans = new ArrayList<>();
		forStringTest = new StringBuilder();
		ArrayList<String> arrayList = new ArrayList<>();
		String cname = new String(rule.getClassName().replace(".", ","));
		List<String> strArray = Arrays.asList(cname.split(","));
		List<File> fileNames = getCryslFiles(FOLDER_PATH);

		// for (File file : fileNames)

		String classnamecheck = strArray.get((strArray.size()) - 1);

		String path = "./Output/" + classnamecheck + "_doc.txt";
		out = new PrintWriter(new FileWriter(path, true));

		Map<String, List<String>> fileContent = readCryslFile(file.toString());
		List<String> objectList = fileContent.get("OBJECTS");

		for (String pair : objectList) {
			String[] entry = pair.split(" ");
			objectMap.put(entry[1], entry[0]);
		}

		List<Event> eventList = processEvents(fileContent.get("EVENTS"));
		List<String> originalOrder = Arrays
				.asList(fileContent.get("ORDER").get(0).replaceAll("\\(", "\\( ").split(","));
		List<String> fl = new ArrayList<String>();
		List<String> n = new ArrayList<String>();
		getSymValues();
/*
		if (rule.getClassName().equals("java.security.DigestInputStream")) {
			List<String> test = parseOrderBetter(originalOrder);
			TreeNode<String> tree = orderToTree(test);
			List<String> list = preorder(tree, false);
			//String sentence = parseTreeToText(tree);

		}
*/

		if (!rule.getClassName().equals("javax.net.ssl.SSLParameters")) {
			List<String> test = parseOrderBetter(originalOrder);
			TreeNode<String> tree = orderToTree(test);
			List<String> list = preorder(tree, false);
			//String sentence = parseTreeToText(tree);

		}
		String myTestString = forStringTest.toString();

		for (String orderStr : originalOrder) {

			String[] orderArr = orderStr.split("[\\s,]+|(?<![\\s,])(?![a-zA-Z0-9\\s,])");

			if (orderArr.length > 1 && orderArr[0].isEmpty()) {
				orderArr = Arrays.copyOfRange(orderArr, 1, orderArr.length);
			}

			int delete;
			String control = "n";
			for (int q = 0; q < orderArr.length; q++) {
				int flag = 0;
				// control = "n";
				for (Map.Entry<String, String> entry : symbolMap.entrySet()) {

					if (entry.getKey().equals(orderArr[q])) { // symbol keys
						String symbolSearchstr = orderArr[q].replace(orderArr[q], entry.getValue()); // symbol values

						if (fl.size() - 1 < 0) {
						} else {
							if (control.equals("y")) {
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
					fl.add(orderArr[q]);
					fl.add("must be called exactly once.");
				}
				flag = 0;
			}
		}

		for (String ff : fl) {
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

		List<String> fo = new ArrayList<>();
		String a = "";
		if (n.size() > 2) {
			if (n.size() % 2 == 0) {
				for (int i = 0; i <= n.size() - 2; i += 2) {
					a = n.get(i) + " " + n.get(i + 1);
					fo.add(a);
				}
			} else {
				for (int i = 0; i <= n.size() - 2; i += 2) {
					a = n.get(i) + " " + n.get(i + 1);
					fo.add(a);
				}
				fo.add(n.get(n.size() - 1));
			}
		} else {
			a = n.get(0) + " " + n.get(1);
			fo.add(a);
		}



		String strTemp = getTemplateOrder();
		List<String> lines = Arrays.asList(strTemp.split("\\r?\\n"));
		String d = lines.get(1);
		String c = lines.get(0);
		String b = lines.get(3);
		String finalresult = "";
		finalresult = c + "\n" + "\n";

		for (String ftr : fo) {
			Map<String, String> valuesMap = new HashMap<String, String>();
			valuesMap.put("methodName+Cardinality", ftr);
			//arrayList.add(ftr);
			StringSubstitutor sub = new StringSubstitutor(valuesMap);
			String resolvedString = sub.replace(d);
			arrayList.add(resolvedString);
			finalresult += resolvedString + "\n";
		}
		finalresult += "\n" + b + "\n";

		//out.println(finalresult);


		// }
		out.close();
		objectMap.clear();
		processedresultMap.clear();
		symbolMap.clear();

		List<String> myTestList = new ArrayList<>();
		myTestList.add(myTestString);

		return myTestList;
	}

	private String parseTreeToText(TreeNode<String> root) {
		StringBuilder stringBuilder = new StringBuilder();
		if(root == null) return stringBuilder.toString();
		Queue<TreeNode> queue = new LinkedList<>();
		queue.offer(root);
		while (!queue.isEmpty()) {
			int len = queue.size();
			for (int i = 0; i < len ; i++) {
				TreeNode<String> node = queue.poll();
				System.out.print(node.getData() + " ");
				for (TreeNode item : node.getChildren()) {
					queue.offer(item);
				}
			}
			System.out.println();
		}

		return stringBuilder.toString();
	}



	public List<String> preorder(TreeNode<String> root, boolean inBlock) {
		if (root == null) return ans;
		ans.add(root.getData());
		//start
		if (root.getData().equals("-1")) {
			System.out.println("The order of this class:");
			forStringTest.append("The order of this class:\n");
		}
		//leaf
		if ((symbols.contains(root.getData())|| root.getData().equals("1")) && root.getChildren().size() == 1) {
			if (!inBlock) {
			for (TreeNode<String> leaf : root.getChildren()) {
				printSymbol(root.getData(),leaf.getData());
				//System.out.println(root.getData() + " " + leaf.getData());
				//forStringTest.append(root.getData() + " " + leaf.getData()+"\n");
				}
			} else {
				for (TreeNode<String> leaf : root.getChildren()) {
					forStringTest.append("\t");
					printSymbol(root.getData(),leaf.getData());
					//System.out.println("\t"+root.getData() + " " + leaf.getData());
					//forStringTest.append("\t"+root.getData() + " " + leaf.getData() + "\n");
				}
			}
		}
		//aggr
		if ((symbols.contains(root.getData()) && root.getChildren().size() > 1)) {
			//System.out.println("The following block has to be called..." + root.getData());
			forStringTest.append("The following block");
			printSymbol(root.getData(),"");
			for (TreeNode<String> child : root.getChildren())
				preorder(child, true);
			System.out.println("Block end");
			forStringTest.append("Block end\n");
			return ans;
		} else {
			for (TreeNode<String> child : root.getChildren())
				preorder(child, false);
			return ans;
		}
	}

	private void printSymbol(String symbol, String data) {
		switch (symbol) {
			case "*":
				forStringTest.append(data + " can be called as often as desired\n");
				break;
			case "+":
				forStringTest.append(data + " has to be called atleast once \n");
				break;
			case "1":
				forStringTest.append(data + " has to be called exactly once \n");
				break;
			case "?":
				forStringTest.append(data + " can not be called but no more than once \n");
				break;
			case "|":
				forStringTest.append(data + " or  \n");
				break;

		}
	}


	private TreeNode<String> orderToTree(List<String> test) {
		TreeNode<String> root = new TreeNode<>("-1");
		for (String aggr : test) {
			stringToTree(root, aggr);

		}
		return root;
	}

	private void stringToTree(TreeNode<String> root, String aggr) {
		if (!aggr.contains("(")) {
			String symbol = aggr.substring(aggr.length() - 1);
			if (symbols.contains(symbol)) {
				TreeNode temp = new TreeNode(symbol);
				root.addChild(temp);
				temp.addChild(aggr.substring(0,aggr.length()-1));
			} else {
				TreeNode temp = new TreeNode("1");
				root.addChild(temp);
				temp.addChild(aggr);

			}
		} else {
			String breakMeUp = aggr;
			int bracketcounter = (int) breakMeUp.chars().filter(ch -> ch == '(').count();
			if (!(bracketcounter > 1)) {
				String symbol = aggr.substring(aggr.length() - 1);
				if (!breakMeUp.contains("|")) {
					if (symbols.contains(symbol)) {
						TreeNode temp = new TreeNode(symbol);
						root.addChild(temp);
						String between = aggr.substring(aggr.indexOf("(")+1, aggr.lastIndexOf(")"));
						String[] parts = between.trim().split(" ");
						for (String part : parts) {
							symbol = part.substring(part.length() - 1);
							if (symbols.contains(symbol)) {
								TreeNode temp2 = new TreeNode(symbol);
								temp.addChild(temp2);
								temp2.addChild(part.substring(0,part.length()-1));
							} else {
								TreeNode temp2 = new TreeNode("1");
								temp.addChild(temp2);
								temp2.addChild(part);

							}

						}
					} else {
						String[] parts = substringBetween(breakMeUp, "(", ")").trim().split(" ");
						for (String part : parts) {
							symbol = part.substring(part.length() - 1);
							if (symbols.contains(symbol)) {
								TreeNode temp = new TreeNode(symbol);
								root.addChild(temp);
								temp.addChild(part);
							} else {
								TreeNode temp = new TreeNode("1");
								root.addChild(temp);
								temp.addChild(part);

							}

						}
					}
				} else {
					if (symbols.contains(symbol)) {
						TreeNode temp = new TreeNode(symbol);
						root.addChild(temp);
						String between = aggr.substring(aggr.indexOf("(")+1, aggr.lastIndexOf(")"));
						stringToTree(temp, between);
						return;
					} else {
						//TreeNode temp = new TreeNode("1");
						//root.addChild(temp);
						String[] parts = aggr.split("\\|");
						TreeNode temp = new TreeNode("|");
						root.addChild(temp);
						for (String part: parts) {

							stringToTree(temp, part);
						}
						return;
						//stringToTree(temp, breakMeUp);

					}

				}
			} else {
				if (breakMeUp.contains("|")) {
					//addChilds
					String symbol = aggr.substring(aggr.length() - 1);
					if (symbols.contains(symbol)) {
						TreeNode temp = new TreeNode(symbol);
						root.addChild(temp);
						String between = aggr.substring(aggr.indexOf("(")+1, aggr.lastIndexOf(")"));
						stringToTree(temp, between);
						return;
					} else {
						//TreeNode temp = new TreeNode("1");
						//root.addChild(temp);
						String[] parts = aggr.split("\\|");
						TreeNode temp = new TreeNode("|");
						root.addChild(temp);
						for (String part: parts) {

							stringToTree(temp, part);
						}
						return;
						//stringToTree(temp, breakMeUp);

					}

				} else {
					String[] parts = substringsBetween(breakMeUp, "(", ")");
				}

				//String[] parts = substringsBetween(breakMeUp, "(", ")");
				System.out.println("pa");

			}

		}
	}

	private List<String> parseOrderBetter(List<String> fo) {
		StringBuilder sb = new StringBuilder();
		List<String> betterOrder = new ArrayList<>();
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
					sb.append(fo.get(j));
				}
				betterOrder.add(sb.toString());
				i = j;


			} else {
				betterOrder.add(fo.get(i));
			}
		}
		return betterOrder;
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
