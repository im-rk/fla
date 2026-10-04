package fla;

import java.util.*;

/** Self-checking tests: run `java -cp out fla.Tests`. Exits non-zero on any failure. */
public class Tests {
    static int checks = 0, fails = 0;

    static void check(boolean ok, String msg) {
        checks++;
        if (!ok) { fails++; System.out.println("FAIL: " + msg); }
    }

    static List<String> allStrings(String alpha, int maxLen) {
        List<String> out = new ArrayList<>(List.of(""));
        for (int i = 0; i < out.size(); i++)
            if (out.get(i).length() < maxLen) for (char c : alpha.toCharArray()) out.add(out.get(i) + c);
        return out;
    }

    // even number of a's
    static Dfa evenA() {
        return new Dfa("e").addState("e", true).addState("o", false)
            .addTransition("e", 'a', "o").addTransition("o", 'a', "e")
            .addTransition("e", 'b', "e").addTransition("o", 'b', "o");
    }

    // length divisible by 3 over {a,b}
    static Dfa len3() {
        Dfa d = new Dfa("0").addState("0", true);
        for (int i = 0; i < 3; i++) for (char c : "ab".toCharArray()) d.addTransition("" + i, c, "" + (i + 1) % 3);
        return d;
    }

    static void dfaSim() {
        Dfa d = evenA();
        check(d.accepts(""), "even: epsilon");
        check(d.accepts("bb") && d.accepts("aa") && d.accepts("abab"), "even: accepts");
        check(!d.accepts("a") && !d.accepts("bab"), "even: rejects");
        check(!d.accepts("abc"), "unknown symbol rejects");
        Dfa partial = new Dfa("s").addState("f", true).addTransition("s", 'x', "f");
        check(partial.accepts("x") && !partial.accepts("xx") && !partial.accepts("y"), "partial DFA");
    }

    // (a|b)*abb
    static Nfa endsAbb() {
        return new Nfa("0").addState("3", true)
            .addTransition("0", 'a', "0").addTransition("0", 'b', "0")
            .addTransition("0", 'a', "1").addTransition("1", 'b', "2").addTransition("2", 'b', "3");
    }

    // epsilon-NFA for a*b* style: (ab)* | a*  via eps moves
    static Nfa epsNfa() {
        return new Nfa("s").addState("s", false).addState("x", true).addState("y", true)
            .addTransition("s", Nfa.EPS, "x").addTransition("s", Nfa.EPS, "p")
            .addTransition("x", 'a', "x")
            .addTransition("p", 'a', "q").addTransition("q", 'b', "y").addTransition("y", Nfa.EPS, "p");
    }

    static void nfaToDfa() {
        for (Nfa n : List.of(endsAbb(), epsNfa())) {
            Dfa d = n.toDfa();
            for (String w : allStrings("ab", 9)) check(n.accepts(w) == d.accepts(w), "nfa vs dfa on '" + w + "'");
        }
        check(endsAbb().toDfa().minimize().states.size() == 4, "abb minimal DFA has 4 states");
        check(endsAbb().accepts("aabb") && !endsAbb().accepts("abba"), "nfa sim");
    }

    static void minimization() {
        // redundant DFA for even a's: duplicated states + unreachable state
        Dfa r = new Dfa("e1").addState("e1", true).addState("e2", true).addState("o1", false).addState("o2", false).addState("zz", true)
            .addTransition("e1", 'a', "o1").addTransition("o1", 'a', "e2").addTransition("e2", 'a', "o2").addTransition("o2", 'a', "e1")
            .addTransition("e1", 'b', "e2").addTransition("e2", 'b', "e1").addTransition("o1", 'b', "o2").addTransition("o2", 'b', "o1")
            .addTransition("zz", 'a', "zz").addTransition("zz", 'b', "zz");
        Dfa m = r.minimize();
        check(m.states.size() == 2, "even-a minimizes to 2 states, got " + m.states.size());
        for (String w : allStrings("ab", 9)) check(r.accepts(w) == m.accepts(w), "min equiv '" + w + "'");
        check(m.minimize().states.size() == 2, "idempotent");
        // empty language -> single dead state; universal -> single state
        Dfa none = new Dfa("a").addTransition("a", 'x', "b").addTransition("b", 'x', "a");
        check(none.minimize().states.size() == 1 && none.minimize().finals.isEmpty(), "empty language");
        Dfa all = new Dfa("a").addState("a", true).addState("b", true).addTransition("a", 'x', "b").addTransition("b", 'x', "a");
        check(all.minimize().states.size() == 1, "universal language");
        // partial DFA gets completed with dead state
        Dfa p = new Dfa("s").addState("f", true).addTransition("s", 'x', "f");
        Dfa pm = p.minimize();
        check(pm.states.size() == 3 && pm.accepts("x") && !pm.accepts("xx"), "partial minimize");
    }

    static void intersection() {
        Dfa a = evenA(), b = len3();
        Dfa p = Dfa.intersect(a, b);
        for (String w : allStrings("ab", 10)) check(p.accepts(w) == (a.accepts(w) && b.accepts(w)), "intersect '" + w + "'");
        check(p.states.size() == 6, "product 2x3 states");
        // different alphabets: L1 over {a,b} even a's, L2 over {a} only
        Dfa onlyA = new Dfa("s").addState("s", true).addTransition("s", 'a', "s");
        Dfa q = Dfa.intersect(a, onlyA);
        for (String w : allStrings("ab", 8)) check(q.accepts(w) == (a.accepts(w) && onlyA.accepts(w)), "intersect alpha '" + w + "'");
        // disjoint -> empty language
        Dfa odd = new Dfa("e").addState("o", true).addTransition("e", 'a', "o").addTransition("o", 'a', "e");
        Dfa ea = new Dfa("e").addState("e", true).addTransition("e", 'a', "o").addTransition("o", 'a', "e");
        check(Dfa.intersect(odd, ea).minimize().finals.isEmpty(), "disjoint intersection empty");
    }

