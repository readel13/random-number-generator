package org.example.ui.reports;

import org.example.ui.AsyncReport;
import org.example.webcam.WebcamSession;
import org.example.rule.RulesSet;
import org.example.utils.BitsUtils;
import org.example.utils.CellAutomataUtils;
import org.example.utils.ShuffleUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.security.SecureRandom;
import java.util.Arrays;

public class OneVsZeros extends JFrame {

    private static final String title = "One vs Zeros";
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    private int generations = 10;

    private boolean includeSecureRandom = true;
    private boolean includeWebcamRaw = true;
    private boolean includeRule30 = true;
    private boolean includeRule90 = true;
    private boolean includeRule105 = true;
    private boolean includeRule150 = true;
    private boolean includeShuffle = true;

    private final WebcamSession session;

    public OneVsZeros(WebcamSession session) {
        this.session = session;
        buildDefaultFrame();

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));

        Checkbox includeSecureRandom = new Checkbox("Include Secure Random", true);
        Checkbox includeWebcam = new Checkbox("Include webcam", true);
        Checkbox includeRule30 = new Checkbox("Include Rule 30", true);
        Checkbox includeRule90 = new Checkbox("Include Rule 90", true);
        Checkbox includeRule105 = new Checkbox("Include Rule 105", true);
        Checkbox includeRule150 = new Checkbox("Include Rule 150", true);
        Checkbox includeShuffle = new Checkbox("Include Shuffle bits", true);

        JTextField generations = new JTextField(10);

        Button saveConfig = new Button("Save config");
        saveConfig.addActionListener(e -> {
            this.includeSecureRandom = includeSecureRandom.getState();
            this.includeWebcamRaw = includeWebcam.getState();
            this.includeRule30 = includeRule30.getState();
            this.includeRule90 = includeRule90.getState();
            this.includeRule105 = includeRule105.getState();
            this.includeRule150 = includeRule150.getState();
            this.includeShuffle = includeShuffle.getState();

            this.generations = Integer.parseInt(generations.getText());

            AsyncReport.load(this, this::createDataset, dataset -> new ChartPanel(buildHistogramChart(dataset)));
        });

        controlPanel.add(new JLabel("Generations:"));
        controlPanel.add(generations);

        controlPanel.add(includeSecureRandom);
        controlPanel.add(includeWebcam);
        controlPanel.add(includeRule30);
        controlPanel.add(includeRule90);
        controlPanel.add(includeRule105);
        controlPanel.add(includeRule150);
        controlPanel.add(includeShuffle);
        controlPanel.add(saveConfig);

        add(controlPanel, BorderLayout.NORTH);

        AsyncReport.load(this, this::createDataset, dataset -> new ChartPanel(buildHistogramChart(dataset)));
    }

    private static JFreeChart buildHistogramChart(DefaultCategoryDataset dataset) {
        // Create the histogram chart
        JFreeChart chart = ChartFactory.createBarChart(
                "Zero vs Ones ",
                "Rules",
                "Amount",
                dataset,
                PlotOrientation.HORIZONTAL,
                true,
                true,
                false
        );

        CategoryPlot plot = chart.getCategoryPlot();

        // Customize category axis
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.STANDARD);

        // Customize renderer
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesPaint(1, Color.RED);
        renderer.setItemMargin(-0.7);

        return chart;
    }


    private DefaultCategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        byte[] imageBytes = session.captureBmp();

        if (includeSecureRandom) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] secureRandomBytes = new byte[imageBytes.length];
            secureRandom.nextBytes(secureRandomBytes);

            long bitSize = 8L * secureRandomBytes.length;
            int zerosPercentage = (int) (((double) countZeros(secureRandomBytes) / bitSize) * 100);
            int onesPercentage = -1 * (100 - zerosPercentage);


            dataset.addValue(zerosPercentage, "Zero", "SecureRandom");
            dataset.addValue(onesPercentage, "One", "SecureRandom");
        }

        if (includeWebcamRaw) {
            long bitSize = 8L * imageBytes.length;
            int zerosPercentage = (int) (((double) countZeros(imageBytes) / bitSize) * 100);
            int onesPercentage = -1 * (100 - zerosPercentage);

            dataset.addValue(zerosPercentage, "Zero", "WebcamRaw");
            dataset.addValue(onesPercentage, "One", "WebcamRaw");
        }

        if (includeRule30) {
            byte[] imageCABytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generations);

            long bitSize = 8L * imageBytes.length;
            int zerosPercentage = (int) (((double) countZeros(imageCABytes) / bitSize) * 100);
            int onesPercentage = -1 * (100 - zerosPercentage);

            dataset.addValue(zerosPercentage, "Zero", "CA30");
            dataset.addValue(onesPercentage, "One", "CA30");
        }

        if (includeRule90) {
            byte[] imageCA90Bytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generations, RulesSet::rule90);
            long bitSize = 8L * imageCA90Bytes.length;
            int zerosPercentage = (int) (((double) countZeros(imageCA90Bytes) / bitSize) * 100);
            int onesPercentage = -1 * (100 - zerosPercentage);

            dataset.addValue(zerosPercentage, "Zero", "CA90");
            dataset.addValue(onesPercentage, "One", "CA90");
        }

        if (includeRule105) {
            byte[] imageCA105Bytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generations, RulesSet::rule105);
            long bitSize = 8L * imageCA105Bytes.length;
            int zerosPercentage = (int) (((double) countZeros(imageCA105Bytes) / bitSize) * 100);
            int onesPercentage = -1 * (100 - zerosPercentage);

            dataset.addValue(zerosPercentage, "Zero", "CA105");
            dataset.addValue(onesPercentage, "One", "CA105");
        }

        if (includeRule150) {
            byte[] imageCA150Bytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generations, RulesSet::rule150);
            long bitSize = 8L * imageCA150Bytes.length;
            int zerosPercentage = (int) (((double) countZeros(imageCA150Bytes) / bitSize) * 100);
            int onesPercentage = -1 * (100 - zerosPercentage);

            dataset.addValue(zerosPercentage, "Zero", "CA150");
            dataset.addValue(onesPercentage, "One", "CA150");
        }

        if (includeShuffle) {
            byte[] shuffledBytes = ShuffleUtils.shuffleBits(imageBytes, generations);
            long bitSize = 8L * shuffledBytes.length;
            int zerosPercentage = (int) (((double) countZeros(shuffledBytes) / bitSize) * 100);
            int onesPercentage = -1 * (100 - zerosPercentage);

            dataset.addValue(zerosPercentage, "Zero", "Shuffle bits");
            dataset.addValue(onesPercentage, "One", "Shuffle bits");
        }

        return dataset;
    }

    private int countZeros(byte[] secureRandomBytes) {
        boolean[] bools = BitsUtils.toBool(secureRandomBytes);

        int countZeros = 0;

        for (var b : bools) {
            if (b) {
                countZeros++;
            }
        }

        return countZeros;
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
