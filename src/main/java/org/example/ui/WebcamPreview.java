package org.example.ui;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import org.example.webcam.WebcamSession;
import org.example.webcam.WebcamSessionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/**
 * Live preview of the session's webcam. Follows the session on its own, so switching devices does
 * not need the main window to rebuild anything.
 */
public class WebcamPreview extends JPanel implements WebcamSessionListener {

    private static final String NO_WEBCAM_MESSAGE = "No webcam detected - connect one and restart the application";

    private final JLabel placeholder = new JLabel(NO_WEBCAM_MESSAGE, SwingConstants.CENTER);

    private WebcamPanel panel;

    public WebcamPreview(WebcamSession session) {
        setLayout(new BorderLayout());

        session.addListener(this);
        if (session.isActive()) {
            webcamActivated(session.getActive());
        } else {
            add(placeholder, BorderLayout.CENTER);
        }
    }

    @Override
    public void webcamReleasing(Webcam released) {
        if (panel == null) {
            return;
        }

        panel.stop();
        remove(panel);
        panel = null;

        add(placeholder, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    @Override
    public void webcamActivated(Webcam activated) {
        remove(placeholder);

        panel = new WebcamPanel(activated);
        panel.setFPSDisplayed(true);
        panel.setImageSizeDisplayed(true);

        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
