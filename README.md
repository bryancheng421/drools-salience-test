# Drools Salience Tie-Break Test
A minimal, zero-setup sandbox for testing how Drools resolves firing order
when two rules have the **same salience**.

## Why
Drools' documented behavior is that when rules have equal salience, the
engine falls back to a default conflict resolution strategy — but online
sources disagree on what that strategy actually is (some say load order /
FIFO, some say LIFO, some say it's effectively unspecified). Rather than
assume, this repo empirically tests it against a real Drools version.

## What's here
- `SalienceTest.java` — a self-contained [JBang](https://www.jbang.dev/)
  script. It loads `rules.drl` from disk, builds it in-memory with
  `KieFileSystem`, then runs **100 fresh sessions**, inserting a single
  fact (`"go"`) into each and recording which rule fires first.
- `rules.drl` — two rules, both matching the same fact (`String(this ==
  "go")`), both with `salience 0`. Firing order is recorded into a global
  `List` so it can be inspected per-run rather than just printed to
  console.

Using a fact-based match (instead of bare `when / then` rules with no
conditions) matters here — it ensures both rules are genuinely activated
in the same propagation cycle off the same insert, so what's being tested
is real agenda tie-break behavior, not an artifact of file/compile order
sneaking in some other way.

## Running it
### GitHub Codespaces (recommended, no local install)
1. Click **Code → Codespaces → Create codespace on main**.
2. Open a terminal and check Java is available:
```bash
   java -version
```
   If it's missing, install a JDK:
```bash
   sudo apt update && sudo apt install -y default-jdk
```
3. Install JBang:
```bash
   curl -Ls https://sh.jbang.dev | bash -s - app setup
   source ~/.bashrc
   jbang --version
```
4. Run the test:
```bash
   jbang SalienceTest.java
```

### Locally
Requires a JDK and [JBang](https://www.jbang.dev/download/) installed.

```bash
jbang SalienceTest.java
```

First run downloads Drools dependencies (cached afterward).

## What we found
Running 100 trials with "Rule A" declared before "Rule B" in `rules.drl`:

```
Rule A fired first: 100 times
Rule B fired first: 0 times
```

Swapping the declaration order (Rule B before Rule A) and rerunning 100
more trials flipped the result completely:
```
Rule B fired first: 100 times
Rule A fired first: 0 times
```

**Conclusion for this specific Drools version (8.44.0.Final) and this
specific scenario** (one fact, one insert, two rules, equal salience,
same propagation cycle): tie-break is deterministic and follows
**declaration order in the .drl file** — whichever rule is declared first
fires first, consistently, every run.

## Important caveat
This result is empirically solid for the exact setup tested here, but it
should **not** be treated as a general guarantee:

- Drools' internal conflict resolution has changed across major versions
  (5.x/6.x/7.x/8.x), so this exact tie-break behavior may not hold on
  other versions.
- More complex scenarios — multiple facts inserted in varying order, rule
  chaining/recursion where one rule's action triggers another rule's
  activation mid-execution — can produce different-looking tie-break
  behavior (this is likely where "LIFO" reports online come from; it's a
  different mechanism than the single-insert case tested here).
- This is undocumented, implementation-specific behavior, not a supported
  contract.

**Practical takeaway: don't design production rule logic that depends on
tie-break order.** Equal salience is, by design, "no preference" from the
rule author's side. If firing order matters, use explicit, distinct
salience values instead.

## Experimenting further
- **Swap rule order** in `rules.drl` and rerun to reproduce the flip
  described above.
- **Change salience values** to confirm higher salience always wins
  regardless of declaration order (sanity check before testing ties).
- **Insert multiple facts** in different orders to see how that
  interacts with the tie-break — a genuinely different scenario from what's
  tested here.
- **Trigger rule chaining** (have one rule's `then` block insert a new
  fact that activates the other rule) to test whether that produces
  different — possibly LIFO-like — behavior.

## Notes
- Drools version used: `8.44.0.Final` (declared via `//DEPS` in
  `SalienceTest.java` — change it there to test other versions).
EOF