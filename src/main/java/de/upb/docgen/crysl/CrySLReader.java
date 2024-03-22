package de.upb.docgen.crysl;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import crypto.cryslhandler.CrySLModelReader;
import crypto.exceptions.CryptoAnalysisException;
import crypto.rules.CrySLRule;

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

		} catch (CryptoAnalysisException e) {
			e.printStackTrace();
		}
		return null;
	}

}