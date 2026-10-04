package fla;

import java.util.*;

/** DFA over char symbols. Missing transitions are treated as going to an implicit dead state. */
public class Dfa {
    public final Set<String> states = new TreeSet<>();
    public final Set<Character> alphabet = new TreeSet<>();
    public final Map<String, Map<Character, String>> delta = new HashMap<>();
    public final Set<String> finals = new TreeSet<>();
    public final String start;

    public Dfa(String start) {
        this.start = start;
        states.add(start);
    }

    public Dfa addState(String s, boolean isFinal) {
        states.add(s);
        if (isFinal) finals.add(s);
        return this;
    }

    public Dfa addTransition(String from, char sym, String to) {
        states.add(from);
        states.add(to);
        alphabet.add(sym);
        delta.computeIfAbsent(from, k -> new HashMap<>()).put(sym, to);
        return this;
    }

    /** Next state, or null if undefined (dead). */
    public String step(String s, char c) {
        if (s == null) return null;
        Map<Character, String> m = delta.get(s);
        return m == null ? null : m.get(c);
    }

    public boolean accepts(String input) {
        String s = start;
        for (char c : input.toCharArray()) {
            s = step(s, c);
            if (s == null) return false;
        }
        return finals.contains(s);
    }

    /** Human-readable state path, e.g. "q0 -a-> q1 -b-> q0"; ends with "(dead)" if stuck. */
    public String trace(String input) {
        StringBuilder sb = new StringBuilder(start);
        String s = start;
        for (char c : input.toCharArray()) {
            s = step(s, c);
            if (s == null) return sb.append(" -").append(c).append("-> (dead)").toString();
            sb.append(" -").append(c).append("-> ").append(s);
        }
        return sb.toString();
    }

    /** Copy completed over the given alphabet with an explicit dead state if needed. */
    Dfa complete(Set<Character> alpha) {
        Dfa d = new Dfa(start);
        d.alphabet.addAll(alpha);
        d.states.addAll(states);
        d.finals.addAll(finals);
        String dead = "dead";
        while (states.contains(dead)) dead += "_";
        boolean usedDead = false;
        for (String s : states)
            for (char c : alpha) {
                String t = step(s, c);
                if (t == null) { t = dead; usedDead = true; }
                d.addTransition(s, c, t);
            }
        if (usedDead) for (char c : alpha) d.addTransition(dead, c, dead);
        return d;
    }

    private Set<String> reachable() {
        Set<String> seen = new LinkedHashSet<>(List.of(start));
        Deque<String> q = new ArrayDeque<>(seen);
        while (!q.isEmpty()) {
            String s = q.poll();
            for (char c : alphabet) {
                String t = step(s, c);
                if (t != null && seen.add(t)) q.add(t);
            }
        }
        return seen;
    }

    /** Minimal complete DFA (unreachable states removed, equivalent states merged) via partition refinement. */
    public Dfa minimize() {
        Dfa d = complete(alphabet);
        Set<String> reach = d.reachable();
        Map<String, Integer> block = new HashMap<>();
        for (String s : reach) block.put(s, d.finals.contains(s) ? 1 : 0);
        int count = -1;
        while (true) {
            Map<List<Integer>, Integer> sigs = new HashMap<>();
            Map<String, Integer> next = new HashMap<>();
            for (String s : reach) {
                List<Integer> sig = new ArrayList<>();
                sig.add(block.get(s));
                for (char c : d.alphabet) sig.add(block.get(d.step(s, c)));
                next.put(s, sigs.computeIfAbsent(sig, k -> sigs.size()));
            }
            block = next;
            if (sigs.size() == count) break;
            count = sigs.size();
        }
        Map<Integer, TreeSet<String>> members = new TreeMap<>();
        for (String s : reach) members.computeIfAbsent(block.get(s), k -> new TreeSet<>()).add(s);
        Map<Integer, String> name = new HashMap<>();
        members.forEach((id, m) -> name.put(id, m.size() == 1 ? m.first() : "{" + String.join(",", m) + "}"));
        Dfa r = new Dfa(name.get(block.get(d.start)));
        r.alphabet.addAll(d.alphabet);
        for (var e : members.entrySet()) {
            String rep = e.getValue().first();
            String n = name.get(e.getKey());
            r.addState(n, d.finals.contains(rep));
            for (char c : d.alphabet) r.addTransition(n, c, name.get(block.get(d.step(rep, c))));
        }
        return r;
    }

    /** Product construction for L(a) ∩ L(b). Only reachable pairs are built. */
    public static Dfa intersect(Dfa a, Dfa b) {
        Set<Character> alpha = new TreeSet<>(a.alphabet);
        alpha.addAll(b.alphabet);
        Dfa x = a.complete(alpha), y = b.complete(alpha);
        Dfa p = new Dfa(pair(x.start, y.start));
        p.alphabet.addAll(alpha);
        Deque<String[]> q = new ArrayDeque<>();
        q.add(new String[]{x.start, y.start});
        Set<String> seen = new HashSet<>(List.of(p.start));
        while (!q.isEmpty()) {
            String[] cur = q.poll();
            String name = pair(cur[0], cur[1]);
            p.addState(name, x.finals.contains(cur[0]) && y.finals.contains(cur[1]));
            for (char c : alpha) {
                String s = x.step(cur[0], c), t = y.step(cur[1], c);
                p.addTransition(name, c, pair(s, t));
                if (seen.add(pair(s, t))) q.add(new String[]{s, t});
            }
        }
        return p;
    }

    private static String pair(String a, String b) { return "(" + a + "," + b + ")"; }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("start=" + start + " finals=" + finals + "\n");
        for (String s : states)
            for (char c : alphabet) {
                String t = step(s, c);
                if (t != null) sb.append("  ").append(s).append(" -").append(c).append("-> ").append(t).append('\n');
            }
        return sb.toString();
    }
}
