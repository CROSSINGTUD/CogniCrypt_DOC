/*this class contains the functions or getting the classname, number of methods and forbidden methods*/

package de.upb.docgen;

import crypto.rules.CrySLForbiddenMethod;
import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import crypto.rules.TransitionEdge;
import de.upb.docgen.utils.Utils;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author Ritika Singh
 */

public class ClassEventForb {

    public static PrintWriter out;

    /**
     * Return the fully qualified class name for the given CrySL rule.
     */
    public String getClassName(CrySLRule rule) throws IOException {
        return rule.getClassName();
    }

    /**
     * Return a path-like class name used for JavaDoc URL construction.
     */
    public String getLinkOnly(CrySLRule rule) throws IOException {
        return rule.getClassName().replaceAll("\\.", "/");
    }

    /**
     * Full JavaDoc URL for a rule's class.
     *
     * <p>Not every documented class lives in the JDK: javax.servlet is Jakarta EE, and the
     * Oracle JavaSE URL for it silently redirects to a generic landing page rather than
     * 404ing, so it has to be routed separately.
     */
    public String getJavaDocUrl(CrySLRule rule) {
        String className = rule.getClassName();
        String path = className.replace(".", "/");
        if (className.startsWith("javax.servlet.")) {
            return "https://javaee.github.io/javaee-spec/javadocs/" + path + ".html";
        }
        return "https://docs.oracle.com/javase/8/docs/api/" + path + ".html";
    }

    /**
     * Render a JavaDoc link using the LinkToJavaDoc template.
     */
    public String getLink(CrySLRule rule) throws IOException {
        char[] buff = Utils.getTemplatesText("LinkToJavaDoc");
        String link = rule.getClassName().replace(".", "/");
        String cName = rule.getClassName();
        Map<String, String> valuesMap = new HashMap<String, String>();
        valuesMap.put("ClassName", cName);
        valuesMap.put("ClassLink", link);
        valuesMap.put("JavaDocUrl", getJavaDocUrl(rule));

        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        return sub.replace(buff);

    }

    /**
     * Render the class name sentence using the ClassNameClause template.
     */
    public String getFullClassName(CrySLRule rule) throws IOException {
        char[] buff = Utils.getTemplatesText("ClassNameClause");
        String className = rule.getClassName();
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("ClassName", className);
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        return sub.replace(buff);
    }

    /**
     * Compute the number of distinct events (methods) in the rule's usage pattern
     * and render it using the appropriate template variant.
     */
    public String getEventNumbers(CrySLRule rule) throws IOException {

        char[] buff1 = Utils.getTemplatesText("EventNumClause");
        char[] buff2 = Utils.getTemplatesText("EventNumClause2");

        ArrayList<CrySLMethod> methodNames = new ArrayList<CrySLMethod>();
        ArrayList<CrySLMethod> listWithoutDuplicates;
        List<TransitionEdge> graph = rule.getUsagePattern().getEdges();
        String methodNumber = null;
        // Collect unique methods across all transition labels.
        for (TransitionEdge transitionEdge : graph) {
            for (int j = 0; j < transitionEdge.getLabel().size(); j++) {
                CrySLMethod methods = transitionEdge.getLabel().get(j);
                methodNames.add(methods);
                listWithoutDuplicates = (ArrayList<CrySLMethod>) methodNames.stream().distinct()
                        .collect(Collectors.toList());
                methodNumber = String.valueOf(listWithoutDuplicates.size());
            }
        }

        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("number", methodNumber);
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        if ("1".equals(methodNumber))
            return sub.replace(buff1);
        return sub.replace(buff2);

    }

    /**
     * Build a list of formatted forbidden-method sentences for the rule.
     */
    public List<String> getForb(CrySLRule rule) throws IOException {
        char[] buff1 = Utils.getTemplatesText("ForbiddenMethodClause");
        char[] buff2 = Utils.getTemplatesText("ForbiddenMethodClauseCon");
        char[] buff3 = Utils.getTemplatesText("ForbiddenMethodClauseAlt");
        char[] buff4 = Utils.getTemplatesText("ForbiddenMethodClauseConAlt");
        StringBuilder sb = new StringBuilder();
        ArrayList<String> composedForbs = new ArrayList<>();
        if (rule.getForbiddenMethods().size() > 0) {
            List<CrySLForbiddenMethod> forbMethods = rule.getForbiddenMethods();
            // For each forbidden method, resolve its name and alternatives into templates.
            for (CrySLForbiddenMethod forMethod : forbMethods) {
                // Scoped per forbidden method: a shared list would leak one method's
                // alternatives into the next method's "use X instead" suggestion.
                ArrayList<String> alternatives = new ArrayList<>();
                sb.setLength(0);
                sb.append(resolveMethod(forMethod.getMethod()));
                if (forMethod.getAlternatives().size() > 0) {
                    for (CrySLMethod altMethod : forMethod.getAlternatives()) {

                        alternatives.add(resolveMethod(altMethod));

                    }
                }
                if (alternatives.size() > 0) {
                    if (!checkForConstructor(forMethod.getMethod().getMethodName())) {
                        Map<String, String> valuesMap = new HashMap<String, String>();
                        valuesMap.put("ForbMethodName", sb.toString());
                        String resolvedAlternates = String.join(" or ", alternatives);
                        valuesMap.put("Alternate", resolvedAlternates);
                        StringSubstitutor sub = new StringSubstitutor(valuesMap);
                        String resolvedString = sub.replace(buff3);
                        composedForbs.add(resolvedString);
                    } else { // constructor
                        Map<String, String> valuesMap = new HashMap<String, String>();
                        valuesMap.put("ForbMethodName", sb.toString());
                        String resolvedAlternates = String.join(" or ", alternatives);
                        valuesMap.put("Alternate", resolvedAlternates);
                        StringSubstitutor sub = new StringSubstitutor(valuesMap);
                        String resolvedString = sub.replace(buff4);
                        composedForbs.add(resolvedString);
                    }
                } else {
                    if (!checkForConstructor(forMethod.getMethod().getMethodName())) {
                        Map<String, String> valuesMap = new HashMap<String, String>();
                        valuesMap.put("ForbMethodName", sb.toString());
                        StringSubstitutor sub = new StringSubstitutor(valuesMap);
                        String resolvedString = sub.replace(buff1);
                        composedForbs.add(resolvedString);
                    } else {
                        Map<String, String> valuesMap = new HashMap<String, String>();
                        valuesMap.put("ForbMethodName", sb.toString());
                        StringSubstitutor sub = new StringSubstitutor(valuesMap);
                        String resolvedString = sub.replace(buff2);
                        composedForbs.add(resolvedString);
                    }
                }
            }
        }
        return composedForbs;
    }

    /**
     * Heuristic to detect constructor-like method names by repeated short name.
     */
    private boolean checkForConstructor(String fullname) {
        String shortname = fullname.substring(fullname.lastIndexOf('.') + 1);
        int index = fullname.indexOf(shortname);
        int otherIndex = fullname.lastIndexOf(shortname);
        return index != otherIndex;
    }

    /**
     * Render a CrySL method into a short signature string with parameter types.
     */
    private String resolveMethod(CrySLMethod forMethod) {
        StringBuilder sb = new StringBuilder();
        String withoutPackageName = forMethod.getShortMethodName();
        sb.append(withoutPackageName);
        sb.append("(");
        Iterator entryIterator = forMethod.getParameters().iterator();
        while (entryIterator.hasNext()) {
            Entry<String, String> par = (Entry) entryIterator.next();
            sb.append(par.getValue().replaceAll("AnyType", "_"));
            if (entryIterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

}
