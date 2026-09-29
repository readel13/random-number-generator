# How `Collections.shuffle` works

Reference notes for the shuffle baseline used in the reports (`org.example.utils.ShuffleUtils`).
Sections 1–6 cover how the JDK implements it; **section 7 explains the benchmark method and the
metrics, and sections 8–12 are the measured head-to-head against the CA rules** on a real webcam
frame.

The implementation details were read out of the JDK this project builds against — **Amazon Corretto
21.0.3**, `lib/src.zip` → `java.base/java/util/Collections.java` and `Random.java` — not from
documentation. The numbers were measured on this machine against `test1.bmp`.

---

## 1. The three overloads (JDK 21)

```java
public static void shuffle(List<?> list)                            // since 1.2
public static void shuffle(List<?> list, Random rnd)                // since 1.2
public static void shuffle(List<?> list, RandomGenerator rnd)       // since 21
```

In JDK 21 the first two are thin wrappers; all the work happens in the `RandomGenerator` version:

```java
public static void shuffle(List<?> list) {
    Random rnd = r;
    if (rnd == null)
        r = rnd = new Random(); // harmless race.
    shuffle(list, rnd);
}

private static Random r;                       // one shared instance for the whole JVM

public static void shuffle(List<?> list, Random rnd) {
    shuffle(list, (RandomGenerator) rnd);      // delegates
}
```

The javadoc on the `Random` overload now says outright that the `RandomGenerator` one "is preferred,
as it is not limited to random generators that extend the `Random` class".

---

## 2. The algorithm: Fisher–Yates (Durstenfeld variant)

There is exactly one shuffling algorithm in the JDK and it is hardcoded:

```java
@SuppressWarnings({"rawtypes", "unchecked"})
public static void shuffle(List<?> list, RandomGenerator rnd) {
    int size = list.size();
    if (size < SHUFFLE_THRESHOLD || list instanceof RandomAccess) {
        for (int i = size; i > 1; i--)
            swap(list, i - 1, rnd.nextInt(i));
    } else {
        Object[] arr = list.toArray();
        for (int i = size; i > 1; i--)
            swap(arr, i - 1, rnd.nextInt(i));
        ListIterator it = list.listIterator();
        for (Object e : arr) { it.next(); it.set(e); }
    }
}
```

`SHUFFLE_THRESHOLD` is `5` (`Collections.java:108`).

**What the loop does.** Walk backwards from the last index to index 1. At each position `i-1`, pick a
random index in `[0, i)` and swap. Once a position has been written it is never touched again, so
after one pass every element has been placed exactly once. That is the modern in-place form of the
Fisher–Yates shuffle (Durstenfeld 1964; Knuth *TAOCP* Vol. 2, Algorithm 3.4.2-P).

**Properties:**

| | |
|---|---|
| Time | Θ(n) — exactly `n-1` swaps and `n-1` `nextInt` calls |
| Extra memory | none for `RandomAccess` lists; one `Object[n]` copy otherwise |
| Uniformity | all `n!` permutations equally likely **iff** the RNG is a fair source (see §4) |
| In-place | yes for `ArrayList`; `LinkedList` goes through the array copy |

**The two branches.** `ArrayList` implements `RandomAccess`, so it is swapped in place. A
`LinkedList` of 5 or more elements is dumped to an array first, shuffled there, then written back
through a `ListIterator` — otherwise each `list.get(randomIndex)` would walk the chain and the
shuffle would be O(n²). A list whose iterator does not support `set` (e.g. `List.of(...)`) throws
`UnsupportedOperationException`.

---

## 3. The default source of randomness

`shuffle(list)` uses **one lazily created `java.util.Random`, shared by the whole JVM**. It is a
48-bit linear congruential generator:

```java
private static final long multiplier = 0x5DEECE66DL;
private static final long addend     = 0xBL;
private static final long mask       = (1L << 48) - 1;

protected int next(int bits) {                        // CAS loop on an AtomicLong seed
    nextseed = (oldseed * multiplier + addend) & mask;
    return (int)(nextseed >>> (48 - bits));
}
```

Seeding, when you never pass your own:

```java
public Random() { this(seedUniquifier() ^ System.nanoTime()); }
// seedUniquifier starts at 8682522807148012L and is multiplied by 1181783497276652981L per call
```

