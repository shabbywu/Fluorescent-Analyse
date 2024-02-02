package cn.shabbywu.imagej.analyser;

import cn.shabbywu.imagej.utils.BestThresholdStore;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ThresholdToSelection;

public class ApplyFluorescenceThresholdAnalyzer {
    private ImagePlus channelImp;
    private String channel;
    private Roi selectionRoi;
    private boolean thresholded;
    // Area | Mean gray value | Integrated density
    private int measurement = Analyzer.AREA | Analyzer.INTEGRATED_DENSITY | Analyzer.MEAN;
    public ApplyFluorescenceThresholdAnalyzer(ImagePlus channelImp, String channel, Roi selectionRoi) {
        this.channelImp = channelImp;
        this.channel = channel;
        this.selectionRoi = selectionRoi;
        this.thresholded = false;
    }

    public void ConvertImpToMask() {
        if (this.thresholded) return;
        BestThresholdStore.BestThreshold bestThreshold = BestThresholdStore.getBestThreshold(channel);
        if (bestThreshold.minThreshold != bestThreshold.maxThreshold) {
            FluorescenceThresholder.doThreshold(channelImp, bestThreshold.minThreshold, bestThreshold.maxThreshold);
        }
        this.thresholded = true;
    }

    public ResultsTable measureFluorescenceArea() {
        if (!thresholded) ConvertImpToMask();
        channelImp.setRoi(selectionRoi);
        ResultsTable rt = new ResultsTable();
        // TODO: 根据需求重新设置 图片和测量方法
        Analyzer analyzer = new Analyzer(channelImp, measurement, rt);
        analyzer.measure();
        rt.setLabel(channelImp.getTitle().replace(String.format("(%s)", channel), "(original)"), 0);
        double originalArea = rt.getValueAsDouble(0, 0);

        // 使用 ThresholdToSelection 圈取灰度图的有效区域
        Roi fluorescenceRoi = ThresholdToSelection.run(channelImp);
        channelImp.setRoi(fluorescenceRoi);
        // TODO: 根据需求重新设置 图片和测量方法
        analyzer = new Analyzer(channelImp, measurement, rt);
        analyzer.measure();
        double fluorescenceArea = rt.getValueAsDouble(0, 1);

        BestThresholdStore.BestThreshold bestThreshold = BestThresholdStore.getBestThreshold(channel);
        IJ.log(String.format("original_area(%s) min_threshold=%s max_threshold=%s area=%s", channel, bestThreshold.minThreshold, bestThreshold.maxThreshold, originalArea));
        IJ.log(String.format("fluorescence(%s) min_threshold=%s max_threshold=%s area=%s", channel, bestThreshold.minThreshold, bestThreshold.maxThreshold, fluorescenceArea));
        return rt;
    }
}