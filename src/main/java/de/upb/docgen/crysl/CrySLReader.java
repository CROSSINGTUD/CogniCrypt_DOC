package de.upb.docgen.crysl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import crypto.cryslhandler.CrySLModelReader;
import crypto.exceptions.CryptoAnalysisException;
import crypto.rules.CrySLRule;
import crypto.rules.CrySLRuleReader;
import de.upb.docgen.DocSettings;
import de.upb.docgen.utils.Utils;

/**
 * @author Ritika Singh
 */

public class CrySLReader {

/*
if (docSettings.getRulesetPathDir() != null) {
			rules = ruleReader.readFromDirectory(new File(docSettings.getRulesetPathDir()));
			//Generate dot files from given ruleset
			if(docSettings.isBooleanE()) {
				System.out.println("Generating the statemachine files from " + docSettings.getRulesetPathDir() + ".");
				StateMachineToGraphviz.generateGraphvizStateMachines(docSettings.getRulesetPathDir(),docSettings.getReportDirectory());
			}
		} else {
			//read rules from jar resources
			rules = CrySLReader.readRulesFromJar();
			//generate dot files from jar resources
			if(docSettings.isBooleanE()) {
				System.out.println("Generating the statemachine files from default resources.");
				StateMachineToGraphviz.generateGraphvizStateMachines(docSettings.getReportDirectory());
			}
		}
 */