So the whole shuffle is driven by `System.nanoTime()` mixed with a counter — decent for simulation,
worthless as a secret.

`nextInt(bound)` is not a naive `% bound`; it uses rejection sampling to avoid modulo bias, plus a
special case for powers of two that takes the **high** bits (the low bits of an LCG have notoriously
short periods):

```java
int r = next(31);
int m = bound - 1;
if ((bound & m) == 0)                    // bound is a power of 2
    r = (int)((bound * (long) r) >> 31);
else
    for (int u = r; u - (r = u % bound) + m < 0; u = next(31));   // reject over-represented values
```

**Thread safety.** `Random.next` is a CAS loop over an `AtomicLong`, so concurrent shuffles are
correct but contend on that one seed. The `r == null` check is an acknowledged benign race — two
threads may each build a `Random`; only one survives.

---

## 4. Is it configurable?

**Yes for the randomness, no for the algorithm.**

### What you can change

**a) Pass your own `Random`** — including a seed, which makes a run reproducible:

```java
Collections.shuffle(list, new Random(42));   // same permutation every run
```

**b) Pass any `RandomGenerator`** (JDK 21+). Available on this machine, via
`RandomGeneratorFactory.all()`:

```
L128X1024MixRandom, L128X128MixRandom, L128X256MixRandom, L32X64MixRandom,
L64X1024MixRandom, L64X128MixRandom, L64X128StarStarRandom, L64X256MixRandom,
Random, SecureRandom, SplittableRandom, Xoroshiro128PlusPlus, Xoshiro256PlusPlus
```

```java
Collections.shuffle(list, RandomGenerator.of("L64X128MixRandom"));  // 192 state bits
Collections.shuffle(list, RandomGenerator.getDefault());            // L32X64MixRandom
Collections.shuffle(list, new SecureRandom());                      // CSPRNG, ~25x slower
```

`RandomGenerator.getDefault()` is `L32X64MixRandom`. `java.util.Random` reports `stateBits() = 48`;
`L64X128MixRandom` reports `192`.

**c) Nothing else.** There is no SPI, no system property and no security setting that changes what
`Collections.shuffle(list)` does. Note in particular that `-Djava.util.secureRandomSeed=true` affects
**`ThreadLocalRandom` only** (`ThreadLocalRandom.java:419`) — `Collections.shuffle` calls
`new Random()`, so that flag never reaches it.

### What you cannot change

- The shuffling algorithm itself — Fisher–Yates is inlined in the method body.
- The `RandomAccess` / copy-out branch choice.
- The shared static `Random` behind the no-arg overload (it is `private static`, unseedable from
  outside).

---

## 5. Measured on this machine (Corretto 21.0.3)

Shuffling an `ArrayList<Boolean>` of 7,373,232 elements — the bit count of one 640×480 BMP frame
(921,654 bytes), which is what `ShuffleUtils` builds:

| Call | Time |
|---|---|
| `Collections.shuffle(list)` | 112 ms |
| `Collections.shuffle(list, new Random(42))` | 111 ms |
| `Collections.shuffle(list, RandomGenerator.of("L64X128MixRandom"))` | 142 ms |
| `Collections.shuffle(list, new SecureRandom())` | 2799 ms |

Single runs after a warm-up, not JMH — treat them as orders of magnitude. The `SecureRandom` row is
the interesting one: a cryptographic source costs roughly 25× per shuffle.

---

## 6. What this means for this project

Three consequences matter for reading the reports:

**A shuffle is a permutation, so it conserves the multiset of bits.** The number of set bits is
identical before and after — verified: 271,542 ones in the source frame and in every shuffled
variant. This is why the `Shuffle` bar in **One vs Zeros** always matches `WebcamRaw` exactly, and
why shuffling can never correct a bit bias. The CA rules can, because they *compute* new bits rather
than move existing ones.

**It does redistribute bits across byte boundaries**, which is why it still helps the byte histogram:
on a skewed synthetic frame, average deviation from the ideal distribution went 6.08 → 2.18 after one
shuffle (Rule 30 gave 4.95). Byte-level shuffling would have changed nothing there, which is why
`ShuffleUtils` shuffles bits.

