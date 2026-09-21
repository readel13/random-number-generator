package org.example.ui.reports;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import org.example.rule.RulesSet;
import org.example.utils.CellAutomataUtils;
import org.example.utils.ShuffleUtils;
import org.example.math.MathUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;

public class SimilarityRuleChart extends JFrame {

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    private final Webcam webcam;

    public SimilarityRuleChart(Webcam webcam) {
        this.webcam = webcam;
        buildDefaultFrame();

        var generationCollection = createDataset();
        var chart = createChart(generationCollection);

        add(new ChartPanel(chart));
    }

    private void buildDefaultFrame() {
        setTitle("Rule similarity comparison test");
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
        setSize(WIDTH, HEIGHT);
    }

    private static JFreeChart createChart(DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
                "Similarity rate to CA generations (lower better)",  // title
                "Generation",             // x-axis label
                "Similarity in percentage",   // y-axis label
                dataset);
//
//        var xAxis = (NumberAxis) ((XYPlot) chart.getPlot()).getDomainAxis();
//        xAxis.setTickUnit(new NumberTickUnit(1));
//        xAxis.setRange(1, 10);

//        var yAxis = (NumberAxis) ((XYPlot) chart.getPlot()).getRangeAxis();
//        yAxis.setRange(0, 20);
//        yAxis.setTickUnit(new NumberTickUnit(1));
        return chart;
    }

    private DefaultCategoryDataset createDataset() {
        var dataset = new DefaultCategoryDataset();

        byte[] first = WebcamUtils.getImageBytes(webcam, "bmp");

        for (int i = 0; i < 10; i++) {
            int finalIndex = i + 1;

            // CA rules
            byte[] cellular30 = CellAutomataUtils.evolveWithCABytes(first, finalIndex);
            byte[] cellular90 = CellAutomataUtils.evolveWithCABytes(first, finalIndex, RulesSet::rule90);
            byte[] cellular105 = CellAutomataUtils.evolveWithCABytes(first, finalIndex, RulesSet::rule105);
            byte[] cellular150 = CellAutomataUtils.evolveWithCABytes(first, finalIndex, RulesSet::rule150);

            // Collections.shuffle baseline
            byte[] shuffled = ShuffleUtils.shuffleBytes(first, finalIndex);

//                System.out.println("Similiraty rate: " + similarityRate);
            dataset.addValue(MathUtil.compare(first, cellular30), "Rule30", String.valueOf(finalIndex));
            dataset.addValue(MathUtil.compare(first, cellular90), "Rule 90", String.valueOf(finalIndex));
            dataset.addValue(MathUtil.compare(first, cellular105), "Rule 105", String.valueOf(finalIndex));
            dataset.addValue(MathUtil.compare(first, cellular150), "Rule 150", String.valueOf(finalIndex));
            dataset.addValue(MathUtil.compare(first, shuffled), "Shuffle", String.valueOf(finalIndex));
        }

        return dataset;
    }
}
