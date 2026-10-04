package fla;

import java.util.List;

/** Demo of all five tasks: `java -cp out fla.Main`. */
public class Main {
    public static void main(String[] args) {
        // 1. DFA simulation: strings over {a,b} with an even number of a's
        Dfa even = Tests.evenA();
        System.out.println("== 1. DFA simulation ==");
        for (String w : List.of("", "abab", "ab", "bbb"))
            System.out.println("'" + w + "' -> " + (even.accepts(w) ? "ACCEPTED" : "REJECTED") + "   " + even.trace(w));

        // 2. NFA -> DFA: (a|b)*abb
        Nfa nfa = Tests.endsAbb();
        Dfa conv = nfa.toDfa();
        System.out.println("\n== 2. NFA to DFA (subset construction) ==\n" + conv);

        // 3. Minimization
        System.out.println("== 3. Minimized DFA ==\n" + conv.minimize());

        // 4. Intersection: even a's AND length divisible by 3
        Dfa both = Dfa.intersect(even, Tests.len3());
        System.out.println("== 4. Intersection ==\n" + both);
        for (String w : List.of("aab", "aaab", "bbb", "abab"))
            System.out.println("'" + w + "' -> " + (both.accepts(w) ? "ACCEPTED" : "REJECTED"));

        // 5. PDA: a^n b^n
        Pda pda = new Pda("q0", 'Z', Pda.Mode.FINAL_STATE).addFinal("qf")
            .addTransition("q0", 'a', 'Z', "q0", "AZ").addTransition("q0", 'a', 'A', "q0", "AA")
            .addTransition("q0", Pda.EPS, 'Z', "qf", "Z")
            .addTransition("q0", 'b', 'A', "q1", "").addTransition("q1", 'b', 'A', "q1", "")
            .addTransition("q1", Pda.EPS, 'Z', "qf", "Z");
        System.out.println("\n== 5. PDA (a^n b^n) ==");
        for (String w : List.of("aabb", "aab", "ba"))
            System.out.println("'" + w + "' -> " + (pda.accepts(w) ? "ACCEPTED " + pda.trace(w) : "REJECTED"));
    }
}
