package org.example.ui.reports;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import org.example.math.MathUtil;
import org.example.math.model.FrameStats;
import org.example.rule.RulesSet;
import org.example.utils.CellAutomataUtils;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BytesDistributionReport extends JFrame {

    private static final String title = "Bytes Distribution Report";
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    private final static String AVG_DEVIATION_TEXT_FORMAT = "%s deviation: %.2f%%";

    private int generationsState = 1;

    private final JTextField generations = new JTextField(10);

    private boolean includeSecureRandomState = true;
    private boolean includeWebcamRawState = true;
    private boolean includeRule30State = true;
    private boolean includeRule90State = true;
    private boolean includeRule105State = true;
    private boolean includeRule150State = true;

    private final Checkbox includeSecureRandom = new Checkbox("Include Secure Random", true);
    private final Checkbox includeWebcam = new Checkbox("Include webcam", true);
    private final Checkbox includeRule30 = new Checkbox("Include Rule 30", true);
    private final Checkbox includeRule90 = new Checkbox("Include Rule 90", true);
    private final Checkbox includeRule105 = new Checkbox("Include Rule 105", true);
    private final Checkbox includeRule150 = new Checkbox("Include Rule 150", true);

    private Map<String, Double> ruleDeviationMap = new HashMap<>();

    private final Webcam webcam;

    public BytesDistributionReport(Webcam webcam) {
        this.webcam = webcam;
        buildDefaultFrame();

        var controlPanel = buildControlPanel();

        add(controlPanel, BorderLayout.NORTH);

        var dataset = createDataset();
        JFreeChart chart = buildChart(dataset);

        add(new ChartPanel(chart), BorderLayout.CENTER);
    }

    private JPanel buildControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));

        Button saveConfig = new Button("Save config");
        saveConfig.addActionListener(e -> {
            this.includeSecureRandomState = includeSecureRandom.getState();
            this.includeWebcamRawState = includeWebcam.getState();
            this.includeRule30State = includeRule30.getState();
            this.includeRule90State = includeRule90.getState();
            this.includeRule105State = includeRule105.getState();
            this.includeRule150State = includeRule150.getState();

            this.generationsState = Integer.parseInt(generations.getText());

            var currentChart = Arrays.stream(this.getContentPane().getComponents())
                    .filter(t -> t instanceof ChartPanel)
                    .map(t -> (ChartPanel) t)
                    .findFirst()
                    .orElse(null);

            this.remove(currentChart);
            this.add(new ChartPanel(buildChart(createDataset())));
            this.revalidate();
            this.repaint();
        });

        controlPanel.add(new JLabel("Generations:"));
        controlPanel.add(generations);

        controlPanel.add(includeSecureRandom);
        controlPanel.add(includeWebcam);
        controlPanel.add(includeRule30);
        controlPanel.add(includeRule90);
        controlPanel.add(includeRule105);
        controlPanel.add(includeRule150);
        controlPanel.add(saveConfig);

        return controlPanel;
    }

    private JFreeChart buildChart(XYSeriesCollection dataset) {
        // Create the histogram chart
        JFreeChart chart = ChartFactory.createXYBarChart(
                "Byte Percentage Histogram",
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

    private XYSeriesCollection createDataset() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        if (!ruleDeviationMap.isEmpty()) {
            ruleDeviationMap.clear();
        }

        byte[] imageBytes = WebcamUtils.getImageBytes(webcam, "bmp");

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
            XYSeries series = new XYSeries("Webcam Raw");

            FrameStats frameStats = MathUtil.analyseFrame(imageBytes, 30, 70);
            frameStats.getByteCountMap().forEach((aByte, count) -> series.add((double) aByte, 100.0d * count / imageBytes.length));

            ruleDeviationMap.put("Webcam Raw", frameStats.getAverageDeviationFromIdealDist());
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

        return dataset;
    }

    private void buildDefaultFrame() {
        setTitle(title);
        setLayout(new BorderLayout());
        setSize(WIDTH, HEIGHT);
        setVisible(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
