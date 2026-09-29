package org.example.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Runs a report's dataset off the event dispatch thread and puts the result back on it.
 *
 * <p>Building the dataset in the constructor (or in a "Save config" listener) blocked the EDT after
 * the frame had already been shown, so the window never got its first paint - the white rectangle.
 * While a job runs the frame's own buttons are disabled so a second run cannot start on top of it,
 * and {@link ReportActivity} stops the main window from switching cameras underneath a worker that
 * is still capturing.</p>
 */
public final class AsyncReport {

    private AsyncReport() {
    }

    /**
     * Builds the result in the background and swaps it into the centre of {@code frame} when it is
     * ready, showing a progress bar meanwhile. Replaces whatever the previous run left there.
     */
    public static <T> void load(JFrame frame, Supplier<T> work, Function<T, Component> render) {
        ResultSlot slot = slotOf(frame);
        slot.showBusy();

        start(frame, work, value -> slot.showResult(render.apply(value)), slot::showError);
    }

    /**
     * Runs the work in the background and hands the result to {@code onDone} on the EDT. Nothing is
     * swapped - for the frames that feed an existing component rather than a chart.
     */
    public static <T> void run(Component owner, Supplier<T> work, Consumer<T> onDone) {
        Window window = owner instanceof Window w ? w : SwingUtilities.getWindowAncestor(owner);

        start(window, work, onDone, error -> JOptionPane.showMessageDialog(window, describe(error),
                "Report failed", JOptionPane.ERROR_MESSAGE));
    }

    private static <T> void start(Container scope, Supplier<T> work, Consumer<T> onDone, Consumer<Throwable> onError) {
        List<Component> reEnable = disableControls(scope);
        ReportActivity.begin();

        new SwingWorker<T, Void>() {

            @Override
            protected T doInBackground() {
                return work.get();
            }

            @Override
            protected void done() {
                try {
                    onDone.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    onError.accept(e.getCause());
                } finally {
                    reEnable.forEach(component -> component.setEnabled(true));
                    ReportActivity.end();
                }
            }
        }.execute();
    }

    /**
     * Switches off only the buttons, which is all that is needed to stop a second run being started
     * on top of this one. Checkboxes and input fields stay live so the next configuration can be set
     * up while this run finishes - and, just as importantly, stay legible: a disabled AWT
     * {@link Checkbox} greys its label out so far that the control row looks empty.
     *
     * <p>The main window's camera controls are a separate concern, locked by {@link ReportActivity}.</p>
     *
     * @return the buttons that were switched off, so exactly those come back on afterwards.
     */
    private static List<Component> disableControls(Container scope) {
        List<Component> disabled = new ArrayList<>();

        if (scope == null) {
            return disabled;
        }

        for (Component component : descendants(scope)) {
            boolean button = component instanceof Button || component instanceof JButton;

            if (button && component.isEnabled()) {
                component.setEnabled(false);
                disabled.add(component);
            }
        }

        return disabled;
    }

    private static List<Component> descendants(Container root) {
        List<Component> all = new ArrayList<>(Arrays.asList(root.getComponents()));

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) instanceof Container container) {
                all.addAll(Arrays.asList(container.getComponents()));
            }
        }

        return all;
    }

    private static ResultSlot slotOf(JFrame frame) {
        for (Component component : frame.getContentPane().getComponents()) {
            if (component instanceof ResultSlot slot) {
                return slot;
            }
        }

        ResultSlot slot = new ResultSlot();
        frame.add(slot, BorderLayout.CENTER);
        frame.revalidate();

        return slot;
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();

        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    /** Holds whatever the report is showing right now: the progress bar, the chart, or a failure. */
    private static class ResultSlot extends JPanel {

        ResultSlot() {
            super(new BorderLayout());
        }

        void showBusy() {
            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);
            progress.setMaximumSize(new Dimension(400, 24));
            progress.setAlignmentX(CENTER_ALIGNMENT);

            JLabel label = new JLabel("Running report...", SwingConstants.CENTER);
            label.setAlignmentX(CENTER_ALIGNMENT);
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

            JPanel centered = new JPanel();
            centered.setLayout(new BoxLayout(centered, BoxLayout.Y_AXIS));
            centered.add(Box.createVerticalGlue());
            centered.add(label);
            centered.add(progress);
            centered.add(Box.createVerticalGlue());

            swap(centered);
        }

        void showResult(Component result) {
            swap(result);
        }

        void showError(Throwable error) {
            swap(new JLabel("Report failed: " + describe(error), SwingConstants.CENTER));
        }

        private void swap(Component component) {
            removeAll();
            add(component, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }
}
