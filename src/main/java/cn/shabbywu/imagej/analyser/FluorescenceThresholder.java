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
            
            ByteProcessor ip = (ByteProcessor) imp.getProcessor();
            ip.setLutAnimation(true);

            // fColor 前景(黑色), bColor 背景色(白色)
            int fColor = 0;
            int bColor = 255;
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
            // 再次设置阈值为 fColor(255), 保证后面 Create Selection 时可以正常取值
            imp.getProcessor().setThreshold(fColor, fColor, ImageProcessor.NO_LUT_UPDATE);
            imp.updateAndRepaintWindow();
        } finally {
            imp.unlock();
        }
    }
}