**The entropy it adds is capped by the RNG state, not by the frame size.** With the default
generator, the permutation is chosen by a 48-bit LCG state, so at most 2^48 ≈ 2.8 × 10^14 distinct
permutations are reachable regardless of input length — and `n!` already exceeds 2^48 at **n = 17**.
For a 7.4-million-bit frame the reachable fraction is indistinguishable from zero. So the shuffle
injects at most 48 bits of (non-cryptographic, clock-derived) entropy into the whole frame, while the
CA injects exactly 0. Neither is an entropy *source*; both are mixers, and all real entropy has to
come from the webcam. That is the honest framing for the comparison the reports draw.

**Repeated rounds add nothing.** A permutation of a permutation is still a uniform permutation, so
`rounds = 3` is statistically identical to `rounds = 1` (measured: 0.61% similarity to the original
at both). `ShuffleUtils` keeps the round count only so the speed chart compares like-for-like against
N CA generations.

---

## 7. Benchmark method and what the metrics mean

### Two different ways data can fail to look random

Think of a shuffled deck of cards. Two separate things could be wrong with it:

1. **Wrong proportions** — the deck has nine aces and no kings. You can see this just by *counting*,
   without caring about the order.
2. **Right proportions, predictable order** — all 52 cards are present exactly once, but they come
   out in the order they came from the factory. Counting finds nothing wrong; only looking at the
   *sequence* reveals it.

One number cannot catch both, so the benchmark uses one metric for each: **chi-square** counts, and
**gzip** looks at order.

### Metric 1: chi-square — "are the counts even?"

Count how many times each of the 256 possible byte values appears, and compare that to what an even
spread would give.

```java
long[] counts = new long[256];
for (byte v : data) counts[v + 128]++;       // one bucket per byte value
double expected = data.length / 256.0;       // even spread
chi = Σ (counts[i] - expected)² / expected   // sum over all 256 buckets
```

**Dice example.** Roll a die 600 times; you expect 100 of each face.

- Rolls `96, 104, 110, 89, 101, 100` → χ² = (4² + 4² + 10² + 11² + 1² + 0²) / 100 = **2.54**.
  A normal fair-dice result.
- Rolls `300, 60, 60, 60, 60, 60` → χ² = (200² + 5 × 40²) / 100 = **480**. Obviously loaded.

The deviations are squared, so one badly-off bucket dominates — that is the point.

**Reading the number.** The yardstick is the bucket count minus one. With 256 byte buckets that is
255:

| χ² | verdict |
|---|---|
| **≈ 255** | indistinguishable from an even spread — **this is the target** |
| > 293 | fails the evenness test at p = 0.05 |
| > 311 | fails at p = 0.01 |
| 6,867 | the shuffle — still detectable, but ~1,700× closer than the raw frame |
| 11,956,945 | the raw webcam frame |

Two traps worth knowing:

- **χ² = 0 is not a good score, it is a suspicious one.** It means the counts are *exactly* even,
  which real randomness never is. A genuine random source scatters around 255, give or take ~23.
- **χ² grows with sample size.** The same 1% bias measured on 10× more data gives a 10× bigger χ².
  So these numbers are only comparable because every row uses the same 2,764,854-byte frame. Never
  compare a χ² from one file size against another.

**Its blind spot:** χ² only sees *how many*, never *in what order*. The sequence
`0, 1, 2, …, 255, 0, 1, 2, …` has perfectly even counts and zero randomness.

### Metric 2: gzip — "is there a pattern?"

Try to compress the data and see how far it shrinks. Compression works by finding repetition —
DEFLATE spots repeated byte sequences within a 32 KB window (LZ77) and gives shorter codes to
frequent values (Huffman). If the data shrinks, there was a pattern to exploit. If it cannot shrink
at all, there was nothing to find.

```java
ByteArrayOutputStream out = new ByteArrayOutputStream();
try (GZIPOutputStream gz = new GZIPOutputStream(out)) { gz.write(data); }
ratio = 100.0 * out.size() / data.length;    // 100% = did not shrink = no pattern found
```

**Measured examples**, 1 MiB each, to calibrate both metrics at once:

