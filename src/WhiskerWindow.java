/**WhiskerWindow
 * -------------
 * Displays the data visually using a statistical graph.
 */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.statistics.*;


@SuppressWarnings("serial")
public class WhiskerWindow extends JPanel implements ActionListener {
	
    public WhiskerWindow(JTextArea text, String title, String axis, ArrayList<Double> v, 
    		ArrayList<Double> empty, ArrayList<Double> YFP, ArrayList<Double> mock) {
    	super( new BorderLayout());
    	values = v;
    	Bempty = empty;
    	BYFP = YFP;
    	Bmock = mock;
    	final BoxAndWhiskerCategoryDataset dataset = createDataset(title);
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLowerMargin(0.001d); // percentage of space before first bar
        xAxis.setUpperMargin(0.001d); // percentage of space after last bar
        xAxis.setCategoryMargin(0.1d); // percentage of space between categories
        xAxis.setMaximumCategoryLabelWidthRatio((float) 10);
        final ValueAxis yAxis = new NumberAxis(axis);
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        // define the plot
        final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(true);
        renderer.setSeriesPaint(0, Color.LIGHT_GRAY);
        renderer.setUseOutlinePaintForWhiskers(true);
        renderer.setMaximumBarWidth(0.10);
        renderer.setMeanVisible(false);
        plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
        //plot.setOrientation(PlotOrientation.HORIZONTAL);
        chart = new JFreeChart(title,plot);
        chart.removeLegend();
        chart.setBackgroundPaint(Color.white);
    
        // add the chart to a panel...
        chartPanel = new ChartPanel(chart, true, true, false, true, false);
        chartPanel.setPreferredSize(new java.awt.Dimension(300, 600));
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
        chartPanel.setAutoscrolls(true);
        chartPanel.setMinimumDrawWidth(20*dataset.getRowCount());

        JPanel buttonPanel = new JPanel();
        save = new JButton("Save");
        saveName = new JTextField(20);
        save.addActionListener(this);
        buttonPanel.add(new JLabel("Enter save name:"));
        buttonPanel.add(saveName);
        buttonPanel.add(save);
        add(chartPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.PAGE_START);
    }

    private BoxAndWhiskerCategoryDataset createDataset(String title) {
    	 final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        dataset.add(values, title, "Bait+Prey");

        if (!Bempty.isEmpty()) dataset.add(Bempty, title, "Empty Controls");
        if (!BYFP.isEmpty()) dataset.add(BYFP, title, "YFP Controls");
        if (!Bmock.isEmpty()) dataset.add(Bmock, title, "Mock Controls");
        return dataset;
    }
       
    public void actionPerformed(ActionEvent e) {
    	Object source = e.getSource();
    	if (source == save) saveWindow();
    }

    private void saveWindow() {
    	try  {
            BufferedImage image = new BufferedImage(chartPanel.getWidth(), chartPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = image.createGraphics();
            chartPanel.paint(graphics2D);
            JFileChooser chooser = new JFileChooser();
    		chooser.setApproveButtonText("Choose Folder");
    		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    	    int returnVal = chooser.showOpenDialog(this);
    	    if (returnVal == JFileChooser.APPROVE_OPTION) {
    	    	File directory = chooser.getSelectedFile();
    	    	String name = saveName.getText().trim();
    	    	if (name.isEmpty() || name.contains(".")) {
    	    		log.append("Please enter a valid name under which to save the graph. (Do not include file type in save name.)");
    	    		return;
    	    	}
    	    	ImageIO.write(image,"jpeg", new File(directory.getAbsolutePath() + "/" + name + ".jpeg"));
    	    	saveName.setText("");
    	    } else {
    	    	log.append("Please try again.\n");
    	    	return;
    	    }
    	} catch(Exception exception) {
            log.append("Error: Unable to save.\n");    
    	}
    }
	private JTextArea log;
    private CategoryPlot plot;
    private JFreeChart chart;
    private ArrayList<Double> values;
    private ArrayList<Double> Bempty;
    private ArrayList<Double> BYFP;
    private ArrayList<Double> Bmock;
    private ChartPanel chartPanel;
    private JButton save;
    private JTextField saveName;
    
}