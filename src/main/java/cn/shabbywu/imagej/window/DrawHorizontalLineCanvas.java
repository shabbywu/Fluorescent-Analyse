package cn.shabbywu.imagej.window;

import cn.shabbywu.imagej.utils.Misc;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.measure.Calibration;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;

public class DrawHorizontalLineCanvas extends ImageCanvas implements MouseListener, MouseMotionListener {
    Point firstPoint = null;

    public DrawHorizontalLineCanvas(ImagePlus imp) {
        super(imp);
        Overlay overlay = getOverlay();
        if (overlay != null) {
            overlay.remove("DrawHorizontalLine");
            getImage().draw();
        } else {
            imp.setOverlay(new Overlay());
        }
    }

    public void mouseEntered(MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void mousePressed(MouseEvent e) {
        firstPoint = e.getPoint();
    }

    public void mouseDragged(MouseEvent e) {
        if (firstPoint == null) return;
        Overlay overlay = getOverlay();
        overlay.remove("DrawHorizontalLine");

        GeneralPath path = new GeneralPath();
        path.moveTo(offScreenXD(firstPoint.x), offScreenYD(firstPoint.y));
        path.moveTo(offScreenXD(e.getX()), offScreenYD(e.getY()));
        Roi roi = new ShapeRoi(path);
        roi.updateWideLine(4);
        overlay.add(roi, "DrawHorizontalLine");
        getImage().setOverlay(overlay);
    }

    public void mouseReleased(MouseEvent e) {
        if (firstPoint == null) return;

        double distance = Math.abs(offScreenXD(e.getX()) - offScreenX(firstPoint.x));
        double known = 64;
        String unit = "μm";

        GenericDialog gd = new GenericDialog("设置比例尺");
        gd.setFont(Misc.GetFont());
        gd.addNumericField("Distance in pixels: ", distance, 3);
        gd.addNumericField("Known distance: ", known, 3);
        gd.addStringField("Unit of length: ", unit);
        gd.showDialog();

        Overlay overlay = getOverlay();
        overlay.remove("DrawHorizontalLine");
        getImage().setOverlay(overlay);
        if (gd.wasCanceled()) {
            firstPoint = null;
            return;
        }

        distance = gd.getNextNumber();
        known = gd.getNextNumber();
        unit = gd.getNextString();
        IJ.log((String.format("distance=%.3f known=%.2f unit=%s global", distance, known, unit)));

        Calibration cal = new Calibration(imp);
        cal.pixelWidth = known / distance;
        cal.pixelHeight = known / distance;
        cal.setUnit(unit);
        imp.setCalibration(cal);
        imp.setGlobalCalibration(cal);
        imp.repaintWindow();
        firstPoint = null;
        imp.getWindow().close();
    }
}
