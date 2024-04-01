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

    public String getClassName(CrySLRule rule) throws IOException {
        return rule.getClassName();
    }


    public String getLinkOnly(CrySLRule rule) throws IOException {
        return rule.getClassName().replaceAll("\\.", "/");
    }

    public String getLink(CrySLRule rule) throws IOException {
        char[] buff = Utils.getTemplatesText("LinkToJavaDoc");
        String link = rule.getClassName().replace(".", "/");
        String cName = rule.getClassName();
        Map<String, String> valuesMap = new HashMap<String, String>();
        valuesMap.put("ClassName", cName);
        valuesMap.put("ClassLink", link);

        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        return sub.replace(buff);

    }

    public String getFullClassName(CrySLRule rule) throws IOException {
        char[] buff = Utils.getTemplatesText("ClassNameClause");
        String className = rule.getClassName();
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("ClassName", className);
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        return sub.replace(buff);
    }

    public String getEventNumbers(CrySLRule rule) throws IOException {

        char[] buff1 = Utils.getTemplatesText("EventNumClause");
        char[] buff2 = Utils.getTemplatesText("EventNumClause2");

        ArrayList<CrySLMethod> methodNames = new ArrayList<CrySLMethod>();
        ArrayList<CrySLMethod> listWithoutDuplicates;
        List<TransitionEdge> graph = rule.getUsagePattern().getEdges();
        String methodNumber = null;
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
        if ("1".equals(methodNumber)) return sub.replace(buff1);
        return sub.replace(buff2);

    }

    public List<String> getForb(CrySLRule rule) throws IOException {
        char[] buff1 = Utils.getTemplatesText("ForbiddenMethodClause");
        char[] buff2 = Utils.getTemplatesText("ForbiddenMethodClauseCon");
        char[] buff3 = Utils.getTemplatesText("ForbiddenMethodClauseAlt");
        char[] buff4 = Utils.getTemplatesText("ForbiddenMethodClauseConAlt");
        StringBuilder sb = new StringBuilder();
        ArrayList<String> composedForbs = new ArrayList<>();
        ArrayList<String> alternatives = new ArrayList<>();
        if (rule.getForbiddenMethods().size() > 0) {
            List<CrySLForbiddenMethod> forbMethods = rule.getForbiddenMethods();
            for (CrySLForbiddenMethod forMethod : forbMethods) {
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
                    } else { //constructor
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

    private boolean checkForConstructor(String fullname) {
        String shortname = fullname.substring(fullname.lastIndexOf('.') + 1);
        int index = fullname.indexOf(shortname);
        int otherIndex = fullname.lastIndexOf(shortname);
        return index != otherIndex;
    }

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