	public static List<CrySLRule> readRulesFromJar() throws IOException, CryptoAnalysisException {

		final String path = "CrySLRules";
		final File jarFile = new File(CrySLReader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		CrySLRuleReader cryslmodelreader = new CrySLRuleReader();

		List<CrySLRule> rules = new ArrayList<>();

		if(jarFile.isFile()) {  // Run with JAR file
			final JarFile jar = new JarFile(jarFile);
			final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
			try {
				while (entries.hasMoreElements()) {
					final JarEntry name = entries.nextElement();
					if (name.getName().endsWith(".crysl")) { //only handle crysl files
						File extractedRule = Utils.extract(name.getName());  //Create a temporary crysl file
						String shortend = extractedRule.getName().substring(0, extractedRule.getName().indexOf("crysl") + "crysl".length()); //CrySL Rule name without temp file ending
						Files.move(extractedRule.toPath(), Paths.get(shortend), StandardCopyOption.REPLACE_EXISTING);
						File renamedTempFile = (Paths.get(shortend).toFile());
						System.out.println(renamedTempFile.getPath());
						CrySLRule rule = cryslmodelreader.readFromSourceFile(renamedTempFile); //Renaming allows the file to be read by the CrySLModelReader
						renamedTempFile.deleteOnExit(); //Removes the temp CrySL file after jvm is finished
						rules.add(rule);

					}
				}
			} catch (CryptoAnalysisException ex) {
				
			}
			jar.close();


		} else { // Run with IDE
			final URL url = CrySLReader.class.getResource("/" + path);
			if (url != null) {
				try {
					final File apps = new File(url.toURI());
					for (File file : apps.listFiles()) {
						rules.add(cryslmodelreader.readFromSourceFile(file));

					}
				} catch (URISyntaxException | CryptoAnalysisException ex) {

				}
			}
		}
		return rules;


	}


	public static File readRuleFromJarFile(String ruleName) throws IOException, CryptoAnalysisException {

		final String path = "CrySLRules";
		final CodeSource codeSource = CrySLReader.class.getProtectionDomain().getCodeSource();
		final File jarFile = new File(codeSource.getLocation().getPath());
		CrySLRuleReader cryslModelReader = new CrySLRuleReader();

		if (jarFile.isFile()) {  // Run with JAR file
			try (JarFile jar = new JarFile(jarFile)) {
				final Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
				while (entries.hasMoreElements()) {
					final JarEntry entry = entries.nextElement();
					if (entry.getName().endsWith(".crysl") && entry.getName().contains(ruleName)) { // only handle crysl files
						File extractedRule = Utils.extract(entry.getName());  // Create a temporary crysl file
						String shortName = extractedRule.getName().substring(0, extractedRule.getName().indexOf("crysl") + "crysl".length()); // CrySL Rule name without temp file ending
						Files.move(extractedRule.toPath(), Paths.get(shortName), StandardCopyOption.REPLACE_EXISTING);
						File renamedTempFile = (Paths.get(shortName).toFile());
						CrySLRule rule = cryslModelReader.readFromSourceFile(renamedTempFile); // Renaming allows the file to be read by the CrySLModelReader
						renamedTempFile.deleteOnExit(); // Removes the temp CrySL file after JVM is finished
						return renamedTempFile;
					}
				}
			}
		} else { // Run with IDE
			final URL url = CrySLReader.class.getResource("/" + path);
			if (url != null) {
				try {
					final File apps = new File(url.toURI());
					for (File file : apps.listFiles()) {
						if (file.getName().contains(ruleName)) {
							return file;
						}
					}
				} catch (URISyntaxException ex) {
					// Handle exception
				}
			}
		}
		return null;
	}


	public static File readSymbolPropertiesFromJar() {
		final String path = "Templates" + "/" + "symbol.properties";
		final CodeSource codeSource = CrySLReader.class.getProtectionDomain().getCodeSource();
		final File jarFile = new File(codeSource.getLocation().getPath());

		if (jarFile.isFile()) {  // Run with JAR file
			try (JarFile jar = new JarFile(jarFile)) {
				final Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
				while (entries.hasMoreElements()) {
					final JarEntry entry = entries.nextElement();
					if (entry.getName().endsWith(".properties")) { // only handle crysl files
						File extractedRule = Utils.extract(entry.getName());  // Create a temporary crysl file
						String shortName = extractedRule.getName().substring(0, extractedRule.getName().indexOf("properties") + "properties".length()); // CrySL Rule name without temp file ending
						Files.move(extractedRule.toPath(), Paths.get(shortName), StandardCopyOption.REPLACE_EXISTING);
						File renamedTempFile = (Paths.get(shortName).toFile());
						//CrySLRule rule = cryslModelReader.readFromSourceFile(renamedTempFile); // Renaming allows the file to be read by the CrySLModelReader
						renamedTempFile.deleteOnExit(); // Removes the temp CrySL file after JVM is finished
						return renamedTempFile;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else { // Run with IDE
			final URL url = CrySLReader.class.getResource("/" + path);
			if (url != null) {
				try {
					final File apps = new File(url.toURI());
					return apps;
				} catch (URISyntaxException ex) {
					// Handle exception
				}
			}
		}
		return null;
	}



	public static File readFTLFromJar(String FTLname) throws IOException {

		final String path = "FTLTemplates" + "/" + FTLname;
		final File jarFile = new File(CrySLReader.class.getProtectionDomain().getCodeSource().getLocation().getPath());



		if(jarFile.isFile()) {  // Run with JAR file
			final JarFile jar = new JarFile(jarFile);
			final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
			while(entries.hasMoreElements()) {
				final JarEntry name = entries.nextElement();
				if (name.getName().contains(FTLname)) { //only handle crysl files
					File extractedRule = Utils.extract(name.getName());  //Create a temporary crysl file
					String shortend = extractedRule.getName().substring(0,extractedRule.getName().indexOf("ftl")+"ftl".length()); //CrySL Rule name without temp file ending
					Files.move(extractedRule.toPath(), Paths.get(shortend), StandardCopyOption.REPLACE_EXISTING);
					File renamedTempFile = (Paths.get(shortend).toFile());
					//CrySLRule rule = cryslmodelreader.readFromSourceFile(renamedTempFile); //Renaming allows the file to be read by the CrySLModelReader
					renamedTempFile.deleteOnExit(); //Removes the temp CrySL file after jvm is finished
					return renamedTempFile;

				}
			}
			jar.close();

		} else { // Run with IDE
			final URL url = CrySLReader.class.getResource("/" + path);
			if (url != null) {
				try {
					final File apps = new File(url.toURI());
					return apps;
				} catch (URISyntaxException ex) {
					// Handle exception
				}
			}
		}
		return null;


	}
}