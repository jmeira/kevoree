package org.kevoree.library.android.agrapher;


import java.util.ArrayList;
import java.util.LinkedList;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 09/11/11
 * Time: 10:43
 * To change this template use File | Settings | File Templates.
 */

public class GraphLine {


    private XYMultipleSeriesDataset StatsDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer StatsRenderer = new XYMultipleSeriesRenderer();

    private GraphicalView StatsChartView;

    private LinkedList<Double> data = new LinkedList<Double>();

    private XYSeries series;
    private  int max_points =60;
    private int color_axes = Color.GRAY;
    private int color_courbe = Color.RED;
    /**
     *
     * @param title titre de la courbe
     * @param _max_points nombre maximum de point
     * @param _color_axes couleur de l'axe
     * @param _color_courbe couleur de la courbe
     */
    public GraphLine(String title,int _max_points,int _color_axes,int _color_courbe){

        this.max_points = _max_points;
        series= new XYSeries(title);


        this.color_axes =_color_axes;
        this.color_courbe =_color_courbe;
    }


    public GraphicalView CreateView(Context context){

        StatsRenderer.setAxesColor(color_axes);
        StatsRenderer.setXAxisMax(max_points);

        StatsDataset.addSeries(series);
        XYSeriesRenderer renderer = new XYSeriesRenderer();
        renderer.setColor(color_courbe);
        renderer.setLineWidth(3);

        StatsRenderer.addSeriesRenderer(renderer);

        StatsChartView =ChartFactory.getLineChartView(context, StatsDataset,StatsRenderer);

        return StatsChartView;
    }

    public void refreshLine() {
        try {
        series.clear();
        int index = 0;

        StringBuffer s = new StringBuffer();
        for (int h = 0; h < series.getItemCount(); h++)
            s.append(series.getY(h)).append(",");

        for (int i = (data.size() - 1); i >= 0 && index < max_points; i--) {

            series.add(index++, data.get(i));
        }

        if(StatsChartView !=null)
            StatsChartView.repaint();
        }catch (Exception e)
        {
                 e.printStackTrace();
        }
    }

    public void add(double value)
    {

        if (data.size() > max_points) {
            data.removeFirst();
        }
        data.addLast(value);
        refreshLine();


    }

}