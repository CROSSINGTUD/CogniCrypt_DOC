package de.upb.docgen.utils;

import de.upb.docgen.Order;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FTLTemplateLoaderFromJar {
    public static File readFtlTemplateFromJar(String templateName) throws IOException {
        String pathToLangTemplates = Order.class.getResource("/FTLTemplates").getPath();
        File template;
        String folderName = pathToLangTemplates.substring(pathToLangTemplates.lastIndexOf("/") + 1);
        template = Utils.extract(folderName + "/" + templateName);
        return template;
        /*
        String shortend = template.getName().substring(0,template.getName().indexOf("ftl")+"ftl".length()); //CrySL Rule name without temp file ending
        Files.move(template.toPath(), Paths.get(shortend), StandardCopyOption.REPLACE_EXISTING);
        File renamedTempFile = (Paths.get(shortend).toFile());
        renamedTempFile.deleteOnExit(); //Removes the temp CrySL file after jvm is finished
        return renamedTempFile;

*/
    }
}
