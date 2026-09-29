package org.example.ui.reports;

import org.apache.commons.lang3.StringUtils;
import org.example.ui.AsyncReport;
import org.example.webcam.WebcamSession;
import org.example.ui.NumberField;
import org.example.ui.button.MyCustomButton;
import org.example.utils.BufferedImageUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ByteChangeDistrComparison extends JFrame {

    private final WebcamSession session;

    private int selectedByteNumber = 5;

    private int waitMsBetweenFrames = 0;

    private int numberOfFrames = 50;

    private final NumberField byteNumberInput = new NumberField(10, selectedByteNumber);
    private final NumberField waitBetweenFramesInput = new NumberField(10, waitMsBetweenFrames);
    private final NumberField numberOfFramesInput = new NumberField(10, numberOfFrames);

    public ByteChangeDistrComparison(WebcamSession session) {
        this.session = session;
        buildDefaultFrame();

        var controlPanel = buildControlPanel();

        add(controlPanel, BorderLayout.NORTH);
        AsyncReport.load(this, this::createDataset, dataset -> new ChartPanel(createChart(dataset)));
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

            AsyncReport.load(this, this::createDataset, dataset -> new ChartPanel(createChart(dataset)));
        }));
        return controlPanel;
    }

    private JFreeChart createChart(DefaultCategoryDataset dataset) {
        JFreeChart lineChart = ChartFactory.createBarChart(
                "Byte Change Distribution",
                "Deltas",
                "Percentage of repeating",
                dataset);

        CategoryPlot plot = (CategoryPlot) lineChart.getPlot();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        double minDatasetValue = findMinValue(dataset);
        double maxDatasetValue = findMaxValue(dataset);
        rangeAxis.setRange(minDatasetValue - 2, maxDatasetValue + 2);

        if ((maxDatasetValue - minDatasetValue) < 20) {
            rangeAxis.setAutoTickUnitSelection(false);
            rangeAxis.setTickUnit(new NumberTickUnit(1));
        }

        return lineChart;
    }

    public DefaultCategoryDataset createDataset() {
        var dataset = new DefaultCategoryDataset();

        List<Integer> deltas = new ArrayList<>();

        int prevValue = BufferedImageUtils.getSelectedByteFromImage(session.captureBmp(), selectedByteNumber);

        for (int i = 1; i < numberOfFrames; i++) {
            byte currentVal = BufferedImageUtils.getSelectedByteFromImage(session.captureBmp(), selectedByteNumber);

            deltas.add((currentVal - prevValue));

            prevValue = currentVal;

            if (waitMsBetweenFrames > 0) {
                try {
                    Thread.sleep(waitMsBetweenFrames);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }

        deltas.stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                .entrySet().stream().peek(e -> e.setValue((long) (100 * ((double) e.getValue() / numberOfFrames))))// transform count values to percentages
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new))
                .forEach((key, value) -> dataset.addValue(value, "Deltas value", key));

        return dataset;
    }

    private void buildDefaultFrame() {
        setTitle("Byte Change Distribution");
        setLayout(new BorderLayout());
        setSize(1600, 900);

        setVisible(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
