package fla;

import java.util.*;

/** NFA with epsilon moves (use Nfa.EPS as the symbol). */
public class Nfa {
    public static final char EPS = '\0';

    public final Set<String> states = new TreeSet<>();
    public final Set<Character> alphabet = new TreeSet<>(); // never contains EPS
    public final Map<String, Map<Character, Set<String>>> delta = new HashMap<>();
    public final Set<String> finals = new TreeSet<>();
    public final String start;

    public Nfa(String start) {
        this.start = start;
        states.add(start);
    }

    public Nfa addState(String s, boolean isFinal) {
        states.add(s);
        if (isFinal) finals.add(s);
        return this;
    }

    public Nfa addTransition(String from, char sym, String to) {
        states.add(from);
        states.add(to);
        if (sym != EPS) alphabet.add(sym);
        delta.computeIfAbsent(from, k -> new HashMap<>()).computeIfAbsent(sym, k -> new TreeSet<>()).add(to);
        return this;
    }

    private Set<String> targets(String s, char c) {
        Map<Character, Set<String>> m = delta.get(s);
        Set<String> t = m == null ? null : m.get(c);
        return t == null ? Set.of() : t;
    }

    public Set<String> closure(Collection<String> from) {
        Set<String> out = new TreeSet<>(from);
        Deque<String> q = new ArrayDeque<>(from);
        while (!q.isEmpty())
            for (String t : targets(q.poll(), EPS)) if (out.add(t)) q.add(t);
        return out;
    }

    private Set<String> move(Set<String> set, char c) {
        Set<String> out = new TreeSet<>();
        for (String s : set) out.addAll(targets(s, c));
        return closure(out);
    }

    public boolean accepts(String input) {
        Set<String> cur = closure(List.of(start));
        for (char c : input.toCharArray()) cur = move(cur, c);
        return cur.stream().anyMatch(finals::contains);
    }

    /** Subset construction. DFA states are named "{q0,q1}"; the empty set "{}" is the dead state. */
    public Dfa toDfa() {
        Set<String> init = closure(List.of(start));
        Dfa d = new Dfa(name(init));
        d.alphabet.addAll(alphabet);
        Deque<Set<String>> q = new ArrayDeque<>(List.of(init));
        Set<String> seen = new HashSet<>(List.of(name(init)));
        while (!q.isEmpty()) {
            Set<String> cur = q.poll();
            d.addState(name(cur), cur.stream().anyMatch(finals::contains));
            for (char c : alphabet) {
                Set<String> nxt = move(cur, c);
                d.addTransition(name(cur), c, name(nxt));
                if (seen.add(name(nxt))) q.add(nxt);
            }
        }
        return d;
    }

    private static String name(Set<String> s) { return "{" + String.join(",", s) + "}"; }
}
