package cn.shabbywu.imagej.window;

import cn.shabbywu.imagej.utils.Misc;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.ColorProcessor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class FluorescenceAnalysingToolkitWindow extends ImageWindow {
    protected ImagePlus impDuplicated;
    private int measurement = Analyzer.AREA | Analyzer.INTEGRATED_DENSITY | Analyzer.MEAN;
    public FluorescenceAnalysingToolkitWindow(ImagePlus imp, ImageCanvas ic) {
        super(imp, ic);
        impDuplicated = imp.duplicate();
        setFont(Misc.GetFont());
        initBottomPanel();
        resizeWindow();
    }

    void initBottomPanel(){
        Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        Button resetButton = new Button(Misc.$i18n("重置"));
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getImagePlus().setProcessor(impDuplicated.duplicate().getProcessor());
            }
        });
        panel.add(resetButton);

        Button openImageButton = new Button(Misc.$i18n("打开图片"));
        openImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenDialog od = new OpenDialog(Misc.$i18n("选择一张图片"), null);
                String fileName = od.getFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    imp = new Opener().openImage(od.getDirectory(), fileName);
                    setImage(imp);
                    setTitle(fileName);
                    resizeWindow();
                }
            }
        });
        panel.add(openImageButton);

        Button scaleButton = new Button(Misc.$i18n("设置比例尺"));
        scaleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IJ.showMessage("提示", "请沿着比例尺划线, 点击 OK 继续");
                ImagePlus copy = imp.duplicate();
                copy.setTitle(Misc.SetScaleWindowTitle);

                ImageWindow w = new ImageWindow(copy, new DrawHorizontalLineCanvas(copy));
                IJ.log("Waiting to draw a line");
            }
        });
        panel.add(scaleButton);

        Button manualMeasureButton = new Button(Misc.$i18n("清空外部区域 && 分离 RGB 通道 && 进入下一步"));
        manualMeasureButton.addActionListener(new MeasureActionListener(false));
        panel.add(manualMeasureButton);

        // TODO: 是否需要一键测量?
        //        Button batchMeasureButton = new Button("一键测量");
        //        batchMeasureButton.addActionListener(new MeasureActionListener(true));
        //        panel.add(batchMeasureButton);

        add(panel);
    }

    public void resizeWindow() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point loc = getLocation();
        Dimension size = getSize();
        if (loc.y + size.height > screenSize.height) {
            getCanvas().zoomOut(0, 0);
        }
    }

    class MeasureActionListener implements ActionListener {
        boolean batchMode;
        public MeasureActionListener(boolean batchMode) {
            this.batchMode = batchMode;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Roi roi = imp.getRoi();
            if (roi == null) {
                boolean ready = IJ.showMessageWithCancel(Misc.$i18n("提示"), Misc.$i18n("是否已圈取细胞区域？点击 OK 继续"));
                if (!ready) return;
            }
            roi = imp.getRoi();
            if (!roi.getTypeAsString().equals("Freehand")) {
                boolean ready = IJ.showMessageWithCancel(Misc.$i18n("提示"), Misc.$i18n("检测到画圈区域并非使用 Freehand, 是否继续？"));
                if (!ready) return;
            }

            ImagePlus copy = imp.duplicate();
            copy.setTitle(imp.getTitle());
            copy.setRoi(roi);
            if (batchMode) {
                IJ.run(copy, "Clear Outside", null);
                ChannelStack stack = splitChannel(copy);
                double originalArea = measureOriginalArea(copy);
                LinkedHashMap<String, Double> result = new LinkedHashMap<String, Double>();
                result.put("original", originalArea);
                for (Field field : ChannelStack.class.getDeclaredFields()) {
                    String fieldName = field.getName();
                    try {
                        ImagePlus imp = (ImagePlus)field.get(stack);
                        result.put(fieldName, measureOriginalArea(imp));
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                ResultsTable rt = new ResultsTable();
                String title = imp.getTitle();
                for (String subtitle : result.keySet()) {
                    rt.addRow();
                    rt.addValue(Misc.$i18n("Label"), String.format("%s (%s)", title, subtitle));
                    rt.addValue("Area", result.get(subtitle));
                }
                rt.show(String.format("Measure Result for '%s'", title));
            } else {
                IJ.run(copy, "Clear Outside", null);
                ChannelStack stack = splitChannel(copy);
                // TODO: 确认这个 roi 是否符合需求: 能否正确计算 original area?
                // original area 的含义是什么？
                new ChannelAnalysingToolkitWindow(stack.red, new ImageCanvas(stack.red), (Roi) roi.clone(), "red").setVisible(true);
                new ChannelAnalysingToolkitWindow(stack.green, new ImageCanvas(stack.green), (Roi) roi.clone(), "green").setVisible(true);
                new ChannelAnalysingToolkitWindow(stack.blue, new ImageCanvas(stack.blue), (Roi) roi.clone(), "blue").setVisible(true);
            }
        }

        public double measureOriginalArea(ImagePlus imp) {
            ResultsTable rt = new ResultsTable();
            Analyzer analyzer = new Analyzer(imp, measurement, rt);
            analyzer.measure();
            double originalArea = rt.getValueAsDouble(0, 0);
            IJ.log(String.format("original_area: %s", originalArea));
            return originalArea;
        }

        public class ChannelStack {
            public ImagePlus red;
            public ImagePlus green;
            public ImagePlus blue;

            public ChannelStack(ImagePlus red, ImagePlus green, ImagePlus blue) {
                this.red = red;
                this.green = green;
                this.blue = blue;
            }
        }

        public ChannelStack splitChannel(ImagePlus imp) {
            int width = imp.getWidth();
            int height = imp.getHeight();
            ImageStack rgbStack = imp.getStack();
            ImageStack redStack = new ImageStack(width, height);
            ImageStack greenStack = new ImageStack(width, height);
            ImageStack blueStack = new ImageStack(width, height);
            int n = rgbStack.getSize();
            for (int i = 1; i <= n; i++) {
                IJ.showStatus(String.format("%d/%d", i, n));
                byte[] r = new byte[width * height];
                byte[] g = new byte[width * height];
                byte[] b = new byte[width * height];
                ColorProcessor cp = (ColorProcessor)rgbStack.getProcessor(i);
                cp.getRGB(r, g, b);
                redStack.addSlice("", r);
                greenStack.addSlice("", g);
                blueStack.addSlice("", b);
            }
            String title = imp.getTitle();
            return new ChannelStack(
              new ImagePlus(title + "(red)", redStack),
                    new ImagePlus(title + "(green)", greenStack),
                    new ImagePlus(title + "(blue)", blueStack)
            );
        }
    }
}
