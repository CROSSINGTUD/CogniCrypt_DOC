package de.upb.docgen.utils;

import java.util.*;

public final class GraphSanitizer {
    /**
     * Utility class: prevent instantiation.
     */
    private GraphSanitizer() {}

    /**
     * Normalize the dependency graph for a start node by removing self-loops,
     * restricting to reachable nodes, and collapsing SCCs for stable ordering.
     */
    public static Map<String, Set<String>> sanitize(Map<String, Set<String>> consumerToProviders,
                                                    Map<String, Set<String>> reverse,
                                                    String start) {
        // 1. Defensive copy & normalize nulls
        Map<String, Set<String>> g = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : consumerToProviders.entrySet()) {
            g.put(e.getKey(), new HashSet<>(e.getValue()));
        }
        // Ensure start present
        g.computeIfAbsent(start, k -> new HashSet<>());

        // 2. Recover missing deps for start if it looks suspicious (empty but reverse has candidates)
        if (g.get(start).isEmpty() && reverse != null) {
            Set<String> rev = reverse.getOrDefault(start, Set.of());
            // Heuristic: treat reverse neighbors that themselves have no edge back to start as potential providers
            Set<String> recovered = new HashSet<>();
            for (String cand : rev) {
                if (!consumerToProviders.getOrDefault(cand, Set.of()).contains(start)) {
                    recovered.add(cand);
                }
            }
            if (!recovered.isEmpty()) {
                g.get(start).addAll(recovered);
                System.err.println("[INFO] Recovered potential providers for " + start + ": " + recovered);
            }
        }

        // 3. Remove trivial self loops
        for (Set<String> deps : g.values()) deps.removeIf(Objects::isNull);
        g.forEach((k,v) -> v.remove(k));

        // 4. Restrict to reachable from start
        Set<String> reachable = new HashSet<>();
        Deque<String> dfs = new ArrayDeque<>();
        dfs.push(start);
        while (!dfs.isEmpty()) {
            String u = dfs.pop();
            if (!reachable.add(u)) continue;
            for (String p : g.getOrDefault(u, Set.of())) {
                dfs.push(p);
            }
        }
        g.keySet().retainAll(reachable);
        for (String k : new ArrayList<>(g.keySet())) {
            g.get(k).retainAll(reachable);
        }

        // 5. Collapse SCCs > 1 node
        List<Set<String>> sccs = tarjanSCC(g, reachable);
        boolean hasNonTrivial = sccs.stream().anyMatch(c -> c.size() > 1);
        if (!hasNonTrivial) {
            System.err.println("[INFO] No strongly connected components to collapse for start=" + start + ".");
            return g; // already a DAG
        }

        // Map each node to representative (lexicographically first for determinism)
        Map<String, String> rep = new HashMap<>();
        for (Set<String> comp : sccs) {
            String r = comp.stream().sorted().findFirst().get();
            for (String n : comp) rep.put(n, r);
        }

        Map<String, Set<String>> collapsed = new HashMap<>();
        for (String node : reachable) {
            String rNode = rep.get(node);
            collapsed.computeIfAbsent(rNode, k -> new HashSet<>());
            for (String dep : g.getOrDefault(node, Set.of())) {
                String rDep = rep.get(dep);
                if (!rNode.equals(rDep)) {
                    collapsed.get(rNode).add(rDep);
                }
            }
        }

        // Expand collapsed reps back into original nodes as flat adjacency for ordering:
        // Each member of an SCC shares the union of outgoing edges of its representative.
        Map<String, Set<String>> expanded = new HashMap<>();
        // Precompute component membership
        Map<String, Set<String>> compMembers = new HashMap<>();
        for (Set<String> comp : sccs) {
            String r = rep.get(comp.iterator().next());
            compMembers.computeIfAbsent(r, k -> new HashSet<>()).addAll(comp);
        }
        for (Set<String> comp : sccs) {
            String r = rep.get(comp.iterator().next());
            Set<String> out = collapsed.getOrDefault(r, Set.of());
            for (String member : comp) {
                Set<String> deps = new HashSet<>();
                for (String outRep : out) {
                    deps.addAll(compMembers.getOrDefault(outRep, Set.of()));
                }
                expanded.put(member, deps);
            }
        }

        long collapsedCount = sccs.stream().filter(c -> c.size() > 1).count();
        System.err.println("[INFO] Collapsed " + collapsedCount + " strongly connected component(s) for start=" + start + ":");
        // Detailed enumeration
        int idx = 1;
        // Sort SCCs by first member for determinism
        List<Set<String>> detailed = new ArrayList<>();
        for (Set<String> comp : sccs) if (comp.size() > 1) detailed.add(comp);
        detailed.sort(Comparator.comparing(c -> c.stream().sorted().findFirst().orElse("")));
        for (Set<String> comp : detailed) {
            List<String> members = new ArrayList<>(comp);
            Collections.sort(members);
            String representative = members.get(0);
            List<String> internalEdges = new ArrayList<>();
            for (String src : members) {
                for (String dst : g.getOrDefault(src, Set.of())) {
                    if (comp.contains(dst)) internalEdges.add(src + "->" + dst);
                }
            }
            Collections.sort(internalEdges);
            System.err.println("  [SCC #" + (idx++) + "] rep=" + representative +
                    " size=" + comp.size() +
                    " members=" + members +
                    " internalEdges=" + internalEdges);
        }
        return expanded;
    }

    /**
     * Compute strongly connected components (SCCs) using Tarjan's algorithm.
     */
    private static List<Set<String>> tarjanSCC(Map<String, Set<String>> g, Set<String> nodes) {
        Map<String, Integer> idx = new HashMap<>();
        Map<String, Integer> low = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new HashSet<>();
        List<Set<String>> sccs = new ArrayList<>();
        int[] counter = {0};

        for (String v : nodes) {
            if (!idx.containsKey(v)) {
                dfs(v, g, idx, low, stack, onStack, counter, sccs);
            }
        }
        return sccs;
    }

    /**
     * Depth-first search step for Tarjan's SCC algorithm.
     */
    private static void dfs(String v,
                            Map<String, Set<String>> g,
                            Map<String, Integer> idx,
                            Map<String, Integer> low,
                            Deque<String> stack,
                            Set<String> onStack,
                            int[] counter,
                            List<Set<String>> sccs) {
        idx.put(v, counter[0]);
        low.put(v, counter[0]);
        counter[0]++;
        stack.push(v);
        onStack.add(v);

        for (String w : g.getOrDefault(v, Set.of())) {
            if (!idx.containsKey(w)) {
                dfs(w, g, idx, low, stack, onStack, counter, sccs);
                low.put(v, Math.min(low.get(v), low.get(w)));
            } else if (onStack.contains(w)) {
                low.put(v, Math.min(low.get(v), idx.get(w)));
            }
        }

        if (Objects.equals(low.get(v), idx.get(v))) {
            Set<String> comp = new HashSet<>();
            String x;
            do {
                x = stack.pop();
                onStack.remove(x);
                comp.add(x);
            } while (!x.equals(v));
            sccs.add(comp);
        }
    }
}
