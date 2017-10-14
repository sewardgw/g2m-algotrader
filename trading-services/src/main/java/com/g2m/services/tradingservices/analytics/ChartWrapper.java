package com.g2m.services.tradingservices.analytics;

import java.awt.Color;
import java.awt.Shape;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RefineryUtilities;
import org.jfree.ui.TextAnchor;

import com.g2m.services.tradingservices.analytics.AnalyticsGraphs.IncrementalEquity;


public class ChartWrapper   {
	XYDataset dataset;
	JFreeChart chart;
	ChartPanel chartPanel;
	String title;
	SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");

	public ChartWrapper(String title) {
		//super(title);
		this.title = title;

	}

	/**
	 * Creates a sample dataset.
	 * 
	 * @return a sample dataset.
	 */
	public void createChartFromEquityUpdates(List<IncrementalEquity> ie) {

		//final XYSeries time = new XYSeries("Time");
		final XYSeries profit = new XYSeries("Realized Profit");
		final XYSeries comissions = new XYSeries("Comissions");
		final XYSeries profitAndComissions = new XYSeries("Profit With Comissions");

		double profitTotal = 0;
		double comissionTotal =0;
		double profitAndComissionTotal = 0;
		
		for(IncrementalEquity i : ie){
			profitTotal += i.getRealizedProfit();
			comissionTotal += i.getComissionCosts();
			profitAndComissionTotal += i.getRealizedProfit() - i.getComissionCosts();
			
			profit.add(i.getCloseTime(), profitTotal);
			comissions.add(i.getCloseTime(), comissionTotal);
			profitAndComissions.add(i.getCloseTime(), profitAndComissionTotal);
		}

		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(profit);
		dataset.addSeries(comissions);
		dataset.addSeries(profitAndComissions);

		this.dataset = dataset;

	}

	/**
	 * Creates a chart.
	 * 
	 * @param dataset  the data for the chart.
	 * 
	 * @return a chart.
	 */
	public void createChart() {

		// create the chart...
		final JFreeChart chart = ChartFactory.createTimeSeriesChart(
				"Equity Chart",      // chart title
				"Date",                      // x axis label
				"Total $",                   // y axis label
				dataset,                  // data
				//PlotOrientation.VERTICAL,
				true,                     // include legend
				true,                     // tooltips
				false                     // urls
				);

		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
		chart.setBackgroundPaint(Color.white);

		//	    final StandardLegend legend = (StandardLegend) chart.getLegend();
		//      legend.setDisplaySeriesShapes(true);

		// get a reference to the plot for further customisation...
		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.lightGray);
		//    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(sdf);
		
		// add a labelled marker for the target price...
        final Marker target = new ValueMarker(0.0);
        target.setPaint(Color.red);
        target.setLabel("Break Even");
        target.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        target.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
        target.setOutlinePaint(Color.red);
        plot.addRangeMarker(target);
		//plot.addDomainMarker(marker);
		
		
		Shape[] cross = DefaultDrawingSupplier.createStandardSeriesShapes();

		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		
		renderer.setSeriesShape(0, cross[0]);
		renderer.setSeriesShape(2, cross[1]);
		
		
		//renderer.setSeriesLinesVisible(0, false);
		renderer.setSeriesShapesVisible(1, false);
		plot.setRenderer(renderer);

		// change the auto tick unit selection to integer units only...
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// OPTIONAL CUSTOMISATION COMPLETED.

		this.chart = chart;

	}

	public void createChartPanel() {
		chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(1080, 720));
		//setContentPane(chartPanel);
	}

	public void save(String location){

		File lineChartFile = new File( location + System.getProperty("file.separator") + "EquityGraph.jpeg" ); 
		
		try {
			//lineChartFile.createNewFile();
			ChartUtilities.saveChartAsJPEG(lineChartFile ,chart, 1080 , 720);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




}