| input | χ² | gzip % | what it shows |
|---|---|---|---|
| `AAAA…A` | 267,386,880 | **0.10** | one value repeated — both metrics scream |
| `0,1,2,…,255` repeating | **0.0** | **0.42** | *perfect* χ², and 240:1 compressible — χ² alone is worthless |
| random `A`/`B` coin flips | 133,169,185 | 15.90 | genuinely unpredictable, but only 2 symbols |
| `SecureRandom` | 281 | 100.03 | what "good" looks like on both |

The second row is the one to remember: a counter scores a flawless chi-square. Only gzip catches it.

Reference points for real data: a photo compresses to ~20% (neighbouring pixels are similar), plain
English text to ~30%, already-compressed or genuinely random data to 100%. Values slightly *above*
100% (our 100.03%) are normal — gzip adds a small header and framing it cannot compress away.

**Its blind spot:** gzip only finds *local, repeated* structure inside a 32 KB window. A counter of
4-byte integers, or a plain LCG, is close to incompressible while being perfectly predictable. So
gzip = 100% means "no obvious pattern", never "cryptographically strong".

### Why both, and why neither is a proof

They cover each other's blind spots: χ² checks the counts, gzip checks the order. Your own data
shows why one alone would mislead — Rule 30 at 50 generations improves χ² by 400× (11.9M → 30k)
while gzip still reports 57%, i.e. the byte *frequencies* were flattening out but the spatial
structure of the photograph was still plainly there. Looking only at the histogram, you would have
concluded it was working far better than it was.

Both are cheap screening tools, not verdicts. For anything written up formally, the real
instruments are **NIST SP 800-22**, **Dieharder**, **TestU01 (BigCrush)** or the small `ent`
utility; they add serial-correlation, runs, spectral and birthday-spacing tests that these two miss
entirely. χ² and gzip are here because they need no dependencies and are enough to *rank* the
options.

### The other three columns

| Column | Meaning | Ideal |
|---|---|---|
| `avg dev %` | `FrameStats.averageDeviationFromIdealDist` — the project's own metric, printed as the histogram report's subtitle | 0 |
| `ones %` | percentage of set bits; catches bit bias, which χ² over bytes only shows indirectly | 50.000 |
| `simil. %` | `MathUtil.compare` against the input frame — how many byte positions are unchanged | 0 |

### How the runs were made

- **Input:** `test1.bmp` from the repo root, a real 1280×720 webcam capture — 2,764,854 bytes =
  22,118,832 bits. Using a real frame matters: synthetic noise has no spatial correlation, so it
  would flatter every method.
- **JDK:** Amazon Corretto 21.0.3, `-Xmx6g`, one JVM per table.
- **Timing:** best of 3 after a warm-up round for the 1× table; single runs for the 10× and 50×
  tables. Wall-clock via `System.nanoTime()`.
- **Not JMH.** No forking, no dead-code-elimination guards, no statistics over iterations. The
  ratios between rows are trustworthy; the absolute milliseconds are not portable.
- Metrics computed with the snippets above, on the full output including the 54-byte BMP header
  (which is what the reports themselves feed to the CA).

---

## 8. Head-to-head with the CA rules (measured)

Setup and column definitions are in §7.

### One generation / one round

| variant | best ms | avg dev % | ones % | simil. % | chi-square | gzip % |
|---|---|---|---|---|---|---|
| raw frame | – | 162.311 | 50.864 | 100.00 | 11,956,945 | 19.47 |
| **SecureRandom** | **6** | **0.844** | **50.017** | 0.39 | **273** | **100.03** |
| CA Rule 30 ×1 | 99 | 122.504 | 46.731 | 0.21 | 5,376,559 | 21.80 |
| CA Rule 90 ×1 | 76 | 130.679 | 50.359 | 0.00 | 6,601,317 | 22.37 |
| CA Rule 105 ×1 | 120 | 111.762 | 54.084 | 0.96 | 3,538,417 | 22.63 |
| CA Rule 150 ×1 | 112 | 111.762 | 45.916 | 0.21 | 3,538,417 | 22.63 |
| Collections.shuffle ×1 | 686 | 4.329 | 50.864 | 0.39 | 6,867 | 100.03 |

### Ten and fifty