    static void pdaChecks() {
        // a^n b^n (n>=0), final-state
        Pda anbn = new Pda("q0", 'Z', Pda.Mode.FINAL_STATE).addFinal("qf")
            .addTransition("q0", 'a', 'Z', "q0", "AZ").addTransition("q0", 'a', 'A', "q0", "AA")
            .addTransition("q0", Pda.EPS, 'Z', "qf", "Z")
            .addTransition("q0", 'b', 'A', "q1", "").addTransition("q1", 'b', 'A', "q1", "")
            .addTransition("q1", Pda.EPS, 'Z', "qf", "Z");
        for (String w : allStrings("ab", 10)) {
            boolean exp = w.matches("a*b*") && w.indexOf('b') < 0 == (w.chars().filter(c -> c == 'a').count() == 0) || w.isEmpty();
            int n = w.length() / 2;
            exp = w.equals("a".repeat(n) + "b".repeat(n)) && w.length() % 2 == 0;
            check(anbn.accepts(w) == exp, "a^n b^n '" + w + "'");
        }
        List<String> t = anbn.trace("aabb");
        check(t != null && t.get(0).equals("(q0, aabb, Z)") && t.get(t.size() - 1).startsWith("(qf, eps"), "trace shows stack ops: " + t);

        // even-length palindromes over {a,b}, nondeterministic, empty-stack
        Pda pal = new Pda("p", 'Z', Pda.Mode.EMPTY_STACK)
            .addTransition("p", 'a', 'Z', "p", "aZ").addTransition("p", 'b', 'Z', "p", "bZ")
            .addTransition("p", 'a', 'a', "p", "aa").addTransition("p", 'a', 'b', "p", "ab")
            .addTransition("p", 'b', 'a', "p", "ba").addTransition("p", 'b', 'b', "p", "bb")
            .addTransition("p", Pda.EPS, 'Z', "m", "Z")
            .addTransition("p", Pda.EPS, 'a', "m", "a").addTransition("p", Pda.EPS, 'b', "m", "b")
            .addTransition("m", 'a', 'a', "m", "").addTransition("m", 'b', 'b', "m", "")
            .addTransition("m", Pda.EPS, 'Z', "m", "");
        for (String w : allStrings("ab", 10)) {
            boolean exp = w.length() % 2 == 0 && new StringBuilder(w).reverse().toString().equals(w);
            check(pal.accepts(w) == exp, "palindrome '" + w + "'");
        }

        // balanced parentheses, empty-stack
        Pda par = new Pda("q", 'Z', Pda.Mode.EMPTY_STACK)
            .addTransition("q", '(', 'Z', "q", "PZ").addTransition("q", '(', 'P', "q", "PP")
            .addTransition("q", ')', 'P', "q", "").addTransition("q", Pda.EPS, 'Z', "q", "");
        for (String w : allStrings("()", 10)) {
            int bal = 0; boolean ok = true;
            for (char c : w.toCharArray()) { bal += c == '(' ? 1 : -1; if (bal < 0) ok = false; }
            check(par.accepts(w) == (ok && bal == 0), "parens '" + w + "'");
        }

        // epsilon-pushing loop must terminate and reject
        Pda loop = new Pda("s", 'Z', Pda.Mode.FINAL_STATE).addFinal("f").addTransition("s", Pda.EPS, Pda.EPS, "s", "X");
        loop.maxStack = 50;
        check(!loop.accepts("a"), "epsilon loop terminates");
    }

    static void randomised() {
        Random rnd = new Random(42);
        for (int iter = 0; iter < 200; iter++) {
            int n = 1 + rnd.nextInt(6);
            Dfa a = randDfa(rnd, n), b = randDfa(rnd, 1 + rnd.nextInt(5));
            Dfa m = a.minimize(), p = Dfa.intersect(a, b);
            check(m.states.size() <= a.states.size() + 1, "min not larger");
            check(m.minimize().states.size() == m.states.size(), "min idempotent");
            Nfa nf = randNfa(rnd, n);
            Dfa nd = nf.toDfa();
            for (String w : allStrings("ab", 7)) {
                check(a.accepts(w) == m.accepts(w), "rand min");
                check(p.accepts(w) == (a.accepts(w) && b.accepts(w)), "rand intersect");
                check(nf.accepts(w) == nd.accepts(w), "rand nfa->dfa");
            }
        }
    }

    static Dfa randDfa(Random r, int n) {
        Dfa d = new Dfa("0");
        for (int i = 0; i < n; i++) {
            d.addState("" + i, r.nextBoolean());
            for (char c : "ab".toCharArray()) if (r.nextInt(10) > 0) d.addTransition("" + i, c, "" + r.nextInt(n)); // some partial
        }
        return d;
    }

    static Nfa randNfa(Random r, int n) {
        Nfa x = new Nfa("0");
        for (int i = 0; i < n; i++) {
            x.addState("" + i, r.nextBoolean());
            for (char c : new char[]{'a', 'b', Nfa.EPS}) for (int j = 0; j < n; j++) if (r.nextInt(4) == 0) x.addTransition("" + i, c, "" + j);
        }
        return x;
    }

    public static void main(String[] args) {
        dfaSim(); nfaToDfa(); minimization(); intersection(); pdaChecks(); randomised();
        System.out.println(checks + " checks, " + fails + " failures");
        System.exit(fails == 0 ? 0 : 1);
    }
}
