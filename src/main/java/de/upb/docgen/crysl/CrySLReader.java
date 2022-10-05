package de.upb.docgen.crysl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import crypto.cryslhandler.CrySLModelReader;
import crypto.rules.CrySLRule;
import de.upb.docgen.utils.Utils;
import sun.misc.Launcher;

/**
 * @author Ritika Singh
 */

public class CrySLReader {

	public static List<CrySLRule> readRulesFromSourceFilesWithoutFiles(final String folderPath) {
		return new ArrayList<>(readRulesFromSourceFiles(folderPath).values());
	}

	public static Map<File, CrySLRule> readRulesFromSourceFiles(final String folderPath) {

		File f = null;
		try {
			CrySLModelReader cryslmodelreader = new CrySLModelReader();

			Map<File, CrySLRule> rules = new HashMap<>();
			File[] files = new File(folderPath).listFiles();
			for (File file : files) {
				if (file != null && file.getName().endsWith(".crysl")) {
					f = file;
					rules.put(file, cryslmodelreader.readRule(file));
				}
			}
			// System.out.println(rules);
			return rules;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}


	public static Map<File, CrySLRule> readRulesFromJar() throws IOException {

		final String path = "CrySLRules";
		final File jarFile = new File(CrySLReader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		CrySLModelReader cryslmodelreader = new CrySLModelReader();

		Map<File, CrySLRule> rules = new HashMap<>();
		if(jarFile.isFile()) {  // Run with JAR file
			final JarFile jar = new JarFile(jarFile);
			final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
			while(entries.hasMoreElements()) {
				final JarEntry name = entries.nextElement();
				if (name.getName().endsWith(".crysl")) { //only handle crysl files
					File extractedRule = Utils.extract(name.getName());  //Create a temporary crysl file
					String shortend = extractedRule.getName().substring(0,extractedRule.getName().indexOf("crysl")+"crysl".length()); //CrySL Rule name without temp file ending
					Files.move(extractedRule.toPath(), Paths.get(shortend), StandardCopyOption.REPLACE_EXISTING);
					File renamedTempFile = (Paths.get(shortend).toFile());
					CrySLRule rule = cryslmodelreader.readRule(renamedTempFile); //Renaming allows the file to be read by the CrySLModelReader
					renamedTempFile.deleteOnExit(); //Removes the temp CrySL file after jvm is finished
					rules.put(renamedTempFile, rule);

				}
			}
			jar.close();
		} else { // Run with IDE
			final URL url = Launcher.class.getResource("/" + path);
			if (url != null) {
				try {
					final File apps = new File(url.toURI());
					for (File file : apps.listFiles()) {
						rules.put(file, cryslmodelreader.readRule(file));

					}
				} catch (URISyntaxException ex) {

				}
			}
		}
		return rules;


	}



}