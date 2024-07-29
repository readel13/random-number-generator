package org.example.ui.button;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import org.example.utils.CellAutomataUtils;
import org.example.math.MathUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class AnalyticButton extends Button {

    private final static String BUTTON_NAME = "Make analysis image";

    private final Webcam webcam;

    public AnalyticButton(Webcam webcam) {
        this.webcam = webcam;
        setLabel(BUTTON_NAME);

        addActionListener(buildActionListener());
    }

    private ActionListener buildActionListener() {
        return e -> {
            List<Double> rates = new ArrayList<>();
            List<Boolean> consistentFrames = new ArrayList<>();

            SecureRandom secureRandom = new SecureRandom();
            byte[] values = new byte[921654];
            secureRandom.nextBytes(values);

            System.out.println("Secure random bytes: ");
            MathUtil.analyseFrame(values, 30, 70);

            for (int i = 0; i < 10; i++) {
                byte[] first = WebcamUtils.getImageBytes(webcam, "bmp");
                boolean consistentFrame = MathUtil.analyseFrame(first, 30, 70).isConsistentFrame();
                consistentFrames.add(consistentFrame);

                byte[] firstCellular = CellAutomataUtils.evolveWithCABytes(first, i);

//                consistentFrames.add(ComparingUtils.isConsistentFrame(first, 30, 70));
//
                byte[] thrid = WebcamUtils.getImageBytes(webcam, "bmp");

                rates.add(MathUtil.compare(first, thrid));
            }

            double similarityRate = rates.stream().mapToDouble(d -> d).average().orElse(0.0d);
            double consistentcyRate = (double) (100 * consistentFrames.stream().filter(Boolean.TRUE::equals).count()) / consistentFrames.size();
            System.out.println("AVG similarity rate: " + similarityRate);
            System.out.println("AVG consistent rate: " + consistentcyRate);

            JOptionPane.showMessageDialog(null, "AVG similarity rate: %.2f\nAVG consistent rate: %.2f".formatted(similarityRate, consistentcyRate));
        };
    }
}
