package de.upb.docgen.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Sven Feldmann
 */


public class PredicateTreeGenerator {


    public static Map<String, TreeNode<String>> buildDependencyTreeMap(Map<String, Set<String>> mappedClassnamePredicates) {
        Map<String, TreeNode<String>> chainMap = new HashMap<>();
        Set<String> visitedNodes = new HashSet<>();
        for (String classname : mappedClassnamePredicates.keySet()) {
            TreeNode<String> root = new TreeNode<>(classname);
            for (String nextInChain : mappedClassnamePredicates.get(classname)) {
                if (root.getData().equals(nextInChain)) continue;
                TreeNode<String> child = new TreeNode<>(nextInChain);
                //recursive to populate chain for child
                populatePredicateTree(child, nextInChain, mappedClassnamePredicates, visitedNodes);
                root.addChild(child);
            }
            chainMap.put(classname, root);
        }
        return chainMap;
    }

    private static TreeNode<String> populatePredicateTree(TreeNode<String> firstChild, String nextInChain, Map<String, Set<String>> mappedClassNamePredicates, Set<String> visitedNodes) {
        if (mappedClassNamePredicates.get(nextInChain).size() == 0) {
            return firstChild;
        }

        visitedNodes.add(nextInChain);

        for (String child : mappedClassNamePredicates.get(nextInChain)) {
            if (visitedNodes.contains(child)) {
                // Circular dependency detected
                continue;
            }
            if (firstChild.getData().equals(child)) {
                return firstChild;
            }
            for (TreeNode children : firstChild.getChildren()) {
                if (children.getData().equals(child)) {
                    return firstChild;
                }
            }
            TreeNode<String> childnode = new TreeNode<>(child);
            firstChild.addChild(childnode);
            populatePredicateTree(childnode, child, mappedClassNamePredicates, visitedNodes);
        }

        visitedNodes.remove(nextInChain);

        return firstChild;
    }

}