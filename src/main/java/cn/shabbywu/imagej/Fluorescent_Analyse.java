package cn.shabbywu.imagej;

import cn.shabbywu.imagej.utils.Misc;
import cn.shabbywu.imagej.window.FluorescenceAnalysingToolkitWindow;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Fluorescent_Analyse implements PlugInFilter {
    @Override
    public int setup(String s, ImagePlus imp) {
        // 加载插件时, 关闭由本插件打开的其他窗口
        for (String title: WindowManager.getImageTitles()) {
            if (title.equals(Misc.SetScaleWindowTitle) || title.equals(Misc.$i18n(Misc.SetScaleWindowTitle)) || title.contains("(red)") || title.contains("(green)") || title.contains("(blue)")) {
                ImageWindow w = (ImageWindow)WindowManager.getWindow(title);
                if (w == null) continue;
                w.close();
            }
        }
        while (imp == null) {
            OpenDialog od = new OpenDialog(Misc.$i18n("选择一张图片"), null);
            String fileName = od.getFileName();
            if (fileName != null && !fileName.isEmpty()) {
                imp = new Opener().openImage(od.getDirectory(), fileName);
                WindowManager.setTempCurrentImage(imp);
            }
            if (imp == null) {
                IJ.showMessage(Misc.$i18n("请选择一张图片再调用该插件"));
            }
        }
        return DOES_8G | DOES_16 | DOES_32 | DOES_RGB | NO_IMAGE_REQUIRED;
    }

    @Override
    public void run(ImageProcessor ip) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            imp = WindowManager.getTempCurrentImage();
        }
        ImageCanvas canvas = new ImageCanvas(imp);
        new FluorescenceAnalysingToolkitWindow(imp, canvas).setVisible(true);
    }

    /**
     * Main method for debugging.
     *
     * For debugging, it is convenient to have a method that starts ImageJ, loads
     * an image and calls the plugin, e.g. after setting breakpoints.
     *
     * @param args unused
     */
    public static void main(String[] args) throws Exception {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        // see: https://stackoverflow.com/a/7060464/1207769
        Class<?> clazz = Fluorescent_Analyse.class;
        java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        java.io.File file = new java.io.File(url.toURI());
        System.setProperty("plugins.dir", file.getAbsolutePath());

        // run the plugin
        new ImageJ();
        IJ.runPlugIn(clazz.getName(), "");
    }
}
