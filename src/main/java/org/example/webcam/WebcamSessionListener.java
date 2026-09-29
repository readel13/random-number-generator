package org.example.webcam;

import com.github.sarxos.webcam.Webcam;

/**
 * Notified when {@link WebcamSession} switches devices. {@link #webcamReleasing(Webcam)} runs while
 * the old device is still open, so listeners can detach from it before it is closed.
 */
public interface WebcamSessionListener {

    default void webcamReleasing(Webcam released) {
    }

    void webcamActivated(Webcam activated);
}