| variant | ms @10 | chi² @10 | gzip @10 | ms @50 | chi² @50 | gzip @50 |
|---|---|---|---|---|---|---|
| CA Rule 30 | 770 | 436,358 | 28.42 | 3,651 | 29,943 | 56.95 |
| CA Rule 90 | 514 | 8,141,462 | 27.79 | 1,619 | 6,742,381 | 55.54 |
| CA Rule 105 | 797 | 3,897,895 | 28.60 | 3,543 | 1,458,020 | 60.50 |
| CA Rule 150 | 726 | 3,897,895 | 28.60 | 3,506 | 1,458,020 | 60.50 |
| Collections.shuffle | 5,408 | 6,855 | 100.03 | 26,500 | 6,842 | 100.03 |

Per pass: Rule 90 ≈ 32 ms, Rule 30 ≈ 73 ms, Rules 105/150 ≈ 70 ms, shuffle ≈ 530 ms.

### So: is the shuffle slower?

**Per pass, yes — about 7× slower than Rule 30.** That part of the observation holds.

**Per unit of quality, no — it is far ahead.** One shuffle round costs 686 ms and lands at
chi² 6,867 with gzip 100%. Rule 30 needs *fifty* generations, 3,651 ms — 5× the time — to reach
chi² 29,943, and its output is still 57% compressible. No CA rule at any generation count tested
gets within three orders of magnitude of the shuffle on chi², and none of them makes the output
incompressible. Measured as "quality per millisecond", the ranking is the reverse of the raw
per-pass timings.

---

## 9. Why `Collections.shuffle` is the slow one

Breakdown of one round over the same frame:

| Stage | ms |
|---|---|
| `toBoolCached` (unpack to `boolean[]`) | 10 |
| box into `ArrayList<Boolean>` | 59 |
| **`Collections.shuffle`** | **524** |
| unbox + `toByteArray` | 113 |
| total | 706 |

Three quarters of the cost is inside `shuffle` itself, and it is not the algorithm — Fisher–Yates is
`n-1` swaps, the same linear work the CA does. It is the `List<Boolean>` contract:

- **22.1M `nextInt` calls**, each a CAS loop on the shared `Random`'s `AtomicLong` seed (§3).
- **22.1M random-index accesses into an ~88 MB reference array** — essentially a cache miss per
  swap. The CA, by contrast, is a straight sequential scan over a `boolean[]`, which is exactly what
  a prefetcher likes.
- Boxing indirection on every `get`/`set`. (`Boolean.valueOf` is cached, so nothing is *allocated*
  per element — the cost is the pointer hop, not GC.)

The proof that the API is the bottleneck: the same Fisher–Yates loop written by hand over the
primitive `boolean[]`, with a plain `Random`, runs in **111 ms** — 5× faster, statistically
identical output, and competitive with a single CA generation. Doing that would mean no longer
calling `Collections.shuffle`, which is fine for production but defeats the purpose if the point is
to benchmark the standard library.

---

## 10. What each approach can and cannot fix

The two failure modes are independent, and each method fixes exactly one of them.

**Bit bias — only the CA can fix it.** The frame is 50.864% ones. After any number of shuffle
rounds it is *still exactly* 50.864%, because a permutation cannot change a multiset (§6). Rule 30
at 50 generations pulls it to 50.144%. This is also the whole explanation for the shuffle's residual
chi² of 6,867: a random arrangement of biased bits gives bytes distributed as Bernoulli(0.50864)⁸,
which is not uniform. **With unbiased input bits the shuffle would be statistically perfect** — its
only distributional flaw is inherited from the source.

**Byte-level structure — only the shuffle really fixes it.** One round takes gzip from 19% to 100%
and chi² from 12M to 6.9k. Fifty Rule 30 generations get to 57% and 30k.

**Only Rule 30 mixes at all.** Rules 90, 105 and 150 are *additive* (linear/affine) — `p^r`,
`p^q^!r`, `p^q^r` — so they preserve algebraic structure indefinitely and never converge toward
uniform. Rule 90 is the worst: after 32 generations **157 of the 256 byte values still never occur
at all**, and its chi² at 50 generations (6.7M) is barely better than the raw frame. Rule 30, the
one nonlinear rule in the set, reaches all 256 buckets. If the goal is extraction, the other three
are not candidates — they are there to show what failure looks like.

