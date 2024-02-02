package cn.shabbywu.imagej.window;

import cn.shabbywu.imagej.analyser.ApplyFluorescenceThresholdAnalyzer;
import cn.shabbywu.imagej.utils.BestThresholdStore;
import cn.shabbywu.imagej.utils.Misc;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.StackWindow;
import ij.measure.ResultsTable;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 分析通道最佳阈值的窗口
 */
public class ChannelThresholdStackWindow extends StackWindow {
    ChannelAnalysingToolkitWindow channelAnalysisWindow;
    String channel;

    public ChannelThresholdStackWindow(ImagePlus imp, ChannelAnalysingToolkitWindow channelAnalysisWindow) {
        super(imp);
        this.channelAnalysisWindow = channelAnalysisWindow;
        channel = channelAnalysisWindow.channel;
        setFont(Misc.GetFont());
        initBottomPanel();
        resizeWindow();
    }

    void initBottomPanel(){
        Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        Button setThresholdButton = new Button("设置最佳阈值(Dark background B&W)");
        setThresholdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GenericDialog gd = new GenericDialog("设置最佳阈值(Dark background B&W)");
                gd.setFont(Misc.GetFont());
                gd.addNumericField("Lower threshold level: ", getImagePlus().getSlice(), 0);
                gd.addNumericField("Upper threshold level: ", 255, 0);
                gd.showDialog();
                if (gd.wasCanceled()) return;

                double minThreshold = gd.getNextNumber();
                double maxThreshold = gd.getNextNumber();

                // 设置最佳阈值、测量
                BestThresholdStore.setBestThreshold(channel, new BestThresholdStore.BestThreshold(((int) minThreshold), (int) maxThreshold));
                channelAnalysisWindow.refreshMeasureButton();
                channelAnalysisWindow.doMeasure();
                close();
            }
        });
        panel.add(setThresholdButton);
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
}
