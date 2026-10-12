package org.example.ui.reports;

import org.apache.commons.lang3.StringUtils;
import org.example.math.MathUtil;
import org.example.math.model.FrameStats;
import org.example.rule.RulesSet;
import org.example.ui.AsyncReport;
import org.example.ui.WrapLayout;
import org.example.ui.dropdown.SizeDropdown;
import org.example.utils.CellAutomataUtils;
import org.example.utils.GaussSequenceUtils;
import org.example.utils.IdealSequenceUtils;
import org.example.utils.ShuffleUtils;
import org.example.webcam.WebcamService;
import org.example.webcam.WebcamSession;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BytesDistributionReport extends JFrame {

    private static final String title = "Bytes Distribution Report";
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    private final static String AVG_DEVIATION_TEXT_FORMAT = "%s deviation: %.2f%%";

    /**
     * Which histogram the generated seed has. Both are built from exact counts, so neither carries
     * sampling noise.
     */
    private enum IdealSeed {

        EQUAL("Ideal Equal"),
        GAUSS("Ideal Gauss");

        private final String label;

        IdealSeed(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private int generationsState = 1;

    private final JTextField generations = new JTextField(10);

    private boolean includeSecureRandomState = true;
    private boolean includeWebcamRawState = true;
    private boolean includeRule30State = true;
    private boolean includeRule90State = true;
    private boolean includeRule105State = true;
    private boolean includeRule150State = true;
    private boolean includeShuffleState = true;

    private final Checkbox includeSecureRandom = new Checkbox("Include Secure Random", true);
    private final Checkbox includeWebcam = new Checkbox("Include raw seed", true);
    private final Checkbox includeRule30 = new Checkbox("Include Rule 30", true);
    private final Checkbox includeRule90 = new Checkbox("Include Rule 90", true);
    private final Checkbox includeRule105 = new Checkbox("Include Rule 105", true);
    private final Checkbox includeRule150 = new Checkbox("Include Rule 150", true);
    private final Checkbox includeShuffle = new Checkbox("Include Shuffle bits", true);

    /**
     * Ticking clears every series toggle, unticking turns them all back on. It follows the toggles
     * too, so it is ticked exactly when no series is selected.
     */
    private final Checkbox unselectAll = new Checkbox("Unselect all", false);

    private boolean useIdealSeedState = false;
    private IdealSeed idealSeedState = IdealSeed.EQUAL;
    private double gaussMeanState = GaussSequenceUtils.DEFAULT_MEAN;
    private double gaussSigmaState = GaussSequenceUtils.DEFAULT_SIGMA;
    private IdealSequenceUtils.Order idealOrderState = IdealSequenceUtils.Order.CYCLIC;
    private Dimension idealSizeState = WebcamService.DEFAULT_RESOLUTION;

    // a JCheckBox rather than the AWT Checkbox its neighbours use, so it can carry a tooltip
    private final JCheckBox useIdealSeed = new JCheckBox("Use ideal sequence as a seed", false);
    private final JComboBox<IdealSeed> idealSeed = new JComboBox<>(IdealSeed.values());
    private final JTextField gaussMean = new JTextField(String.valueOf(GaussSequenceUtils.DEFAULT_MEAN), 5);
    private final JTextField gaussSigma = new JTextField(String.valueOf(GaussSequenceUtils.DEFAULT_SIGMA), 5);
    private final JComboBox<IdealSequenceUtils.Order> idealOrder =
            new JComboBox<>(IdealSequenceUtils.Order.values());
    private final SizeDropdown idealSize = new SizeDropdown(WebcamService.DEFAULT_RESOLUTION);

    /**
     * Order and size only mean anything for a generated seed, so they are shown only then.
     */
    private final JPanel idealOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

    /**
     * Mean and sigma only shape the gauss sequence.
     */
    private final JPanel gaussOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

    private Map<String, Double> ruleDeviationMap = new HashMap<>();

    private final WebcamSession session;

    public BytesDistributionReport(WebcamSession session) {
        this.session = session;
        buildDefaultFrame();

        var controlPanel = buildControlPanel();

        add(controlPanel, BorderLayout.NORTH);

        AsyncReport.load(this, this::createDataset, dataset -> new ChartPanel(buildChart(dataset)));
    }

    private JPanel buildControlPanel() {
        // two rows: the series toggles, then the seed controls underneath
        JPanel seriesRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 15));
        JPanel seedRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.add(seriesRow);
        controlPanel.add(seedRow);

        Button saveConfig = new Button("Save config");
        saveConfig.addActionListener(e -> {
            this.includeSecureRandomState = includeSecureRandom.getState();
            this.includeWebcamRawState = includeWebcam.getState();
            this.includeRule30State = includeRule30.getState();
            this.includeRule90State = includeRule90.getState();
            this.includeRule105State = includeRule105.getState();
            this.includeRule150State = includeRule150.getState();
            this.includeShuffleState = includeShuffle.getState();

            this.generationsState = StringUtils.isNoneEmpty(generations.getText())
                    ? Integer.parseInt(generations.getText())
                    : generationsState;

            this.useIdealSeedState = useIdealSeed.isSelected();
            this.idealSeedState = (IdealSeed) idealSeed.getSelectedItem();
            this.gaussMeanState = parseOr(gaussMean, gaussMeanState);
            double sigma = parseOr(gaussSigma, gaussSigmaState);
            this.gaussSigmaState = sigma > 0 ? sigma : gaussSigmaState;
            this.idealOrderState = (IdealSequenceUtils.Order) idealOrder.getSelectedItem();
            this.idealSizeState = idealSize.getSelectedResolution();

            AsyncReport.load(this, this::createDataset, dataset -> new ChartPanel(buildChart(dataset)));
        });

        seriesRow.add(new JLabel("Generations:"));
        seriesRow.add(generations);

        List<Checkbox> seriesToggles = List.of(includeSecureRandom, includeWebcam, includeRule30,
                includeRule90, includeRule105, includeRule150, includeShuffle);

        unselectAll.addItemListener(e -> seriesToggles.forEach(toggle -> toggle.setState(!unselectAll.getState())));
        // setState does not fire item events, so this only reacts to the user's own clicks
        seriesToggles.forEach(toggle -> toggle.addItemListener(e ->
                unselectAll.setState(seriesToggles.stream().noneMatch(Checkbox::getState))));

        seriesRow.add(unselectAll);
        seriesRow.add(includeSecureRandom);
        seriesRow.add(includeWebcam);
        seriesRow.add(includeRule30);
        seriesRow.add(includeRule90);
        seriesRow.add(includeRule105);
        seriesRow.add(includeRule150);
        seriesRow.add(includeShuffle);

        useIdealSeed.setToolTipText("<html>Build the seed instead of capturing it.<br>"
                + "Its histogram is known exactly before any rule is applied,<br>"
                + "so each rule is measured against a fixed baseline.</html>");

        idealSeed.setToolTipText("<html>The histogram of the generated frame.<br>"
                + "<b>ideal-equal</b> holds every byte value from -128 to 127 the same number of times -<br>"
                + "a perfectly flat histogram, the best case a randomness extractor can be handed.<br>"
                + "<b>ideal gauss</b> follows a normal curve with the given mean and sigma - a known<br>"
                + "skewed input, showing how much a rule actually flattens a distribution.<br>"
                + "Its counts are computed from the curve, not sampled, so it is the same every run.</html>");

        gaussMean.setToolTipText("Centre of the curve, as a signed byte value (-128..127).");
        gaussSigma.setToolTipText("<html>Standard deviation, in byte values; must be positive.<br>"
                + "The curve is truncated to -128..127 rather than clamped,<br>"
                + "so wide curves do not pile up on the end values.</html>");

        idealOrder.setToolTipText("<html>How the equal byte counts are laid out.<br>"
                + "<b>Cyclic</b> walks -128,-127,...,127 and repeats.<br>"
                + "<b>Grouped</b> puts every -128 first, then every -127, and so on.<br>"
                + "Both hold identical counts; only the local structure a rule sees differs.</html>");

        idealSize.setToolTipText("<html>Size of the generated frame, using the same resolutions as the camera.<br>"
                + "The sequence is width x height x 3 bytes, matching a BGR image at that size.<br>"
                + "Every option here divides evenly by 256, so ideal-equal counts come out exactly equal.</html>");

        gaussOptions.add(new JLabel("Mean:"));
        gaussOptions.add(gaussMean);
        gaussOptions.add(new JLabel("Sigma:"));
        gaussOptions.add(gaussSigma);
        gaussOptions.setVisible(idealSeed.getSelectedItem() == IdealSeed.GAUSS);

        idealSeed.addItemListener(e -> {
            gaussOptions.setVisible(idealSeed.getSelectedItem() == IdealSeed.GAUSS);
            controlPanel.revalidate();
            controlPanel.repaint();
        });

        idealOptions.add(new JLabel("Sequence:"));
        idealOptions.add(idealSeed);
        idealOptions.add(gaussOptions);
        idealOptions.add(new JLabel("Order:"));
        idealOptions.add(idealOrder);
        idealOptions.add(new JLabel("Size:"));
        idealOptions.add(idealSize);
        idealOptions.setVisible(useIdealSeed.isSelected());

        useIdealSeed.addItemListener(e -> {
            idealOptions.setVisible(useIdealSeed.isSelected());
            controlPanel.revalidate();
            controlPanel.repaint();
        });

        seedRow.add(useIdealSeed);
        seedRow.add(idealOptions);

        seedRow.add(saveConfig);

        return controlPanel;
    }

    private JFreeChart buildChart(XYSeriesCollection dataset) {
        // Create the histogram chart
        JFreeChart chart = ChartFactory.createXYBarChart(
                chartTitle(),
                "Byte Value",
                false,
                "Percentage",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        // Customize chart (optional)
        XYPlot plot = (XYPlot) chart.getPlot();

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setTickUnit(new NumberTickUnit(5)); // Set tick unit for y-axis
        domainAxis.setRange(Byte.MIN_VALUE, Byte.MAX_VALUE); // Adjust range as needed based on your data

        ValueMarker marker = new ValueMarker((double) 100 / 256);

        // Set the font to bold
        Font boldFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        marker.setLabelFont(boldFont);
        marker.setLabelTextAnchor(TextAnchor.TOP_CENTER);

        // position is the value on the axis
        marker.setPaint(Color.BLUE);
        marker.setLabel("AVG"); // see JavaDoc for labels, colors, strokes

        plot.addRangeMarker(marker);

        String subtitle = ruleDeviationMap.entrySet().stream().map(entry ->
                AVG_DEVIATION_TEXT_FORMAT.formatted(entry.getKey(), entry.getValue())
        ).collect(Collectors.joining(", "));

        chart.addSubtitle(new TextTitle(subtitle));

        return chart;
    }

    private String chartTitle() {
        if (!useIdealSeedState) {
            return "Byte Percentage Histogram (webcam frame)";
        }

        return "Byte Percentage Histogram (generated frame: %s, %s)".formatted(
                seedName(), WebcamService.describe(idealSizeState));
    }

    private XYSeriesCollection createDataset() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        if (!ruleDeviationMap.isEmpty()) {
            ruleDeviationMap.clear();
        }

        byte[] imageBytes = buildSeed();

        if (includeSecureRandomState) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] secureRandomBytes = new byte[921654];
            secureRandom.nextBytes(secureRandomBytes);

            XYSeries series = new XYSeries("Secure Random");

            FrameStats frameStats = MathUtil.analyseFrame(secureRandomBytes, 30, 70);
            frameStats.getByteCountMap().forEach((aByte, count) -> series.add((double) aByte, 100.0d * count / secureRandomBytes.length));

            ruleDeviationMap.put("Secure Random", frameStats.getAverageDeviationFromIdealDist());
            dataset.addSeries(series);
        }

        if (includeWebcamRawState) {
            String seedName = seedName();
            XYSeries series = new XYSeries(seedName);

            FrameStats frameStats = MathUtil.analyseFrame(imageBytes, 30, 70);
            frameStats.getByteCountMap().forEach((aByte, count) -> series.add((double) aByte, 100.0d * count / imageBytes.length));

            ruleDeviationMap.put(seedName, frameStats.getAverageDeviationFromIdealDist());
            dataset.addSeries(series);
        }

        if (includeRule30State) {
            byte[] imageCABytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generationsState);

            XYSeries series = new XYSeries("CA30");

            FrameStats frameStats = MathUtil.analyseFrame(imageCABytes, 30, 70);
            frameStats.getByteCountMap().forEach((aByte, count) -> series.add((double) aByte, 100.0d * count / imageCABytes.length));

            ruleDeviationMap.put("CA30", frameStats.getAverageDeviationFromIdealDist());
            dataset.addSeries(series);
        }

        if (includeRule90State) {
            byte[] imageCA90Bytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generationsState, RulesSet::rule90);

            XYSeries series = new XYSeries("CA90");

            FrameStats frameStats = MathUtil.analyseFrame(imageCA90Bytes, 30, 70);
            frameStats.getByteCountMap().forEach((aByte, count) -> series.add((double) aByte, 100.0d * count / imageCA90Bytes.length));

            ruleDeviationMap.put("CA90", frameStats.getAverageDeviationFromIdealDist());
            dataset.addSeries(series);
        }

        if (includeRule105State) {
            byte[] imageCA105Bytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generationsState, RulesSet::rule105);

            XYSeries series = new XYSeries("CA105");

            FrameStats frameStats = MathUtil.analyseFrame(imageCA105Bytes, 30, 70);
            frameStats.getByteCountMap().forEach((aByte, count) -> series.add((double) aByte, 100.0d * count / imageCA105Bytes.length));

            ruleDeviationMap.put("CA105", frameStats.getAverageDeviationFromIdealDist());
            dataset.addSeries(series);
        }

        if (includeRule150State) {
            byte[] imageCA150Bytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generationsState, RulesSet::rule150);

            XYSeries series = new XYSeries("CA150");

            FrameStats frameStats = MathUtil.analyseFrame(imageCA150Bytes, 30, 70);
            frameStats.getByteCountMap().forEach((aByte, count) -> series.add((double) aByte, 100.0d * count / imageCA150Bytes.length));

            ruleDeviationMap.put("CA150", frameStats.getAverageDeviationFromIdealDist());
            dataset.addSeries(series);
        }

        if (includeShuffleState) {
            byte[] shuffledBytes = ShuffleUtils.shuffleBits(imageBytes, generationsState);

            XYSeries series = new XYSeries("Shuffle bits");

            FrameStats frameStats = MathUtil.analyseFrame(shuffledBytes, 30, 70);
            frameStats.getByteCountMap().forEach((aByte, count) -> series.add((double) aByte, 100.0d * count / shuffledBytes.length));

            ruleDeviationMap.put("Shuffle bits", frameStats.getAverageDeviationFromIdealDist());
            dataset.addSeries(series);
        }

        return dataset;
    }

    /**
     * The bytes every series is derived from: either a live frame, or a generated sequence with a
     * known histogram - flat (ideal-equal) or a normal curve (ideal gauss).
     */
    private byte[] buildSeed() {
        if (!useIdealSeedState) {
            return session.captureBmp();
        }

        int length = IdealSequenceUtils.byteCount(idealSizeState);

        return switch (idealSeedState) {
            case EQUAL -> IdealSequenceUtils.generate(length, idealOrderState);
            case GAUSS -> GaussSequenceUtils.generate(length, gaussMeanState, gaussSigmaState, idealOrderState);
        };
    }

    private String seedName() {
        if (!useIdealSeedState) {
            return "Webcam Raw";
        }

        String order = idealOrderState == IdealSequenceUtils.Order.CYCLIC ? "cyclic" : "grouped";

        return switch (idealSeedState) {
            case EQUAL -> "Ideal Equal %s".formatted(order);
            case GAUSS -> "Ideal Gauss %s (mean %s, sigma %s)".formatted(order, gaussMeanState, gaussSigmaState);
        };
    }

    /**
     * The field's number, or {@code fallback} when it is empty or not a number.
     */
    private static double parseOr(JTextField field, double fallback) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void buildDefaultFrame() {
        setTitle(title);
        setLayout(new BorderLayout());
        setSize(WIDTH, HEIGHT);
        setVisible(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
