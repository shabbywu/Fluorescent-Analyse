package cn.shabbywu.imagej.analyser;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public class FluorescenceThresholder {
    public static void doThreshold(ImagePlus imp, double minThreshold, double maxThreshold) {
        try {
            if (!imp.lock()) return;

            ImageProcessor ip = imp.getProcessor();
            ip.setThreshold(minThreshold, maxThreshold, ImageProcessor.RED_LUT);
            ip.setLutAnimation(true);

            // fColor 前景(黑色), bColor 背景色(白色)
            int fColor = 0;
            int bColor = 255;

            if (ip.isColorLut()) ip.setColorModel(ip.getDefaultColorModel());
            ip.setAutoThreshold(ImageProcessor.ISODATA2, ImageProcessor.RED_LUT);

            int[] lut = new int[256];
            for (int i = 0; i < 256; i++) {
                // 荧光越强, 颜色越亮(数值越大)
                if (i >= minThreshold && i <= maxThreshold) {
                    lut[i] = fColor;
                } else {
                    lut[i] = bColor;
                }
            }
            ip.applyTable(lut);
            imp.getProcessor().setThreshold(fColor, fColor, ImageProcessor.NO_LUT_UPDATE);
            imp.updateAndRepaintWindow();
        } finally {
            imp.unlock();
        }
    }
}
