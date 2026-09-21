package org.example.ui.reports;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.example.rule.RulesSet;
import org.example.utils.CellAutomataUtils;
import org.example.utils.MeasureTimeUtil;
import org.example.utils.ShuffleUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpeedRuleChart extends JFrame {

    private final Webcam webcam;

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    private int iterations = 10;
    private int generations = 1;
    private boolean includeTakingPhoto = false;

    private final JLabel genLabel = new JLabel("Generations:");
    private final JTextField genInput = new JTextField(10);
    private final JLabel iterLabel = new JLabel("Iterations:");
    private final JTextField iterInput = new JTextField(10);
    private final Checkbox includeTakePhotoCheckBox = new Checkbox("Include taking a photo", includeTakingPhoto);

    public SpeedRuleChart(Webcam webcam) {
        this.webcam = webcam;
        buildDefaultFrame();

        var configButton = new JButton("Save config");
        configButton.addActionListener((event) -> {
            generations = Integer.parseInt(genInput.getText());
            iterations = Integer.parseInt(iterInput.getText());
            includeTakingPhoto = includeTakePhotoCheckBox.getState();

            var currentChart = Arrays.stream(this.getContentPane().getComponents())
                    .filter(t -> t instanceof ChartPanel)
                    .map(t -> (ChartPanel) t)
                    .findFirst()
                    .orElse(null);

            remove(currentChart);
            add(new ChartPanel(createChart(createDataset())));
            revalidate();
            repaint();
        });

        var controlPanel = buildControlPanel(configButton);
        add(controlPanel, BorderLayout.NORTH);

        var dataset = createDataset();
        var chart = createChart(dataset);

        add(new ChartPanel(chart), BorderLayout.CENTER);
    }

    private JPanel buildControlPanel(JButton configButton) {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        controlPanel.add(genLabel);
        controlPanel.add(genInput);
        controlPanel.add(iterLabel);
        controlPanel.add(iterInput);
        controlPanel.add(includeTakePhotoCheckBox);
        controlPanel.add(configButton);
        return controlPanel;
    }

    private void buildDefaultFrame() {
        setResizable(true);
        setLayout(new BorderLayout());
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    private double average(List<Long> content) {
        return content.stream().mapToLong(i -> i).average().getAsDouble();
    }

    private JFreeChart createChart(DefaultCategoryDataset dataset) {
        return ChartFactory.createBarChart(
                "Rule speed comparison test (lower is better)",
                "%d generations of each Rules".formatted(generations),
                "Average time of %d iterations in ms".formatted(iterations),
                dataset);
    }

    private DefaultCategoryDataset createDataset() {
        var dataset = new DefaultCategoryDataset();

        var rule30TimeExecs = new ArrayList<Long>();
        var rule90TimeExecs = new ArrayList<Long>();
        var rule105TimeExecs = new ArrayList<Long>();
        var rule150TimeExecs = new ArrayList<Long>();
        var shuffleTimeExecs = new ArrayList<Long>();

        byte[] originalPhoto = WebcamUtils.getImageBytes(webcam, "bmp");
        var imageTakeExec = MeasureTimeUtil.measureTime(() -> WebcamUtils.getImageBytes(webcam, "bmp"));

        MeasureTimeUtil.measureAndPrintTime(() -> {
            DescriptiveStatistics stats = new DescriptiveStatistics();

            for (int i = 0; i < originalPhoto.length; i++) {
                stats.addValue(originalPhoto[i]);
            }

            return stats.getStandardDeviation();
        }, "Descriptive stats");


        for (int i = 0; i < iterations; i++) {
            // CA rules
            long cellular30 = MeasureTimeUtil.measureTime(() -> CellAutomataUtils.evolveWithCABytes(originalPhoto, generations, RulesSet::rule30));
            long cellular90 = MeasureTimeUtil.measureTime(() -> CellAutomataUtils.evolveWithCABytes(originalPhoto, generations, RulesSet::rule90));
            long cellular105 = MeasureTimeUtil.measureTime(() -> CellAutomataUtils.evolveWithCABytes(originalPhoto, generations, RulesSet::rule105));
            long cellular150 = MeasureTimeUtil.measureTime(() -> CellAutomataUtils.evolveWithCABytes(originalPhoto, generations, RulesSet::rule150));

            // Collections.shuffle baseline, one shuffle round per generation
            long shuffle = MeasureTimeUtil.measureTime(() -> ShuffleUtils.shuffleBytes(originalPhoto, generations));

            rule30TimeExecs.add(includeTakingPhoto ? imageTakeExec + cellular30 : cellular30);
            rule90TimeExecs.add(includeTakingPhoto ? imageTakeExec + cellular90 : cellular90);
            rule105TimeExecs.add(includeTakingPhoto ? imageTakeExec + cellular105 : cellular105);
            rule150TimeExecs.add(includeTakingPhoto ? imageTakeExec + cellular150 : cellular150);
            shuffleTimeExecs.add(includeTakingPhoto ? imageTakeExec + shuffle : shuffle);
        }

        long secureRandomTime = MeasureTimeUtil.measureTime(() -> {
            byte[] bytes = new byte[originalPhoto.length];
            SecureRandom secureRandom = null;
            try {
                secureRandom = SecureRandom.getInstanceStrong();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            secureRandom.nextBytes(bytes);
            return bytes;
        });

        dataset.addValue(secureRandomTime, "SecureRandom", "SecureRandom");
        dataset.addValue(average(rule30TimeExecs), "Rule30", "Rule30");
        dataset.addValue(average(rule90TimeExecs), "Rule90", "Rule90");
        dataset.addValue(average(rule105TimeExecs), "Rule105", "Rule105");
        dataset.addValue(average(rule150TimeExecs), "Rule150", "Rule150");
        dataset.addValue(average(shuffleTimeExecs), "Shuffle", "Shuffle");

        return dataset;
    }
}
