package de.upb.docgen;

import java.util.List;

/**
 * Data class which stores all generated natural language sentences
 */

public class ComposedRule {

    private String composedClassName;
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
    private String onlyRuleName;
    private String composedFullClass;
    private String composedLink;
    private String onlyLink;


    public List<String> getAllConstraints() {
        return allConstraints;
    }

    public void setAllConstraints(List<String> allConstraints) {
        this.allConstraints = allConstraints;
    }

    public String getOnlyRuleName() { return onlyRuleName;}

    public void setOnlyRuleName(String onlyRuleName) { this.onlyRuleName = onlyRuleName;}

    public String getComposedFullClass() { return composedFullClass;}

    public void setComposedFullClass(String composedFullClass) { this.composedFullClass = composedFullClass;}

    public String getComposedLink() { return composedLink;}

    public void setComposedLink(String composedLink) { this.composedLink = composedLink;}


    public String getComposedClassName() {
        return composedClassName;
    }


    public List<String> getForbiddenMethods() {
        return forbiddenMethods;
    }

    public void setForbiddenMethods(List<String> forbiddenMethods) {
        this.forbiddenMethods = forbiddenMethods;
    }

    public String getNumberOfMethods() {
        return numberOfMethods;
    }

    public void setNumberOfMethods(String numberOfMethods) {
        this.numberOfMethods = numberOfMethods;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public List<String> getOrder() {
        return order;
    }

    public void setOrder(List<String> order) {
        this.order = order;
    }

    public void setComposedClassName(String composedClassName) {
        this.composedClassName = composedClassName;
    }

    public List<String> getValueConstraints() {
        return valueConstraints;
    }

    public void setValueConstraints(List<String> valueConstraints) {
        this.valueConstraints = valueConstraints;
    }

    public List<String> getConstrainedPredicates() {
        return constrainedPredicates;
    }

    public void setConstrainedPredicates(List<String> constrainedPredicates) {
        this.constrainedPredicates = constrainedPredicates;
    }

    public List<String> getComparsionConstraints() {
        return comparsionConstraints;
    }

    public void setComparsionConstraints(List<String> comparsionConstraints) {
        this.comparsionConstraints = comparsionConstraints;
    }

    public List<String> getConstrainedValueConstraints() {
        return constrainedValueConstraints;
    }

    public void setConstrainedValueConstraints(List<String> constrainedValueConstraints) {
        this.constrainedValueConstraints = constrainedValueConstraints;
    }

    public List<String> getNoCallToConstraints() {
        return noCallToConstraints;
    }

    public void setNoCallToConstraints(List<String> noCallToConstraints) {
        this.noCallToConstraints = noCallToConstraints;
    }

    public List<String> getInstanceOfConstraints() {
        return instanceOfConstraints;
    }

    public void setInstanceOfConstraints(List<String> instanceOfConstraints) {
        this.instanceOfConstraints = instanceOfConstraints;
    }

    public List<String> getConstraintAndEncConstraints() {
        return ConstraintAndEncConstraints;
    }

    public void setConstraintAndEncConstraints(List<String> constraintAndEncConstraints) {
        ConstraintAndEncConstraints = constraintAndEncConstraints;
    }

    public List<String> getEnsuresThisPredicates() {
        return EnsuresThisPredicates;
    }

    public void setEnsuresThisPredicates(List<String> ensuresThisPredicates) {
        EnsuresThisPredicates = ensuresThisPredicates;
    }

    public List<String> getEnsuresPredicates() {
        return EnsuresPredicates;
    }

    public void setEnsuresPredicates(List<String> ensuresPredicates) {
        EnsuresPredicates = ensuresPredicates;
    }

    public List<String> getNegatesPredicates() {
        return NegatesPredicates;
    }

    public void setNegatesPredicates(List<String> negatesPredicates) {
        NegatesPredicates = negatesPredicates;
    }

    public String getOnlyLink() {
        return onlyLink;
    }

    public void setOnlyLink(String onlyLink) {
        this.onlyLink = onlyLink;
    }
}
