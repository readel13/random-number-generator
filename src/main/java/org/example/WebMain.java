package org.example;

import com.github.sarxos.webcam.Webcam;
import org.example.ui.ReportActivity;
import org.example.ui.WebcamPreview;
import org.example.ui.WrapLayout;
import org.example.ui.button.AnalyticButton;
import org.example.ui.button.MyCustomButton;
import org.example.ui.dropdown.ResolutionDropdown;
import org.example.ui.dropdown.WebcamDropdown;
import org.example.ui.frames.GetNoiseTextExample;
import org.example.ui.frames.ModifiedImageFrame;
import org.example.ui.reports.ByteChangeComparison;
import org.example.ui.reports.ByteChangeDistrComparison;
import org.example.ui.reports.BytesDistributionReport;
import org.example.ui.reports.OneVsZeros;
import org.example.ui.reports.SimilarityRuleChart;
import org.example.ui.reports.SpeedRuleChart;
import org.example.webcam.WebcamService;
import org.example.webcam.WebcamSession;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WebMain {

    public static void main(String[] args) {
        // the tooltips here run several lines; the stock 750ms/4s pair pops them up while the
        // pointer is only passing through, then hides them before they can be read
        ToolTipManager.sharedInstance().setInitialDelay(1500);
        ToolTipManager.sharedInstance().setDismissDelay(30_000);
        // the webcam preview repaints constantly and paints straight over a lightweight popup,
        // so tooltips need their own window to survive on screen
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        List<Webcam> webcams = WebcamService.discover();
        WebcamSession session = new WebcamSession();

        var mainWindow = new JFrame("Image change analyzer");
        mainWindow.setLayout(new BorderLayout());
        mainWindow.setResizable(true);
        mainWindow.setSize(1600, 900);
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // two rows: the report buttons, and the camera controls underneath. Sharing one row made the
        // bar overflow the window width, and FlowLayout quietly clipped whatever wrapped.
        var buttonPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 15));
        var cameraPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 15));
        var topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(buttonPanel);
        topPanel.add(cameraPanel);

        // the first discovered device is what Webcam.getDefault() would have returned
        Webcam defaultWebcam = webcams.isEmpty() ? null : webcams.getFirst();
        var resolutionDropdown = new ResolutionDropdown(defaultWebcam);
        var webcamDropdown = new WebcamDropdown(webcams, resolutionDropdown);

        webcamDropdown.setToolTipText("<html>Which camera every report captures from.<br>"
                + "Choosing one reloads the resolutions that device offers;<br>"
                + "nothing switches until you press <b>Save config</b>.</html>");

        resolutionDropdown.setToolTipText("<html>Capture resolution for the selected camera.<br>"
                + "The list merges the sizes the driver reports with a few standard ones -<br>"
                + "a device may quietly clamp to the nearest size it can actually deliver.</html>");

        var applyConfig = new Button("Save config");
        applyConfig.addActionListener(e -> applyConfig(mainWindow, session, webcamDropdown, resolutionDropdown));

        List<Component> reportButtons = List.of(
                new MyCustomButton("Byte distribution", e -> new BytesDistributionReport(session)),
                new MyCustomButton("One Vs Zero", e -> new OneVsZeros(session)),
                new MyCustomButton("ByteChangeComparassion (LineChart)", e -> new ByteChangeComparison(session)),
                new MyCustomButton("Byte Change Distribution", e -> new ByteChangeDistrComparison(session)),
                new AnalyticButton(session),
                new MyCustomButton("Rule similarity comparison test", e -> new SimilarityRuleChart(session)),
                new MyCustomButton("Speed rule test", e -> new SpeedRuleChart(session)),
                new MyCustomButton("Modified Image", e -> new ModifiedImageFrame(session)),
                new MyCustomButton("Get noise text", e -> new GetNoiseTextExample(session))
        );
        reportButtons.forEach(buttonPanel::add);
        cameraPanel.add(new Label("Camera:"));
        cameraPanel.add(webcamDropdown);
        cameraPanel.add(new Label("Resolution:"));
        cameraPanel.add(resolutionDropdown);
        cameraPanel.add(applyConfig);

        // a running report captures from a background thread; switching or reopening the device
        // underneath it would close the webcam mid-capture
        List<Component> controls = new ArrayList<>(reportButtons);
        controls.add(webcamDropdown);
        controls.add(resolutionDropdown);
        controls.add(applyConfig);
        ReportActivity.addListener(idle -> controls.forEach(control -> control.setEnabled(idle)));

        mainWindow.add(topPanel, BorderLayout.NORTH);
        mainWindow.add(new WebcamPreview(session), BorderLayout.CENTER);
        mainWindow.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(session::release));

        if (defaultWebcam == null) {
            controls.forEach(control -> control.setEnabled(false));
            JOptionPane.showMessageDialog(mainWindow,
                    "No webcam was detected on this system. The reports need a camera to capture frames.",
                    "No webcam", JOptionPane.WARNING_MESSAGE);
            return;
        }

        openSelected(mainWindow, session, defaultWebcam, resolutionDropdown.getSelectedResolution());
    }

    private static void applyConfig(JFrame mainWindow,
                                    WebcamSession session,
                                    WebcamDropdown webcamDropdown,
                                    ResolutionDropdown resolutionDropdown) {
        Webcam selected = webcamDropdown.getSelectedWebcam();
        if (selected == null) {
            return;
        }

        openSelected(mainWindow, session, selected, resolutionDropdown.getSelectedResolution());
    }

    private static void openSelected(JFrame mainWindow, WebcamSession session, Webcam webcam, Dimension resolution) {
        try {
            session.activate(webcam, resolution);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(mainWindow,
                    "Could not open %s at %s:%n%s".formatted(webcam.getName(), WebcamService.describe(resolution), ex.getMessage()),
                    "Webcam error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
