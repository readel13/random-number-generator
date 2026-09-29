package org.example.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Counts the report jobs currently running off the event dispatch thread, so the main window can
 * lock the camera selector while any of them is capturing. Switching devices mid-report would close
 * the webcam under a worker that is still calling
 * {@link org.example.webcam.WebcamSession#captureBmp()}.
 *
 * <p>Every method is called from the EDT, so the counter needs no synchronisation.</p>
 */
public final class ReportActivity {

    private static final List<Consumer<Boolean>> listeners = new ArrayList<>();

    private static int running;

    private ReportActivity() {
    }

    public static void begin() {
        running++;

        if (running == 1) {
            notifyListeners();
        }
    }

    public static void end() {
        running--;

        if (running == 0) {
            notifyListeners();
        }
    }

    public static boolean isIdle() {
        return running == 0;
    }

    /** The listener is called with {@code false} when the first report starts and {@code true} when the last finishes. */
    public static void addListener(Consumer<Boolean> idleListener) {
        listeners.add(idleListener);
    }

    private static void notifyListeners() {
        listeners.forEach(listener -> listener.accept(isIdle()));
    }
}
