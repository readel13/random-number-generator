package org.example;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import org.example.ui.button.AnalyticButton;
import org.example.ui.button.MyCustomButton;
import org.example.ui.frames.GetNoiseTextExample;
import org.example.ui.frames.ModifiedImageFrame;
import org.example.ui.reports.BytesDistributionReport;
import org.example.ui.reports.OneVsZeros;
import org.example.ui.reports.SimilarityRuleChart;
import org.example.ui.reports.SpeedRuleChart;
import org.example.ui.dropdown.ResolutionDropdown;
import org.example.ui.dropdown.WebcamDropdown;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class WebMain {

    public static void main(String[] args) {
        List<Webcam> webcams = Webcam.getWebcams();

        var defaultWebcam = Webcam.getDefault();
        defaultWebcam.setCustomViewSizes(ResolutionDropdown.CUSTOM_RESOLUTIONS);
        defaultWebcam.setViewSize(WebcamResolution.HD.getSize());
        defaultWebcam.open(true);

        var mainWindow = new JFrame("Program");
        mainWindow.setLayout(new BorderLayout());
        mainWindow.setResizable(true);
        mainWindow.setSize(1600, 900);
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));

        var resolutionDropdown = new ResolutionDropdown(defaultWebcam);
        var webcamDropdown = new WebcamDropdown(webcams, resolutionDropdown);

        var saveConfig = new Button("Save config");
        saveConfig.addActionListener(e -> {
            // find defaultWebcam component and close connection
            var currentWebcamComponent = Arrays.stream(mainWindow.getContentPane().getComponents())
                    .filter(t -> t instanceof WebcamPanel)
                    .map(t -> (WebcamPanel) t)
                    .findFirst()
                    .orElse(null);

            currentWebcamComponent.getWebcam().close();
            currentWebcamComponent.removeAll();

            mainWindow.remove(currentWebcamComponent);

            var selectedWebcam = webcams.get(webcamDropdown.getSelectedIndex());
            selectedWebcam.setViewSize(ResolutionDropdown.CUSTOM_RESOLUTIONS[resolutionDropdown.getSelectedIndex()]);
            selectedWebcam.open(true);

            var updatedWebcamPanel = new WebcamPanel(selectedWebcam);
            updatedWebcamPanel.setFPSDisplayed(true);
            updatedWebcamPanel.setImageSizeDisplayed(true);

            mainWindow.add(updatedWebcamPanel, BorderLayout.CENTER);
            mainWindow.revalidate();
            mainWindow.repaint();
        });

        buttonPanel.add(new MyCustomButton("Byte distribution", e -> new BytesDistributionReport(defaultWebcam)));
        buttonPanel.add(new MyCustomButton("One Vs Zero", e -> new OneVsZeros(defaultWebcam)));
        buttonPanel.add(new AnalyticButton(defaultWebcam));
        buttonPanel.add(new MyCustomButton("Rule similarity comparison test", e -> new SimilarityRuleChart(defaultWebcam)));
        buttonPanel.add(new MyCustomButton("Speed rule test", e -> new SpeedRuleChart(defaultWebcam)));
        buttonPanel.add(new MyCustomButton("Modified Image", e -> new ModifiedImageFrame(defaultWebcam)));
        buttonPanel.add(new MyCustomButton("Get noise text", e -> new GetNoiseTextExample(defaultWebcam)));
        buttonPanel.add(webcamDropdown);
        buttonPanel.add(resolutionDropdown);
        buttonPanel.add(saveConfig);

        var webcamPanel = new WebcamPanel(defaultWebcam);
        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setImageSizeDisplayed(true);

        mainWindow.add(buttonPanel, BorderLayout.NORTH);
        mainWindow.add(webcamPanel, BorderLayout.CENTER);

        mainWindow.setVisible(true);
    }
}
