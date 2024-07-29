package org.example.ui.reports;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import org.example.rule.RulesSet;
import org.example.utils.CellAutomataUtils;
import org.example.math.MathUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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

    private static JFreeChart createChart(XYSeriesCollection generationCollection) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Similarity rate to CA generations (lower better)",  // title
                "Generation",             // x-axis label
                "Similarity in percentage",   // y-axis label
                generationCollection);

        var xAxis = (NumberAxis) ((XYPlot) chart.getPlot()).getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(1));
        xAxis.setRange(1, 10);

        var yAxis = (NumberAxis) ((XYPlot) chart.getPlot()).getRangeAxis();
        yAxis.setRange(0, 20);
        yAxis.setTickUnit(new NumberTickUnit(1));
        return chart;
    }

    private XYSeriesCollection createDataset() {
        var rule30GenerationCa = new XYSeries("Rule 30");
        var rule90GenerationCa = new XYSeries("Rule 90");
        var rule105GenerationCa = new XYSeries("Rule 105");
        var rule150GenerationCa = new XYSeries("Rule 150");
        var generationCollection = new XYSeriesCollection();

        byte[] first = WebcamUtils.getImageBytes(webcam, "bmp");

        for (int i = 0; i < 10; i++) {
            int finalIndex = i + 1;

            // CA rules
            byte[] cellular30 = CellAutomataUtils.evolveWithCABytes(first, finalIndex);
            byte[] cellular90 = CellAutomataUtils.evolveWithCABytes(first, finalIndex, RulesSet::rule90);
            byte[] cellular105 = CellAutomataUtils.evolveWithCABytes(first, finalIndex, RulesSet::rule105);
            byte[] cellular150 = CellAutomataUtils.evolveWithCABytes(first, finalIndex, RulesSet::rule150);

//                System.out.println("Similiraty rate: " + similarityRate);
            rule30GenerationCa.add(finalIndex, MathUtil.compare(first, cellular30));
            rule90GenerationCa.add(finalIndex, MathUtil.compare(first, cellular90));
            rule105GenerationCa.add(finalIndex, MathUtil.compare(first, cellular105));
            rule150GenerationCa.add(finalIndex, MathUtil.compare(first, cellular150));
        }

        generationCollection.addSeries(rule30GenerationCa);
        generationCollection.addSeries(rule90GenerationCa);
        generationCollection.addSeries(rule105GenerationCa);
        generationCollection.addSeries(rule150GenerationCa);
        return generationCollection;
    }
}
