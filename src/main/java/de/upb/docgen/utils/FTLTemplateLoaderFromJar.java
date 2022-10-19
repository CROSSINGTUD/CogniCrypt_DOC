package de.upb.docgen.utils;

import de.upb.docgen.Order;

import java.io.File;
import java.io.IOException;


public class FTLTemplateLoaderFromJar {
    public static File readFtlTemplateFromJar(String templateName) throws IOException {
        String pathToLangTemplates = Order.class.getResource("/FTLTemplates").getPath();
        String folderName = pathToLangTemplates.substring(pathToLangTemplates.lastIndexOf("/") + 1);
        File template = Utils.extract(folderName + "/" + templateName);
        return template;
    }
}
