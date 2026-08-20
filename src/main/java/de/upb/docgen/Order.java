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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import crypto.exceptions.CryptoAnalysisException;
import de.upb.docgen.crysl.CrySLReader;
import org.apache.commons.lang3.StringUtils;

import crypto.rules.CrySLRule;

/**
 * @author Ritika Singh
 * @author Sven Feldmann
 */

public class Order {

	// Every CrySL section header, not just the ones this class consumes: an omitted
	// header made the parser append that section's lines into the previous bucket.
	private static final List<String> clauseNames = Arrays.asList("SPEC", "EVENTS", "ORDER", "OBJECTS",
			"FORBIDDEN", "CONSTRAINTS", "REQUIRES", "ENSURES", "NEGATES");
	static Map<String, String> processedresultMap = new LinkedHashMap<>();
	static Map<String, String> symbolMap = new LinkedHashMap<>();
	static Map<String, String> objectMap = new LinkedHashMap<>();
	public static PrintWriter out;

	// reading the file and adding it to map(k,v), k- event , v- content inside it
	/**
	 * Read a CrySL file into a map of clause name -> lines.
	 */
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

	/**
	 * Load symbol mappings from Templates/symbol.properties (disk override or JAR fallback).
	 */
	private static Map<String, String> getSymValues() throws IOException {
		Properties properties = new Properties();

		File symbolPropsFile;

		// IMPORTANT: Do NOT couple this decision to --rulesDir.
		// The decision must depend ONLY on --langTemplatesPath.
		String langTemplatesPath = DocSettings.getInstance().getLangTemplatesPath();
		if (langTemplatesPath != null && !langTemplatesPath.trim().isEmpty()) {
			symbolPropsFile = new File(langTemplatesPath, "symbol.properties");
			if (!symbolPropsFile.isFile()) {
				throw new FileNotFoundException(
						"symbol.properties not found at --langTemplatesPath: " + symbolPropsFile.getAbsolutePath()
								+ " (either fix the path or omit --langTemplatesPath to use bundled defaults)"
				);
			}
		} else {
			// bundled fallback (works even if user passed --rulesDir but omitted --langTemplatesPath)
			symbolPropsFile = CrySLReader.readSymbolPropertiesFromJar();
			if (symbolPropsFile == null || !symbolPropsFile.isFile()) {
				throw new FileNotFoundException(
						"Bundled Templates/symbol.properties not found in resources/JAR (expected Templates/symbol.properties)"
				);
			}
		}

		try (FileInputStream fileInput = new FileInputStream(symbolPropsFile)) {
			properties.load(fileInput);
		}

		symbolMap.putAll(properties.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString())));
		return symbolMap;
	}



	/**
	 * Parse EVENT definitions into a list of Event objects and cache a label -> method map.
	 */
	private static List<Event> processEvents(List<String> lines) {
		List<Event> eventList = new ArrayList<>();
		Map<String, String> methodIdentifiersmap = new LinkedHashMap<>();
		Map<String, List<String>> labelIdentifiersmap = new LinkedHashMap<>();

		for (String line : lines) {
			if (line.contains("//")) continue;
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

		for (Map.Entry<String, List<String>> entry : labelIdentifiersmap.entrySet()) {
			String key = entry.getKey();
			List<String> idList = entry.getValue();
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

		}

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

	/**
	 * Cache a processed label -> method string for quick lookup.
	 */
	private static void getProcessedMap(List<Event> eventList) {
		eventList.forEach(event -> {
			processedresultMap.put(event.getEvent(), event.getMethodIdentifierMap());
		});
	}


	/**
	 * Build the natural-language ORDER description for a CrySL rule.
	 * Reads the rule file, resolves symbols, and renders order sentences.
	 */
	public List<String> runOrder(CrySLRule file) throws IOException, CryptoAnalysisException {

		if (file == null) {
			throw new IllegalArgumentException("CrySLRule cannot be null");
		}

		try {
			Map<String, List<String>> fileContent;

			String simpleName = file.getClassName().substring(file.getClassName().lastIndexOf(".") + 1);
			String rulesDir = DocSettings.getInstance().getRulesetPathDir();

			// if --rulesDir isn't provided, read the CrySL rule from bundled JAR resources
			if (rulesDir != null && !rulesDir.trim().isEmpty()) {
				String filePath = rulesDir + File.separator + simpleName + ".crysl";
				fileContent = readCryslFile(filePath);
			} else {
				File rule = CrySLReader.readRuleFromJarFile(simpleName);
				if (rule == null || !rule.isFile()) {
					throw new FileNotFoundException(
							"Bundled CrySL rule not found for: " + simpleName
									+ " (expected /CrySLRules/" + simpleName + ".crysl)"
					);
				}
				fileContent = readCryslFile(rule.getPath());
			}

			// OBJECTS can be missing in some rules — avoid NPE
			List<String> objectList = fileContent.get("OBJECTS");
			if (objectList != null) {
				for (String pair : objectList) {
					String[] entry = pair.split(" ");
					if (entry.length >= 2) {
						objectMap.put(entry[1], entry[0]);
					}
				}
			}

			List<String> events = fileContent.get("EVENTS");
			if (events == null) {
				throw new IOException("CrySL rule " + file.getClassName() + " is missing an EVENTS section");
			}
			processEvents(events);

			List<String> orderLines = fileContent.get("ORDER");
			if (orderLines == null || orderLines.isEmpty()) {
				throw new IOException("CrySL rule " + file.getClassName() + " is missing an ORDER section");
			}

			List<String> originalOrder =
					Arrays.asList(orderLines.get(0).replaceAll("\\(", "\\( ").split(","));

			getSymValues();

			List<String> orderSplittedWithBrackets = connectBrackets(originalOrder);
			ArrayList<String> allNLsentences = parseOrderToNL(orderSplittedWithBrackets);
			List<String> resolvedSentences = aggrgatesToMethods(allNLsentences);
			List<String> orderConstructed = combineAndIndentation(resolvedSentences);

			return orderConstructed;

		} finally {
			// clear in finally so exceptions don't leak state into next rule
			objectMap.clear();
			processedresultMap.clear();
			symbolMap.clear();
		}
	}

	/**
	 * Replace aggregate labels with the resolved method names.
	 */
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

	/**
	 * Combine sentences and apply indentation based on parentheses/alternatives.
	 */
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
					// An event is always a (name, frequency) pair, but guard the second
					// half anyway: a malformed ORDER clause (e.g. a trailing "|") leaves
					// the name unpaired, and n.get(i + 2) used to read past the end.
					StringBuilder alternative = new StringBuilder(n.get(i + 1));
					if (i + 2 < n.size()) {
						// Separator was missing here, rendering "getPrivate()has to be called once."
						alternative.append(" ").append(n.get(i + 2));
					}
					fo.add(StringUtils.repeat("\t", identlevel) + alternative);
					n.remove(i);
					n.remove(i);
					if (i < n.size()) {
						n.remove(i);
					}
					i -= 2;
				} else if (n.get(i).startsWith(symbolMap.get(")"))) {
					// removing closing brackets and lowering the level
					while (n.size() > i && n.get(i).startsWith(symbolMap.get(")"))) {
						identlevel--;
						n.remove(i);
					}
					// Drop the group's trailing frequency phrase - decideSymbolOfBracket has
					// already folded it into the opening "The next block ..." line, so it
					// would otherwise render twice. ONLY an actual frequency phrase may be
					// dropped: this used to delete whatever followed the closing bracket,
					// which silently swallowed the next event (or the next group's header)
					// whenever the group carried no *, + or ? suffix.
					if (n.size() > i && isBracketFrequencyPhrase(n.get(i))) {
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

	/**
	 * True if the token is the frequency phrase a bracket group's *, + or ? suffix
	 * produces (e.g. "can be called arbitary times."), as opposed to an event name or
	 * the start of another group. Used to decide what may safely be discarded after a
	 * closing bracket.
	 */
	private boolean isBracketFrequencyPhrase(String token) {
		if (token == null) {
			return false;
		}
		for (String suffix : new String[] { "*", "+", "?" }) {
			String phrase = symbolMap.get(suffix);
			if (phrase != null && !phrase.isEmpty() && token.startsWith(phrase)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Convert the ORDER clause to natural-language tokens using symbol mappings.
	 */
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

	/**
	 * Decide the call frequency text for a bracketed group (e.g., *, +, ?).
	 */
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

	/**
	 * Merge tokens inside matching parentheses into a single token string.
	 */
	private List<String> connectBrackets(List<String> fo) {
		List<String> connected = new ArrayList<>();

		for (int i = 0; i < fo.size(); i++) {
			if (fo.get(i).contains("(")) {

				StringBuilder sb = new StringBuilder(); // IMPORTANT: reset for each new bracket group

				// Track running depth token by token and stop as soon as it returns to
				// zero, scoping the count to THIS group only. The previous version summed
				// every "(" across the entire remaining token list before consuming, so
				// a second bracket group later in the same ORDER clause inflated the
				// counter and got silently merged into the first group's string.
				int bracketDepth = 0;
				int j = i;
				for (; j < fo.size(); j++) {
					String token = fo.get(j);
					bracketDepth += token.chars().filter(ch -> ch == '(').count();
					bracketDepth -= token.chars().filter(ch -> ch == ')').count();
					sb.append(token).append(" ");
					if (bracketDepth <= 0) {
						j++;
						break;
					}
				}
				connected.add(sb.toString());
				i = j - 1; // so that the outer for-loop's i++ moves to j (the next unprocessed token)

			} else {
				connected.add(fo.get(i));
			}
		}
		return connected;
	}
}

class Event {
	public String event;
	// LinkedHashMap (like every other map in this file) so an alias's "or"-joined
	// method list renders in declaration order rather than hash-bucket order.
	public Map<String, String> methodIdentifierMap = new LinkedHashMap<>();

	/**
	 * Create an event with the given label.
	 */
	public Event(String event) {
		this.event = event;
	}

	/**
	 * Add a label -> method mapping for this event.
	 */
	public void addIdentifierAndMethod(String id, String method) {
		methodIdentifierMap.put(id, method);
	}

	/**
	 * Return the event label.
	 */
	public String getEvent() {
		return event;
	}

	/**
	 * Return a display string for the event's method identifiers.
	 */
	public String getMethodIdentifierMap() {
		return methodIdentifierMap.values().toString().replaceAll(",(?=[^\\)]*(?:\\(|$))", " or")
				.replaceFirst("[\\[\\]]", "").replaceFirst("\\]$", "");
	}

	@Override
	public String toString() {
		return "Event{" + "event='" + event + '\'' + ", methodIdentifierMap=" + methodIdentifierMap + '}';
	}
}