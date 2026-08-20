package de.upb.docgen.utils;

import freemarker.cache.TemplateLoader;

import java.io.*;

// https://stackoverflow.com/questions/1208220/using-an-absolute-path-with-freemarker
public class TemplateAbsolutePathLoader implements TemplateLoader {

    /**
     * Resolve a template source from an absolute path or a file: URI string.
     */
    public Object findTemplateSource(String name) throws IOException {
        if (name == null) return null;
        // Accept file: URIs (common with absolute template paths).
        if (name.startsWith("file:")) {
            try {
                File uriFile = new File(java.net.URI.create(name));
                return uriFile.isFile() ? uriFile : null;
            } catch (IllegalArgumentException e) {
                // Fall back to raw name if URI parsing fails.
            }
        }
        // Fallback: treat the name as a plain filesystem path.
        File source = new File(name);
        return source.isFile() ? source : null;
    }

    /**
     * Report last-modified for template cache invalidation.
     */
    public long getLastModified(Object templateSource) {
        return ((File) templateSource).lastModified();
    }

    /**
     * Open a Reader for the resolved template source using the given encoding.
     */
    public Reader getReader(Object templateSource, String encoding)
            throws IOException {
        if (!(templateSource instanceof File)) {
            throw new IllegalArgumentException("templateSource is a: " + templateSource.getClass().getName());
        }
        return new InputStreamReader(new FileInputStream((File) templateSource), encoding);
    }

    /**
     * No-op: FreeMarker doesn't require explicit close handling for File sources.
     */
    public void closeTemplateSource(Object templateSource) throws IOException {
        // Do nothing.
    }

}
