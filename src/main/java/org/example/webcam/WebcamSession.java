package org.example.webcam;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import com.github.sarxos.webcam.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * The one webcam the application is currently capturing from. Every report holds the session rather
 * than a {@link Webcam}, so switching the device in the dropdown takes effect everywhere without
 * rebuilding the open frames.
 */
@Slf4j
public class WebcamSession {

    public static final String CAPTURE_FORMAT = ImageUtils.FORMAT_BMP;

    private final List<WebcamSessionListener> listeners = new ArrayList<>();

    private Webcam active;

    /** What was last asked for - not {@code active.getViewSize()}, which drivers clamp to what they can deliver. */
    private Dimension requested;

    /**
     * Closes the current device and opens the given one at the given resolution. A failure leaves
     * the session with no active device rather than with a half-open one.
     */
    public void activate(Webcam webcam, Dimension resolution) {
        if (webcam == null) {
            throw new IllegalArgumentException("Webcam to activate must not be null");
        }
        if (webcam.equals(active) && resolution.equals(requested)) {
            return;
        }

        release();

        try {
            WebcamService.resolutionsFor(webcam);
            webcam.setViewSize(resolution);
            webcam.open(true);
            active = webcam;
            requested = resolution;
            log.info("Activated webcam {} at {}", webcam.getName(), WebcamService.describe(resolution));
        } catch (RuntimeException e) {
            log.error("Could not open webcam {} at {}", webcam.getName(), WebcamService.describe(resolution), e);
            webcam.close();
            throw e;
        }

        listeners.forEach(listener -> listener.webcamActivated(active));
    }

    /** Closes the active device, detaching every listener from it first. */
    public void release() {
        if (active == null) {
            return;
        }

        Webcam released = active;
        listeners.forEach(listener -> listener.webcamReleasing(released));
        active = null;
        requested = null;
        released.close();
    }

    /** Closes the active device without notifying listeners - for JVM shutdown, off the EDT. */
    public void shutdown() {
        Webcam released = active;
        active = null;
        requested = null;

        if (released != null) {
            released.close();
        }
    }

    public Webcam getActive() {
        return active;
    }

    public boolean isActive() {
        return active != null && active.isOpen();
    }

    public Dimension getViewSize() {
        return requireActive().getViewSize();
    }

    /** A freshly captured frame as raw BMP bytes - header included, see {@code BufferedImageUtils.getImageOffsetBmp}. */
    public byte[] captureBmp() {
        return WebcamUtils.getImageBytes(requireActive(), CAPTURE_FORMAT);
    }

    /** Writes a frame to {@code <fileName>.bmp} in the working directory. */
    public void captureToFile(String fileName) {
        WebcamUtils.capture(requireActive(), fileName, CAPTURE_FORMAT);
    }

    public void addListener(WebcamSessionListener listener) {
        listeners.add(listener);
    }

    private Webcam requireActive() {
        if (active == null) {
            throw new IllegalStateException("No webcam is selected");
        }
        return active;
    }
}
