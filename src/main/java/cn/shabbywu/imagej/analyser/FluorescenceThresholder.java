package cn.shabbywu.imagej.analyser;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class FluorescenceThresholder {
    public static void doThreshold(ImagePlus imp, double minThreshold, double maxThreshold, boolean redMode) {
        try {
            if (!imp.lock()) return;

            // TODO: 支持 red mode
            if (false) {
                // red lut
                // 保留原图颜色
                ByteProcessor ip = (ByteProcessor)imp.getProcessor();
                ip.setThreshold(minThreshold, maxThreshold, ImageProcessor.RED_LUT);
                ip.setLutAnimation(true);
                ip.applyLut();
            } else {
                // black and white lut
                // 前景(黑色), 背景色(白色)
                ByteProcessor ip = (ByteProcessor)imp.getProcessor();
                ip.setThreshold(minThreshold, maxThreshold, ImageProcessor.BLACK_AND_WHITE_LUT);
                ip.applyLut();
            }
            imp.updateAndRepaintWindow();
        } finally {
            imp.unlock();
        }
    }
}
