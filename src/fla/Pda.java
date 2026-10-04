package fla;

import java.util.*;

/**
 * Nondeterministic PDA. Transition: (state, input|EPS, stackTop|EPS) -> (state, pushString).
 * pushString's first char becomes the new top; "" just pops. A non-EPS stackTop is popped and matched.
 * Explores all branches (BFS) so nondeterminism and epsilon loops are handled; stack depth is capped.
 */
public class Pda {
    public static final char EPS = '\0';
    public enum Mode { FINAL_STATE, EMPTY_STACK }

    private record Rule(String to, String push) {}
    private record Config(String state, int pos, String stack) {}

    private final Map<String, Map<String, List<Rule>>> delta = new HashMap<>(); // state -> "<input><top>" -> rules
    public final Set<String> finals = new TreeSet<>();
    public final String start;
    public final char initialStack;
    public final Mode mode;
    public int maxStack = 1000; // ponytail: depth cap guarantees termination on epsilon-pushing loops

    public Pda(String start, char initialStack, Mode mode) {
        this.start = start;
        this.initialStack = initialStack;
        this.mode = mode;
    }

    public Pda addFinal(String s) { finals.add(s); return this; }

    public Pda addTransition(String from, char input, char top, String to, String push) {
        delta.computeIfAbsent(from, k -> new HashMap<>()).computeIfAbsent("" + input + top, k -> new ArrayList<>()).add(new Rule(to, push));
        return this;
    }

    private boolean accepting(Config c, int len) {
        if (c.pos != len) return false;
        return mode == Mode.FINAL_STATE ? finals.contains(c.state) : c.stack.isEmpty();
    }

    private List<Config> next(Config c, String in) {
        List<Config> out = new ArrayList<>();
        Map<String, List<Rule>> m = delta.get(c.state);
        if (m == null) return out;
        char[] inputs = c.pos < in.length() ? new char[]{in.charAt(c.pos), EPS} : new char[]{EPS};
        for (char i : inputs) {
            int np = i == EPS ? c.pos : c.pos + 1;
            if (!c.stack.isEmpty()) apply(m.get("" + i + c.stack.charAt(0)), c.stack.substring(1), np, out);
            apply(m.get("" + i + EPS), c.stack, np, out);
        }
        return out;
    }

    private void apply(List<Rule> rules, String rest, int pos, List<Config> out) {
        if (rules == null) return;
        for (Rule r : rules) {
            String st = r.push + rest;
            if (st.length() <= maxStack) out.add(new Config(r.to, pos, st));
        }
    }

    public boolean accepts(String input) { return trace(input) != null; }

    /** Accepting run as configurations "(state, remaining input, stack)", or null if rejected. */
    public List<String> trace(String input) {
        Config s = new Config(start, 0, "" + initialStack);
        Map<Config, Config> parent = new HashMap<>();
        parent.put(s, null);
        Deque<Config> q = new ArrayDeque<>(List.of(s));
        while (!q.isEmpty()) {
            Config c = q.poll();
            if (accepting(c, input.length())) {
                LinkedList<String> path = new LinkedList<>();
                for (Config x = c; x != null; x = parent.get(x))
                    path.addFirst("(" + x.state + ", " + (x.pos == input.length() ? "eps" : input.substring(x.pos)) + ", " + (x.stack.isEmpty() ? "eps" : x.stack) + ")");
                return path;
            }
            for (Config n : next(c, input))
                if (!parent.containsKey(n)) { parent.put(n, c); q.add(n); }
        }
        return null;
    }
}
