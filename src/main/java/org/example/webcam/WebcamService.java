package org.example.webcam;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamResolution;
import lombok.extern.slf4j.Slf4j;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Device discovery. The underlying driver (sarxos {@code WebcamDefaultDriver}) already picks the
 * right native grabber per platform, so nothing here is OS specific - this class only makes the
 * lookup fail softly and negotiates the resolutions a given device can be driven at.
 */
@Slf4j
public final class WebcamService {

    /**
     * Resolutions offered for every device even when the driver does not report them; the capture
     * library scales to them via {@link Webcam#setCustomViewSizes(Dimension...)}.
     */
    public static final List<Dimension> PREFERRED_RESOLUTIONS = List.of(
            WebcamResolution.VGA.getSize(),
            WebcamResolution.HD.getSize(),
            WebcamResolution.HDP.getSize(),
            WebcamResolution.FHD.getSize(),
            WebcamResolution.WQHD.getSize()
    );

    /** What a device is opened at unless the user picks something else. */
    public static final Dimension DEFAULT_RESOLUTION = WebcamResolution.HD.getSize();

    private static final long DISCOVERY_TIMEOUT_SECONDS = 10;

    private WebcamService() {
    }

    /**
     * @return every webcam the driver can see, or an empty list when there is none or discovery
     * failed. The first entry is what {@code Webcam.getDefault()} would have returned.
     */
    public static List<Webcam> discover() {
        try {
            List<Webcam> webcams = Webcam.getWebcams(DISCOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Discovered {} webcam(s): {}", webcams.size(), webcams.stream().map(Webcam::getName).toList());
            return List.copyOf(webcams);
        } catch (TimeoutException | RuntimeException e) {
            log.warn("Webcam discovery failed", e);
            return List.of();
        }
    }

    /**
     * Resolutions selectable for the given device: the ones the driver reports, plus the preferred
     * ones, largest last. Registers the extras as custom view sizes so they can actually be set.
     */
    public static List<Dimension> resolutionsFor(Webcam webcam) {
        Set<Dimension> resolutions = new LinkedHashSet<>(PREFERRED_RESOLUTIONS);

        try {
            resolutions.addAll(List.of(webcam.getViewSizes()));
        } catch (WebcamException e) {
            log.warn("Could not read native resolutions of {}, falling back to the preferred ones", webcam.getName(), e);
        }

        List<Dimension> sorted = new ArrayList<>(resolutions);
        sorted.sort(Comparator.comparingInt(d -> d.width * d.height));

        webcam.setCustomViewSizes(sorted.toArray(new Dimension[0]));

        return List.copyOf(sorted);
    }

    public static String describe(Dimension resolution) {
        return resolution.width + "x" + resolution.height;
    }
}
