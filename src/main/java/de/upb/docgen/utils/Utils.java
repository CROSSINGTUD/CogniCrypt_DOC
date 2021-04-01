package de.upb.docgen.utils;

import java.io.File;
import java.net.URL;

public class Utils {
	public static File getFileFromResources(String fileName) {
		URL resource = Utils.class.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("File could not be found!");
		} else {
			return new File(resource.getFile());
		}
	}
	
	
}
