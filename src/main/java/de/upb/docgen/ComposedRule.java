package de.upb.docgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sven Feldmann
 *         Data class which stores all generated natural language sentences
 */
public class ComposedRule {

    // Core rule identity and link metadata.
    private String composedClassName;
    private String onlyRuleName;
    private String composedFullClass;
    private String composedLink;
    private String onlyLink;
    private String javaDocUrl;

    // Sections rendered in the HTML.
    private List<String> forbiddenMethods;
    private String numberOfMethods;
    private List<String> methods;
    private List<String> order;
    private List<String> valueConstraints;
    private List<String> constrainedPredicates;
    private List<String> comparsionConstraints;
    private List<String> constrainedValueConstraints;
    private List<String> noCallToConstraints;
    private List<String> instanceOfConstraints;
    private List<String> ConstraintAndEncConstraints;
    private List<String> EnsuresThisPredicates;
    private List<String> EnsuresPredicates;
    private List<String> NegatesPredicates;
    private List<String> allConstraints;

    // LLM augmentation and raw rule text.
    // Initialised so getLlmExplanation() is never null: FreeMarkerWriter calls .get(...)
    // on it directly for four languages, with no guard.
    private Map<String, String> llmExplanation = new HashMap<>();
    private String cryslRuleText;
    private String secureExample;
    private String insecureExample;

    // Dependency ordering for the rule (leaf->root order).
    private List<String> dependency;

    /**
     * Return dependency list for this rule.
     */
    public List<String> getDependency() {
        return dependency;
    }

    /**
     * Set dependency list for this rule.
     */
    public void setDependency(List<String> dependency) {
        this.dependency = dependency;
    }

    /**
     * Return LLM explanations mapped by language name.
     */
    public Map<String, String> getLlmExplanation() {
        return llmExplanation;
    }

    /**
     * Set LLM explanations mapped by language name.
     */
    public void setLlmExplanation(Map<String, String> llmExplanation) {
        this.llmExplanation = llmExplanation;
    }

    /**
     * Return the generated secure example code (if any).
     */
    public String getSecureExample() {
        return secureExample;
    }

    /**
     * Set the generated secure example code.
     */
    public void setSecureExample(String secureExample) {
        this.secureExample = secureExample;
    }

    /**
     * Return the generated insecure example code (if any).
     */
    public String getInsecureExample() {
        return insecureExample;
    }

    /**
     * Set the generated insecure example code.
     */
    public void setInsecureExample(String insecureExample) {
        this.insecureExample = insecureExample;
    }

    /**
     * Return the raw CrySL rule text used for display.
     */
    public String getCryslRuleText() {
        return cryslRuleText;
    }

    /**
     * Set the raw CrySL rule text used for display.
     */
    public void setCryslRuleText(String cryslRuleText) {
        this.cryslRuleText = cryslRuleText;
    }

    /**
     * Return the flattened list of all constraints.
     */
    public List<String> getAllConstraints() {
        return allConstraints;
    }

    /**
     * Set the flattened list of all constraints.
     */
    public void setAllConstraints(List<String> allConstraints) {
        this.allConstraints = allConstraints;
    }

    /**
     * Return the simple rule name for display.
     */
    public String getOnlyRuleName() {
        return onlyRuleName;
    }

    /**
     * Set the simple rule name for display.
     */
    public void setOnlyRuleName(String onlyRuleName) {
        this.onlyRuleName = onlyRuleName;
    }

    /**
     * Return the human-readable class name sentence.
     */
    public String getComposedFullClass() {
        return composedFullClass;
    }

    /**
     * Set the human-readable class name sentence.
     */
    public void setComposedFullClass(String composedFullClass) {
        this.composedFullClass = composedFullClass;
    }

    /**
     * Return the JavaDoc link string.
     */
    public String getComposedLink() {
        return composedLink;
    }

    /**
     * Set the JavaDoc link string.
     */
    public void setComposedLink(String composedLink) {
        this.composedLink = composedLink;
    }

    /**
     * Return the fully qualified class name for the rule.
     */
    public String getComposedClassName() {
        return composedClassName;
    }

    /**
     * Return the list of forbidden methods for display.
     */
    public List<String> getForbiddenMethods() {
        return forbiddenMethods;
    }

    /**
     * Set the list of forbidden methods for display.
     */
    public void setForbiddenMethods(List<String> forbiddenMethods) {
        this.forbiddenMethods = forbiddenMethods;
    }

    /**
     * Return the count of usage-pattern methods.
     */
    public String getNumberOfMethods() {
        return numberOfMethods;
    }

