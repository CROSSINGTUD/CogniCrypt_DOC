package de.upb.docgen.utils;

import java.util.*;

/**
 * GraphVerification provides static methods to verify the ordering of nodes
 * in a graph and to detect strongly connected components (SCCs) using Tarjan's algorithm.
 */
public final class GraphVerification {

    /**
     * Utility class: prevent instantiation.
     */
    private GraphVerification() {}

    /**
     * Verify that a leaf-to-root topological order ends at the requested start node
     * and report whether the start node still sits inside a non-trivial SCC.
     */
    public static void verifyOrdering(String param, Map<String, Set<String>> sanitizedAdj) {
        // Compute ordering from providers (leaves) up to the consumer (start).
        String start = param;
        List<String> order = Utils.leafToRootOrderTopo(start, sanitizedAdj);
        System.out.println("Order size=" + order.size());
        System.out.println("Last element = " + order.get(order.size() - 1));
        if (!order.get(order.size() - 1).equals(start)) {
            throw new IllegalStateException(start + " is not last; ordering still off.");
        }

        // Build the set of nodes reachable from start (following provider edges).
        Map<String, Set<String>> g = sanitizedAdj;
        Set<String> nodes = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String u = stack.pop();
            if (!nodes.add(u)) continue;
            for (String v : g.getOrDefault(u, Set.of())) {
                if (!v.equals(u)) stack.push(v);
            }
        }

        // Run Tarjan SCC on the reachable subgraph to detect remaining cycles.
        List<Set<String>> sccs = new ArrayList<>();
        Map<String, Integer> idx = new HashMap<>();
        Map<String, Integer> low = new HashMap<>();
        Deque<String> tarjanStack = new ArrayDeque<>();
        Set<String> onStack = new HashSet<>();
        int[] id = {0};

        for (String v : nodes) {
            if (!idx.containsKey(v)) {
                tarjan(v, g, idx, low, tarjanStack, onStack, id, sccs);
            }
        }

        // Report whether the start node is in a trivial or cyclic SCC.
        for (Set<String> comp : sccs) {
            if (comp.contains(start)) {
                if (comp.size() == 1) {
                    System.out.println(start + " in singleton SCC (good).");
                } else {
                    System.out.println(start + " still in cyclic SCC: " + comp);
                }
            }
        }
    }

    /**
     * Tarjan DFS step: computes index/lowlink and emits an SCC when a root is found.
     */
    private static void tarjan(String v,
                               Map<String, Set<String>> g,
                               Map<String, Integer> idx,
                               Map<String, Integer> low,
                               Deque<String> stack,
                               Set<String> on,
                               int[] id,
                               List<Set<String>> sccs) {
        // Assign discovery index and initialize lowlink.
        idx.put(v, id[0]);
        low.put(v, id[0]);
        id[0]++;
        stack.push(v);
        on.add(v);
        for (String w : g.getOrDefault(v, Set.of())) {
            if (!idx.containsKey(w)) {
                // Tree edge: explore and propagate lowlink.
                tarjan(w, g, idx, low, stack, on, id, sccs);
                low.put(v, Math.min(low.get(v), low.get(w)));
            } else if (on.contains(w)) {
                // Back edge to an active node: update lowlink.
                low.put(v, Math.min(low.get(v), idx.get(w)));
            }
        }
        if (Objects.equals(low.get(v), idx.get(v))) {
            // v is the root of an SCC; pop until v is removed.
            Set<String> comp = new TreeSet<>();
            String x;
            do {
                x = stack.pop();
                on.remove(x);
                comp.add(x);
            } while (!x.equals(v));
            sccs.add(comp);
        }
    }
}
