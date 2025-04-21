package org.example.ui.reports;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.ui.NumberField;
import org.example.ui.button.MyCustomButton;
import org.example.utils.BufferedImageUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class ByteChangeComparison extends JFrame {

    private final Webcam webcam;

    private int selectedByteNumber = 5;

    private int waitMsBetweenFrames = 0;

    private int numberOfFrames = 50;

    private final NumberField byteNumberInput = new NumberField(10, selectedByteNumber);
    private final NumberField waitBetweenFramesInput = new NumberField(10, waitMsBetweenFrames);
    private final NumberField numberOfFramesInput = new NumberField(10, numberOfFrames);

    public ByteChangeComparison(Webcam webcam) {
        this.webcam = webcam;
        buildDefaultFrame();

        var controlPanel = buildControlPanel();

        var dataset = createDataset();
        var chart = createChart(dataset);

        add(controlPanel, BorderLayout.NORTH);
        add(new ChartPanel(chart), BorderLayout.CENTER);
    }

    private JPanel buildControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));

        controlPanel.add(new JLabel("Number of byte: "));
        controlPanel.add(byteNumberInput);
        controlPanel.add(new JLabel("Wait between frames: "));
        controlPanel.add(waitBetweenFramesInput);
        controlPanel.add(new JLabel("Number of frames: "));
        controlPanel.add(numberOfFramesInput);
        controlPanel.add(new MyCustomButton("Save Config", e -> {
            selectedByteNumber = StringUtils.isNoneEmpty(byteNumberInput.getText()) ? Integer.parseInt(byteNumberInput.getText()) : selectedByteNumber;
            waitMsBetweenFrames = StringUtils.isNoneEmpty(waitBetweenFramesInput.getText()) ? Integer.parseInt(waitBetweenFramesInput.getText()) : waitMsBetweenFrames;
            numberOfFrames = StringUtils.isNoneEmpty(numberOfFramesInput.getText()) ? Integer.parseInt(numberOfFramesInput.getText()) : numberOfFrames;

            var currentChart = Arrays.stream(this.getContentPane().getComponents())
                    .filter(t -> t instanceof ChartPanel)
                    .map(t -> (ChartPanel) t)
                    .findFirst()
                    .orElse(null);

            this.remove(currentChart);
            this.add(new ChartPanel(createChart(createDataset())));
            this.revalidate();
            this.repaint();
        }));
        return controlPanel;
    }

    private JFreeChart createChart(DefaultCategoryDataset dataset) {
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Byte Change Comparison",
                "FRAME NUMBER",
                "VALUE",
                dataset);

        CategoryPlot plot = (CategoryPlot) lineChart.getPlot();
        plot.setRenderer(new LineAndShapeRenderer());

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        double minDatasetValue = findMinValue(dataset);
        double maxDatasetValue = findMaxValue(dataset);
        rangeAxis.setRange(minDatasetValue - 2, maxDatasetValue + 2);

        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();

        renderer.setDefaultStroke(new BasicStroke(3.0f));
        renderer.setAutoPopulateSeriesStroke(false);

        if ((maxDatasetValue - minDatasetValue) < 20) {
            rangeAxis.setAutoTickUnitSelection(false);
            rangeAxis.setTickUnit(new NumberTickUnit(1));
        }

        return lineChart;
    }

    public DefaultCategoryDataset createDataset() {
        var dataset = new DefaultCategoryDataset();

        for (int i = 1; i <= numberOfFrames; i++) {
            byte[] imageBytes = WebcamUtils.getImageBytes(this.webcam, "bmp");

            var startIndex = BufferedImageUtils.getImageOffsetBmp(imageBytes);

            String groupName = i == 1 ? "Original" : String.valueOf(i);
            dataset.addValue(imageBytes[startIndex + selectedByteNumber], "Frame", String.valueOf(i));

            if (waitMsBetweenFrames > 0) {
                try {
                    Thread.sleep(waitMsBetweenFrames);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }

        return dataset;
    }

    private void buildDefaultFrame() {
        setTitle("Test");
        setLayout(new BorderLayout());
        setSize(1600, 900);

        setVisible(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static double findMinValue(DefaultCategoryDataset dataset) {
        double minValue = Double.POSITIVE_INFINITY;

        int rowCount = dataset.getRowCount();
        int columnCount = dataset.getColumnCount();

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                Number value = dataset.getValue(i, j);

                if (value != null) {
                    double doubleValue = value.doubleValue();

                    if (doubleValue < minValue) {
                        minValue = doubleValue;
                    }
                }
            }
        }


        if (minValue == Double.POSITIVE_INFINITY) {
            return Double.NaN;
        }

        return minValue;
    }

    public static double findMaxValue(DefaultCategoryDataset dataset) {
        double maxValue = Double.NEGATIVE_INFINITY;

        int rowCount = dataset.getRowCount();
        int columnCount = dataset.getColumnCount();

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                Number value = dataset.getValue(i, j);

                if (value != null) {
                    double doubleValue = value.doubleValue();

                    if (doubleValue > maxValue) {
                        maxValue = doubleValue;
                    }
                }
            }
        }

        if (maxValue == Double.NEGATIVE_INFINITY) {
            return Double.NaN;
        }

        return maxValue;
    }
}
