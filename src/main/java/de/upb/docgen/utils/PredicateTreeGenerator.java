package de.upb.docgen.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Sven Feldmann
 */


public class PredicateTreeGenerator {


    /**
     * Build a dependency tree per class name where each root is a rule/class and
     * children are the classes it depends on (directly or transitively).
     */
    public static Map<String, TreeNode<String>> buildDependencyTreeMap(Map<String, Set<String>> mappedClassnamePredicates) {
        Map<String, TreeNode<String>> chainMap = new HashMap<>();
        Set<String> visitedNodes = new HashSet<>();
        for (String classname : mappedClassnamePredicates.keySet()) {
            TreeNode<String> root = new TreeNode<>(classname);
            for (String nextInChain : mappedClassnamePredicates.get(classname)) {
                if (root.getData().equals(nextInChain)) continue;
                TreeNode<String> child = new TreeNode<>(nextInChain);
                // Recursive step to populate the child's subtree.
                populatePredicateTree(child, nextInChain, mappedClassnamePredicates, visitedNodes);
                root.addChild(child);
            }
            chainMap.put(classname, root);
        }
        return chainMap;
    }

    /**
     * Recursively expand a dependency subtree while avoiding cycles and duplicates.
     */
    private static TreeNode<String> populatePredicateTree(TreeNode<String> firstChild, String nextInChain, Map<String, Set<String>> mappedClassNamePredicates, Set<String> visitedNodes) {
        // A dependency naming a class with no rule of its own would otherwise NPE here.
        Set<String> dependencies = mappedClassNamePredicates.get(nextInChain);
        if (dependencies == null || dependencies.isEmpty()) {
            // Leaf node: no further dependencies to expand.
            return firstChild;
        }

        // Track current path to detect circular dependencies.
        visitedNodes.add(nextInChain);

        // Every skip below uses `continue`, never `return`. Returning would abandon the
        // remaining siblings AND leave nextInChain in the shared visitedNodes set, which
        // would then make every later root treat this class as already-visited and silently
        // truncate its dependency tree. Both branches are currently unreachable, but the
        // reasoning that makes them so lives outside this method.
        for (String child : dependencies) {
            if (visitedNodes.contains(child)) {
                // Circular dependency detected; skip expanding this edge.
                continue;
            }
            if (firstChild.getData().equals(child)) {
                continue;
            }
            boolean alreadyAChild = false;
            for (TreeNode children : firstChild.getChildren()) {
                if (children.getData().equals(child)) {
                    // Avoid adding duplicate child nodes at this level.
                    alreadyAChild = true;
                    break;
                }
            }
            if (alreadyAChild) {
                continue;
            }
            TreeNode<String> childnode = new TreeNode<>(child);
            firstChild.addChild(childnode);
            populatePredicateTree(childnode, child, mappedClassNamePredicates, visitedNodes);
        }

        // Pop from current path when unwinding recursion.
        visitedNodes.remove(nextInChain);

        return firstChild;
    }

}