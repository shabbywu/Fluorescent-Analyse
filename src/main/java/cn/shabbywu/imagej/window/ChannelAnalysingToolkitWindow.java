package cn.shabbywu.imagej.window;

import cn.shabbywu.imagej.analyser.ApplyFluorescenceThresholdAnalyzer;
import cn.shabbywu.imagej.analyser.FluorescenceThresholder;
import cn.shabbywu.imagej.utils.BestThresholdStore;
import cn.shabbywu.imagej.utils.Misc;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.ResultsTable;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChannelAnalysingToolkitWindow extends ImageWindow {
    public Roi selectionRoi;
    public String channel;
    protected ImagePlus impDuplicated;
    protected ImagePlus baseImp;
    private Button measureButton;
    private Label bestThresholdLabel;
    public ChannelAnalysingToolkitWindow(ImagePlus imp, ImageCanvas ic, Roi selectionRoi, String channel) {
        super(imp, ic);
        this.impDuplicated = imp.duplicate();
        this.impDuplicated.setTitle(imp.getTitle());
        this.selectionRoi = selectionRoi;
        this.channel = channel;
        setFont(Misc.GetFont());
        initBottomPanel();
        resizeWindow();
        refreshBestThresholdLabel();
    }

    void initBottomPanel() {
        Panel panel1 = new Panel();
        Panel panel2 = new Panel();
        Label baseImpNameLabel = new Label("");
        bestThresholdLabel = new Label("");

        Button chooseBaseImageButton = new Button(Misc.$i18n("选择单通道底图"));
        chooseBaseImageButton.addActionListener(e -> {
            OpenDialog od = new OpenDialog(Misc.$i18n("选择一张图片"), null);
            String fileName = od.getFileName();
            if (fileName != null && !fileName.isEmpty()) {
                baseImp = new Opener().openImage(od.getDirectory(), fileName);
            }
            if (baseImp != null) {
                baseImpNameLabel.setText(Misc.$i18n("当前底图: ") + baseImp.getTitle());
                baseImpNameLabel.revalidate();
                baseImpNameLabel.repaint();
            }
        });
        panel1.add(chooseBaseImageButton);
        panel1.add(baseImpNameLabel);

        panel2.setLayout(new FlowLayout(FlowLayout.RIGHT));
        Button resetButton = new Button(Misc.$i18n("重置"));
        resetButton.addActionListener(e -> getImagePlus().setProcessor(impDuplicated.duplicate().getProcessor()));
        panel2.add(resetButton);

        Button findThresholdButton = new Button(Misc.$i18n("测量最佳阈值"));
        findThresholdButton.addActionListener(new BestFluorescenceThresholdDetector(this));
        panel2.add(findThresholdButton);

        measureButton = new Button(Misc.$i18n("测量荧光面积和强度"));
        measureButton.addActionListener(e -> doMeasure());
        panel2.add(measureButton);

        add(bestThresholdLabel);
        add(panel1);
        add(panel2);
        refreshBestThresholdLabel();
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

    /**
     *  刷新最佳阈值文案
     */
    public void refreshBestThresholdLabel() {
        BestThresholdStore.BestThreshold bestThreshold = BestThresholdStore.getBestThreshold(this.channel);
        String extraMessage = String.format("min=%d, max=%d", bestThreshold.minThreshold, bestThreshold.maxThreshold);
        bestThresholdLabel.setText(Misc.$i18n("最佳阈值: ") + extraMessage);
        bestThresholdLabel.revalidate();
        bestThresholdLabel.repaint();
    }

    /**
     * 执行测量操作
     */
    public void doMeasure() {
        ImagePlus copy = impDuplicated.duplicate();

        ApplyFluorescenceThresholdAnalyzer analyzer = new ApplyFluorescenceThresholdAnalyzer(copy, baseImp, channel, selectionRoi);
        analyzer.convertImpToMask();
        ResultsTable rt = analyzer.measureFluorescenceArea();
        rt.show(String.format("Channel(%s) Measure Result", channel));
        getImagePlus().setProcessor(copy.getProcessor());
    }

    public class BestFluorescenceThresholdDetector implements ActionListener {
        ImagePlus channelImp;
        ChannelAnalysingToolkitWindow parent;
        public BestFluorescenceThresholdDetector(ChannelAnalysingToolkitWindow parent) {
            this.channelImp = parent.impDuplicated.duplicate();
            this.parent = parent;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            generateAdjustImageStack();
        }

        public void generateAdjustImageStack() {
            String title = channelImp.getTitle();
            int width = channelImp.getWidth();
            int height = channelImp.getHeight();

            ImageStack thresholdStack = new ImageStack(width, height);
            for(int i=1; i <= 255; i++) {
                ImagePlus copy = this.channelImp.duplicate();
                FluorescenceThresholder.doThreshold(copy, i, 256);
                thresholdStack.addSlice(String.format("fluorescence(%s) max_threshold: %d", channel, i), copy.getProcessor());
            }
            new ChannelThresholdStackWindow(new ImagePlus(title, thresholdStack), this.parent).setVisible(true);
        }
    }
}
