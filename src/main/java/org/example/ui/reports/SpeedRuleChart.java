package org.example.ui.reports;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.example.ui.AsyncReport;
import org.example.webcam.WebcamSession;
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

    private final WebcamSession session;

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    private int iterations = 10;
    private int generations = 1;
    private boolean includeTakingPhoto = false;

    private final JLabel genLabel = new JLabel("Generations:");
    private final JTextField genInput = new JTextField(10);
    private final JLabel iterLabel = new JLabel("Iterations:");
    private final JTextField iterInput = new JTextField(10);
    // a JCheckBox rather than the AWT Checkbox, so it can carry a tooltip
    private final JCheckBox includeTakePhotoCheckBox = new JCheckBox("Include taking a photo", includeTakingPhoto);

    public SpeedRuleChart(WebcamSession session) {
        this.session = session;
        buildDefaultFrame();

        var configButton = new JButton("Save config");
        configButton.addActionListener((event) -> {
            generations = StringUtils.isNoneEmpty(genInput.getText())
                    ? Integer.parseInt(genInput.getText())
                    : generations;
            iterations = StringUtils.isNoneEmpty(iterInput.getText())
                    ? Integer.parseInt(iterInput.getText())
                    : iterations;
            includeTakingPhoto = includeTakePhotoCheckBox.isSelected();

            AsyncReport.load(this, this::createDataset, dataset -> new ChartPanel(createChart(dataset)));
        });

        var controlPanel = buildControlPanel(configButton);
        add(controlPanel, BorderLayout.NORTH);

        AsyncReport.load(this, this::createDataset, dataset -> new ChartPanel(createChart(dataset)));
    }

    private JPanel buildControlPanel(JButton configButton) {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));

        String generationsTip = "<html>How many CA generations each rule applies before the clock stops.<br>"
                + "One generation reads every bit of the frame once, so the cost grows<br>"
                + "linearly - this is the axis the bars are actually comparing.<br>"
                + "Left empty, the current value (" + generations + ") is kept.</html>";
        genLabel.setToolTipText(generationsTip);
        genInput.setToolTipText(generationsTip);

        String iterationsTip = "<html>How many times each rule is timed; the bar shows the average.<br>"
                + "More iterations even out JIT warm-up and GC pauses, which otherwise<br>"
                + "dominate a single run, at the cost of a longer wait.<br>"
                + "Left empty, the current value (" + iterations + ") is kept.</html>";
        iterLabel.setToolTipText(iterationsTip);
        iterInput.setToolTipText(iterationsTip);

        includeTakePhotoCheckBox.setToolTipText("<html>Add the cost of capturing one frame to every bar.<br>"
                + "Off, the bars compare the rules alone.<br>"
                + "On, they show what a full capture-then-extract round costs - the capture<br>"
                + "is timed once and added to each rule, so it shifts every bar equally.</html>");

        controlPanel.add(genLabel);
        controlPanel.add(genInput);
        controlPanel.add(iterLabel);
        controlPanel.add(iterInput);
        controlPanel.add(includeTakePhotoCheckBox);
        controlPanel.add(configButton);
        return controlPanel;
    }

    private void buildDefaultFrame() {
        setTitle("Speed rule test");
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

        byte[] originalPhoto = session.captureBmp();
        var imageTakeExec = MeasureTimeUtil.measureTime(() -> session.captureBmp());

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
            long shuffle = MeasureTimeUtil.measureTime(() -> ShuffleUtils.shuffleBits(originalPhoto, generations));

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
        dataset.addValue(average(shuffleTimeExecs), "Shuffle bits", "Shuffle bits");

        return dataset;
    }
}
