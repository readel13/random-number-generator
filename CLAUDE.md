# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A Java Swing research tool that evaluates **elementary cellular automata (ECA) as a randomness extractor over live webcam frames**. A frame is captured as raw BMP bytes, the bytes are unpacked to a bit array, one or more CA generations are applied, and the result is measured against `SecureRandom` as the baseline. Everything else in the repo — the charts, the dropdowns, the frames — exists to visualize that comparison.

## Build and run

Maven project, Java 21 source/target. There is no Maven wrapper (`mvnw`) and `mvn` is not on PATH on this machine; the IDE (IntelliJ, project JDK `corretto-22`) supplies Maven. Note the `java` on PATH is 1.8 — building from the shell needs a JDK 21+ on `JAVA_HOME`.

```
mvn -q clean compile        # compile
mvn -q exec:java -Dexec.mainClass=org.example.WebMain    # (no exec plugin configured; normally launched from the IDE)
```

There are **no tests** — no `src/test`, no test dependency in `pom.xml`. `src/main/java/org/example/Test.java` is a scratch `main` that benchmarks `SecureRandom`, not a JUnit test; don't treat it as a test target.

### Entry points

- `org.example.WebMain` — the real application. It starts without a webcam (the report buttons are disabled and a warning dialog is shown), but every report needs one to capture frames.
- `org.example.Main` — headless demo: generate a noise image, apply one Rule 30 generation, write `old.bmp` / `new.bmp` to the working directory.
- `org.example.Test` — scratch benchmark.

## Architecture

### The CA pipeline

`CellAutomataUtils` is the core. Every path funnels through `makeGeneration(boolean[], Rule)`, which applies a `Rule` to each cell using its left/right neighbours with **wraparound at both ends** (index 0 sees the last cell; the last cell sees index 0), implemented with a pattern-matching `switch` over the index.

Data conversions live in `BitsUtils`:
- `toBoolCached(byte[])` — the hot path. Uses a precomputed `boolean[256][8]` table built in the constructor (indexed `byte + 128`), so `CellAutomataUtils` holds a single shared `BitsUtils` instance. The static `toBool` variants go through `BitSet` and are slower; prefer the cached instance method inside loops.
- `toByteArray(boolean[])` — pads back to full bytes because `BitSet.toByteArray()` truncates trailing zero bytes.

The four `CellAutomataUtils` overloads differ in what they return and whether they re-pack to bytes:
- `evolveWithCABytes(byte[], generations[, Rule])` — bytes in, bytes out (used by every report).
- `evolveWithCABytesBits(...)` — returns the raw `boolean[]`, avoiding the re-pack (used by the noise-text frame).
- `evolveWithCAImage(...)` — returns a rendered `BufferedImage`.

### Rules

`Rule` is a `@FunctionalInterface` (`boolean step(left, middle, right)`). `RulesSet` holds the four implemented rules as static methods referenced as `RulesSet::rule30` etc. — **adding a rule means adding a static method there and registering it in `CARuleDropdown.RULES`**, and, for the comparison reports, adding a series by hand (each report enumerates the rules explicitly rather than iterating `RulesSet`).

Rule 30 is the implicit default: `evolveWithCABytes(byte[], int)` and `evolveWithCAImage(BufferedImage, ...)` hardcode it.

### Image bytes vs. pixel bytes

`WebcamSession.captureBmp()` (a thin wrapper over `WebcamUtils.getImageBytes(webcam, "bmp")`) returns a **full BMP file including its header**, so CA generations run over header bytes too. When a report needs actual pixel data it must skip the header via `BufferedImageUtils.getImageOffsetBmp(byte[])`, which reads the little-endian pixel-data offset at BMP bytes 10–13. `ByteChangeComparison` does this; most of the distribution reports do not. Keep this distinction in mind when adding or debugging a report.

`BufferedImageUtils.imgFromBytes` reconstructs a `TYPE_3BYTE_BGR` image with a band mapping of `{2,1,0}`, so byte order is BGR-interleaved throughout.

### Webcam selection

