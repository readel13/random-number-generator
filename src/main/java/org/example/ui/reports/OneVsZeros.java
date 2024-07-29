package org.example.ui.reports;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import org.example.rule.RulesSet;
import org.example.utils.BitsUtils;
import org.example.utils.CellAutomataUtils;
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

    private final Webcam webcam;

    public OneVsZeros(Webcam webcam) {
        this.webcam = webcam;
        buildDefaultFrame();

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));

        Checkbox includeSecureRandom = new Checkbox("Include Secure Random", true);
        Checkbox includeWebcam = new Checkbox("Include webcam", true);
        Checkbox includeRule30 = new Checkbox("Include Rule 30", true);
        Checkbox includeRule90 = new Checkbox("Include Rule 90", true);
        Checkbox includeRule105 = new Checkbox("Include Rule 105", true);
        Checkbox includeRule150 = new Checkbox("Include Rule 150", true);

        JTextField generations = new JTextField(10);

        Button saveConfig = new Button("Save config");
        saveConfig.addActionListener(e -> {
            this.includeSecureRandom = includeSecureRandom.getState();
            this.includeWebcamRaw = includeWebcam.getState();
            this.includeRule30 = includeRule30.getState();
            this.includeRule90 = includeRule90.getState();
            this.includeRule105 = includeRule105.getState();
            this.includeRule150 = includeRule150.getState();

            this.generations = Integer.parseInt(generations.getText());

            var currentChart = Arrays.stream(this.getContentPane().getComponents())
                    .filter(t -> t instanceof ChartPanel)
                    .map(t -> (ChartPanel) t)
                    .findFirst()
                    .orElse(null);

            this.remove(currentChart);
            this.add(new ChartPanel(buildHistogramChart(createDataset())));
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

        add(controlPanel, BorderLayout.NORTH);

        var dataset = createDataset();
        JFreeChart chart = buildHistogramChart(dataset);

        add(new ChartPanel(chart), BorderLayout.CENTER);
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
        renderer.setSeriesPaint(0, Color.BLUE); // Male bars
        renderer.setSeriesPaint(1, Color.RED); // Female bars
        renderer.setItemMargin(-0.7);

        return chart;
    }


    private DefaultCategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        byte[] imageBytes = WebcamUtils.getImageBytes(webcam, "bmp");

        if (includeSecureRandom) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] secureRandomBytes = new byte[imageBytes.length];
            secureRandom.nextBytes(secureRandomBytes);

            int countZeros = countZeros(secureRandomBytes);
            int countOnes = secureRandomBytes.length - countZeros;

            dataset.addValue(countZeros, "Zero", "SecureRandom");
            dataset.addValue(countOnes, "One", "SecureRandom");
        }

        if (includeWebcamRaw) {
            int countZeros = countZeros(imageBytes);
            int countOnes = imageBytes.length - countZeros;

            dataset.addValue(countZeros, "Zero", "WebcamRaw");
            dataset.addValue(countOnes, "One", "WebcamRaw");
        }

        if (includeRule30) {
            byte[] imageCABytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generations);
            int countZeros = countZeros(imageCABytes);
            int countOnes = imageCABytes.length - countZeros;

            dataset.addValue(countZeros, "Zero", "CA30");
            dataset.addValue(countOnes, "One", "CA30");
        }

        if (includeRule90) {
            byte[] imageCA90Bytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generations, RulesSet::rule90);
            int countZeros = countZeros(imageCA90Bytes);
            int countOnes = imageCA90Bytes.length - countZeros;

            dataset.addValue(countZeros, "Zero", "CA90");
            dataset.addValue(countOnes, "One", "CA90");
        }

        if (includeRule105) {
            byte[] imageCA105Bytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generations, RulesSet::rule105);
            int countZeros = countZeros(imageCA105Bytes);
            int countOnes = imageCA105Bytes.length - countZeros;

            dataset.addValue(countZeros, "Zero", "CA105");
            dataset.addValue(countOnes, "One", "CA105");
        }

        if (includeRule150) {
            byte[] imageCA150Bytes = CellAutomataUtils.evolveWithCABytes(imageBytes, generations, RulesSet::rule150);
            int countZeros = countZeros(imageCA150Bytes);
            int countOnes = imageCA150Bytes.length - countZeros;

            dataset.addValue(countZeros, "Zero", "CA150");
            dataset.addValue(countOnes, "One", "CA150");
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
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
