package de.upb.docgen.utils;

import de.upb.docgen.Order;

import java.io.File;
import java.io.IOException;


public class FTLTemplateLoaderFromJar {
    /**
     * Load a FreeMarker template bundled in the JAR by extracting it to a temp file.
     */
    public static File readFtlTemplateFromJar(String templateName) throws IOException {
        // Resolve the /FTLTemplates folder from the classpath.
        String pathToLangTemplates = Order.class.getResource("/FTLTemplates").getPath();
        // Extract the folder name to rebuild a classpath resource path.
        String folderName = pathToLangTemplates.substring(pathToLangTemplates.lastIndexOf("/") + 1);
        // Extract the resource into a temp file and return it.
        File template = Utils.extract(folderName + "/" + templateName);
        return template;
    }
}