`org.example.webcam` owns the camera. `WebcamService.discover()` lists every device (sarxos's `WebcamDefaultDriver` picks the platform grabber, so nothing here is OS specific) and returns an empty list instead of throwing when there is none; `resolutionsFor(webcam)` merges the device's own `getViewSizes()` with a preferred list and registers the union as custom view sizes.

`WebcamSession` holds the **one** device the app captures from. Reports take the session, never a `Webcam`, and call `session.captureBmp()` at capture time — that is what makes the `WebcamDropdown` selection reach the reports; holding a `Webcam` reference would leave them capturing from a device that "Save config" has already closed. `activate()` notifies `WebcamSessionListener`s before closing the old device (so `WebcamPreview` can stop its `WebcamPanel`) and again after opening the new one.

### Statistics

`MathUtil.analyseFrame(bytes, deltaPercentage, percentageConsistent)` is the single analysis entry point. It builds a `Map<Byte,Integer>` histogram over all 256 signed byte values and returns a Lombok-built `FrameStats` carrying two independent consistency measures — `consistencyRateByItemCount` (how many histogram buckets sit within delta of the mean bucket) and `consistencyRateByItemAvgDiff` (how many bytes sit within delta of the mean byte). `FrameStats.isConsistentFrame()` requires both to clear the threshold. `averageDeviationFromIdealDist` compares each bucket against a perfectly uniform distribution and is what the histogram report prints as its subtitle.

`BytesDistributionReport` can swap the webcam frame for a generated seed: `IdealSequenceUtils.generate(length, Order)` builds a sequence holding all 256 byte values equally often, either `CYCLIC` (-128,-127,...,127, repeat) or `GROUPED` (every -128, then every -127, ...). Its `averageDeviationFromIdealDist` is 0% by construction, so whatever a rule does to it is measured against a known-flat baseline instead of a lumpy frame. Sizes come from `WebcamService.PREFERRED_RESOLUTIONS` and are `width * height * 3` bytes, all of which divide evenly by 256.

`ShuffleUtils.shuffleBits` is the non-CA baseline in every comparison report. It permutes the **bit** array, not the bytes, so only the one-bit count survives and the byte histogram changes entirely - labelled "Shuffle bits" in the charts for that reason. A near-flat histogram from it is close to automatic once the one-bit density is ~0.5, and its disorder comes from `Collections.shuffle`'s own `Random`, not from the frame.

`MathUtil.compare` returns a **similarity percentage** (fraction of positions that are equal) — in the charts, lower is better, since the goal is divergence from the input frame.

### UI layer

Plain AWT/Swing, no framework, no layout files. Everything is constructed in constructors.

- `WebMain` builds one `JFrame` with two stacked rows in `BorderLayout.NORTH` — the report buttons, then the camera controls — each using `WrapLayout` (a `FlowLayout` that reports its wrapped height; plain `FlowLayout` claims a single row, so anything that wrapped was clipped away and became unclickable). Each `MyCustomButton` constructs a report `JFrame` in its `ActionListener`. Adding a report means writing a class under `ui/reports` whose constructor takes a `WebcamSession`, shows itself, and registering one line in `WebMain`.
- Every report follows the same shape: `buildDefaultFrame()` → `buildControlPanel()` with a "Save config" button → `createDataset()` → `createChart(dataset)`. "Save config" copies the input-field values into the frame's fields and then calls `AsyncReport.load(...)` again, which replaces whatever the previous run left in the centre. Follow that pattern rather than inventing a new refresh mechanism — in particular, don't go back to streaming `getContentPane().getComponents()` for the old `ChartPanel`.
- Charting is JFreeChart; stats use `commons-math3`; `DescriptiveStatistics`; Lombok is `provided` scope (`@Data`, `@Builder`, `@Slf4j`).
- **CA work runs off the event dispatch thread.** `AsyncReport.load(frame, this::createDataset, dataset -> new ChartPanel(...))` builds the dataset on a `SwingWorker`, shows an indeterminate progress bar in the frame's centre, and swaps the chart in when it is done; `AsyncReport.run(owner, work, onDone)` is the variant for frames that feed an existing component instead of a chart. Use these rather than building a dataset inline - doing it in the constructor blocked the EDT *after* the frame was shown, leaving an unpainted white window.
- While any report runs, `ReportActivity` disables the main window's report buttons and camera controls, because a background capture would otherwise be reading from a device that "Save config" is closing. Report frames disable their own buttons for the duration too.
- Every report frame uses `DISPOSE_ON_CLOSE`; only `WebMain`'s main window uses `EXIT_ON_CLOSE`. Keep it that way — `EXIT_ON_CLOSE` on a report kills the whole app when that report is closed.

## Repo notes

`new.bmp` and `test1.bmp` at the repo root are runtime output from `ModifiedImageFrame` / `Main`, not fixtures; they are untracked and should stay that way.
