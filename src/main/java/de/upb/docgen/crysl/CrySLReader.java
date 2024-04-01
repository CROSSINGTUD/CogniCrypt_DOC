package de.upb.docgen.crysl;

import java.io.File;
import java.io.IOException;
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

	public static List<CrySLRule> readRulesFromSourceFilesWithoutFiles(final String folderPath) throws CryptoAnalysisException {
		return new ArrayList<>(readRulesFromSourceFiles(folderPath).values());
	}

	public static Map<File, CrySLRule> readRulesFromSourceFiles(final String folderPath) throws  CryptoAnalysisException {
		if (folderPath == null || folderPath.isEmpty()) {
			throw new IllegalArgumentException("Folder path cannot be null or empty");
		}

		CrySLModelReader cryslModelReader = new CrySLModelReader();
		Map<File, CrySLRule> rules = new HashMap<>();

		try {
			File folder = new File(folderPath);
			if (!folder.isDirectory()) {
				throw new IllegalArgumentException("Invalid folder path: " + folderPath);
			}

			for (File file : folder.listFiles()) {
				if (file.getName().endsWith(".crysl")) {
					rules.put(file, cryslModelReader.readRule(file));
				}
			}
		} catch (CryptoAnalysisException e) {
			// Handle CryptoAnalysisException
			throw e;
		}

		return rules;
	}

}