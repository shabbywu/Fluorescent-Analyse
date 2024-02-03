package cn.shabbywu.imagej.analyser;

import cn.shabbywu.imagej.utils.BestThresholdStore;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ThresholdToSelection;

public class ApplyFluorescenceThresholdAnalyzer {
    private ImagePlus channelImp;
    private ImagePlus baseImp;
    private String channel;
    private Roi selectionRoi;
    private boolean thresholded;
    // Area | Mean gray value | Integrated density
    private int measurement = Analyzer.AREA | Analyzer.INTEGRATED_DENSITY | Analyzer.MEAN;
    public ApplyFluorescenceThresholdAnalyzer(ImagePlus channelImp, ImagePlus baseImp, String channel, Roi selectionRoi) {
        this.channelImp = channelImp;
        this.baseImp = baseImp;
        this.channel = channel;
        this.selectionRoi = selectionRoi;
        this.thresholded = false;
    }

    /**
     * 使用阈值二值化处理图片
     * 仅保留值在 [minThreshold, maxThreshold] 之间的内容, 目前 maxThreshold == 256
     */
    public void convertImpToMask() {
        if (this.thresholded) return;
        BestThresholdStore.BestThreshold bestThreshold = BestThresholdStore.getBestThreshold(channel);
        if (bestThreshold.minThreshold != bestThreshold.maxThreshold) {
            FluorescenceThresholder.doThreshold(channelImp, bestThreshold.minThreshold, bestThreshold.maxThreshold, false);
        }
        this.thresholded = true;
    }

    public ResultsTable measureFluorescenceArea() {
        if (!thresholded) convertImpToMask();

        channelImp.setRoi(selectionRoi);
        ResultsTable rt = new ResultsTable();
        Analyzer analyzer = new Analyzer(channelImp, measurement, rt);
        analyzer.measure();
        double originalArea = rt.getValueAsDouble(0, 0);
        rt.setLabel(channelImp.getTitle().replace(String.format("(%s)", channel), "(original)"), 0);

        // 使用 ThresholdToSelection 圈取灰度图的有效区域
        // TODO: 支持 analyze particles
        // Note: 对于二值化后的图片, Create Selection 即调用 ThresholdToSelection
        Roi fluorescenceRoi = ThresholdToSelection.run(channelImp);
        channelImp.setRoi(fluorescenceRoi);
        analyzer = new Analyzer(channelImp, measurement, rt);
        analyzer.measure();
        double fluorescenceArea = rt.getValueAsDouble(0, 1);
        rt.setLabel(channelImp.getTitle().replace(String.format("(%s)", channel), "(threshold)"), 1);

        double baseArea = 0;
        if (baseImp != null) {
            baseImp.setRoi(fluorescenceRoi);
            analyzer = new Analyzer(baseImp, measurement, rt);
            analyzer.measure();
            baseArea = rt.getValueAsDouble(0, 2);
            rt.setLabel(channelImp.getTitle().replace(String.format("(%s)", channel), "(base)"), 2);

            // 打开底图校验数据
            ImageWindow diff = new ImageWindow(baseImp);
            diff.setVisible(true);
        }

        BestThresholdStore.BestThreshold bestThreshold = BestThresholdStore.getBestThreshold(channel);
        IJ.log(String.format("original_area(%s) min_threshold=%s max_threshold=%s area=%s", channel, bestThreshold.minThreshold, bestThreshold.maxThreshold, originalArea));
        IJ.log(String.format("fluorescence(%s) min_threshold=%s max_threshold=%s area=%s", channel, bestThreshold.minThreshold, bestThreshold.maxThreshold, fluorescenceArea));
        if (baseImp != null) {

            IJ.log(String.format("base(%s) min_threshold=%s max_threshold=%s area=%s", channel, bestThreshold.minThreshold, bestThreshold.maxThreshold, baseArea));
        }
        return rt;
    }
}