    /**
     * Set the count of usage-pattern methods.
     */
    public void setNumberOfMethods(String numberOfMethods) {
        this.numberOfMethods = numberOfMethods;
    }

    /**
     * Return any precomputed method list (if used).
     */
    public List<String> getMethods() {
        return methods;
    }

    /**
     * Set any precomputed method list.
     */
    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    /**
     * Return the rendered ORDER section.
     */
    public List<String> getOrder() {
        return order;
    }

    /**
     * Set the rendered ORDER section.
     */
    public void setOrder(List<String> order) {
        this.order = order;
    }

    /**
     * Set the fully qualified class name for the rule.
     */
    public void setComposedClassName(String composedClassName) {
        this.composedClassName = composedClassName;
    }

    /**
     * Return value-constraint sentences.
     */
    public List<String> getValueConstraints() {
        return valueConstraints;
    }

    /**
     * Set value-constraint sentences.
     */
    public void setValueConstraints(List<String> valueConstraints) {
        this.valueConstraints = valueConstraints;
    }

    /**
     * Return predicate-constraint sentences.
     */
    public List<String> getConstrainedPredicates() {
        return constrainedPredicates;
    }

    /**
     * Set predicate-constraint sentences.
     */
    public void setConstrainedPredicates(List<String> constrainedPredicates) {
        this.constrainedPredicates = constrainedPredicates;
    }

    /**
     * Return comparison-constraint sentences.
     */
    public List<String> getComparsionConstraints() {
        return comparsionConstraints;
    }

    /**
     * Set comparison-constraint sentences.
     */
    public void setComparsionConstraints(List<String> comparsionConstraints) {
        this.comparsionConstraints = comparsionConstraints;
    }

    /**
     * Return constrained value-constraint sentences.
     */
    public List<String> getConstrainedValueConstraints() {
        return constrainedValueConstraints;
    }

    /**
     * Set constrained value-constraint sentences.
     */
    public void setConstrainedValueConstraints(List<String> constrainedValueConstraints) {
        this.constrainedValueConstraints = constrainedValueConstraints;
    }

    /**
     * Return noCallTo constraint sentences.
     */
    public List<String> getNoCallToConstraints() {
        return noCallToConstraints;
    }

    /**
     * Set noCallTo constraint sentences.
     */
    public void setNoCallToConstraints(List<String> noCallToConstraints) {
        this.noCallToConstraints = noCallToConstraints;
    }

    /**
     * Return instanceof constraint sentences.
     */
    public List<String> getInstanceOfConstraints() {
        return instanceOfConstraints;
    }

    /**
     * Set instanceof constraint sentences.
     */
    public void setInstanceOfConstraints(List<String> instanceOfConstraints) {
        this.instanceOfConstraints = instanceOfConstraints;
    }

    /**
     * Return VC+encmode constraint sentences.
     */
    public List<String> getConstraintAndEncConstraints() {
        return ConstraintAndEncConstraints;
    }

    /**
     * Set VC+encmode constraint sentences.
     */
    public void setConstraintAndEncConstraints(List<String> constraintAndEncConstraints) {
        ConstraintAndEncConstraints = constraintAndEncConstraints;
    }

    /**
     * Return ensures predicates on "this".
     */
    public List<String> getEnsuresThisPredicates() {
        return EnsuresThisPredicates;
    }

    /**
     * Set ensures predicates on "this".
     */
    public void setEnsuresThisPredicates(List<String> ensuresThisPredicates) {
        EnsuresThisPredicates = ensuresThisPredicates;
    }

    /**
     * Return ensures predicates on non-"this" parameters.
     */
    public List<String> getEnsuresPredicates() {
        return EnsuresPredicates;
    }

    /**
     * Set ensures predicates on non-"this" parameters.
     */
    public void setEnsuresPredicates(List<String> ensuresPredicates) {
        EnsuresPredicates = ensuresPredicates;
    }

    /**
     * Return negated predicate sentences.
     */
    public List<String> getNegatesPredicates() {
        return NegatesPredicates;
    }

    /**
     * Set negated predicate sentences.
     */
    public void setNegatesPredicates(List<String> negatesPredicates) {
        NegatesPredicates = negatesPredicates;
    }

    /**
     * Return the JavaDoc path-only link.
     */
    public String getOnlyLink() {
        return onlyLink;
    }

    /**
     * Set the JavaDoc path-only link.
     */
    public String getJavaDocUrl() {
        return javaDocUrl;
    }

    public void setJavaDocUrl(String javaDocUrl) {
        this.javaDocUrl = javaDocUrl;
    }

    public void setOnlyLink(String onlyLink) {
        this.onlyLink = onlyLink;
    }
}