**Rule 105 and Rule 150 are the same rule.** `rule105 == !rule150` for all 8 neighbourhood inputs
(verified), so on the frame the gen-1 outputs are exact bitwise complements and the gen-2 outputs
are byte-for-byte identical. Every distribution metric is therefore shared, which is why their rows
above are duplicates. Worth knowing before you read anything into the two bars in the charts.

---

## 11. Combining them

Since the weaknesses are complementary, the obvious move is a pipeline. Order matters a great deal:

| pipeline | ms | avg dev % | ones % | chi-square | gzip % |
|---|---|---|---|---|---|
| Rule 30 ×1 → shuffle ×1 | 864 | 15.810 | 46.731 | 96,140 | 100.03 |
| **shuffle ×1 → Rule 30 ×1** | **1,363** | **3.181** | **49.552** | **3,577** | **100.03** |
| Rule 30 ×5 → shuffle ×1 | 1,540 | 3.712 | 49.269 | 4,995 | 100.03 |

**Shuffle first, then one CA generation** beats either component alone on every column — chi² 3,577
against 6,867 for the shuffle by itself, and bit bias down to 49.552%. The reverse order is much
worse, because Rule 30 ×1 *introduces* a bias (46.7% ones) that the shuffle then locks in.

---

## 12. Verdict for the stated goal

The goal is lots of noise, SecureRandom-grade, fast. Against that bar, honestly:

- **Neither approach is close on speed.** `new SecureRandom().nextBytes()` fills the same 2.76 MB in
  **6 ms** at chi² 273 and gzip 100%. The best pipeline here needs 1,363 ms for the same volume at
  worse quality — roughly **200× slower**. Capturing the frame at all costs more than the whole
  `SecureRandom` call.
- **Quality-wise only the shuffle-based paths get into the right neighbourhood**, and they get there
  because of `java.util.Random`, not because of the webcam. That is the uncomfortable part for the
  research framing: the shuffle's excellent histogram is borrowed from a 48-bit clock-seeded LCG
  (§3, §6), so it is measuring the JDK's PRNG as much as the camera.
- **What the CA uniquely provides** is that it is deterministic — it adds no external randomness, so
  whatever entropy the output has came from the frame. That is the defensible claim for the CA
  approach, and it is about *provenance of entropy*, not about throughput or distribution quality.

If speed is what matters next, in order of payoff:

1. **Drop the `List<Boolean>`** — hand-written Fisher–Yates on `boolean[]` is 111 ms vs 686 ms for
   the same result (§9). Keep `Collections.shuffle` in the charts as the stdlib reference point.
2. **Get the work off the EDT** — every report currently blocks the UI for the full duration
   (`SwingWorker`), which at 50 rounds means a 26-second freeze.
3. **Stop evolving the BMP header** — most reports run the CA over all 54 header bytes too; only
   `ByteChangeComparison` skips it via `getImageOffsetBmp`.
4. **Use `shuffle → Rule 30 ×1`** if you want the best quality per millisecond from what exists now.
5. The textbook alternative, if the aim is a usable generator rather than a study of CA: hash the
   frame (SHA-256) and use the digest to seed a DRBG. That keeps the physical entropy source and
   gets cryptographic output rates, but it is no longer a CA experiment.

> Footnote on the speed report: `SpeedRuleChart` measures `SecureRandom.getInstanceStrong()`, which
> on Windows is a different (usually slower, blocking-capable) provider than the `new SecureRandom()`
> used in these tables. The 6 ms figure is the non-strong instance, warm.

---

## 13. Where this lives in the repo

- `src/main/java/org/example/utils/ShuffleUtils.java` — `shuffleBytes(byte[], rounds)` /
  `shuffleBytesBits(byte[], rounds)`, mirroring `CellAutomataUtils`.
- Used as a `Shuffle` series in `SimilarityRuleChart`, `SpeedRuleChart`, `BytesDistributionReport`
  and `OneVsZeros`.
- Cost note: the list must be a `List<Boolean>` for `Collections.shuffle`, so an HD frame boxes ~22M
  `Boolean` references per round on the EDT. `Boolean.valueOf` is cached, so nothing is allocated per
  element, but the backing array is tens of MB.
