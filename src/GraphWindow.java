/**Graph Window
 * ------------
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
public class GraphWindow extends JPanel implements ActionListener {
	
    public GraphWindow(JTextArea text, String title, String axis, int numControls, ArrayList<Double> means, ArrayList<Double> stddev, ArrayList<String> type, ArrayList<String> label) {
    	super( new BorderLayout());
    	controlNumber = numControls;
    	Pmeans = means;
    	Pstddev = stddev;
    	Ptype = type;
    	Plabel = label;
    	log = text;
    	final StatisticalCategoryDataset dataset = createDatasets();
        CategoryAxis xAxis = new CategoryAxis("Interaction");
        xAxis.setLowerMargin(0.001d); // percentage of space before first bar
        xAxis.setUpperMargin(0.001d); // percentage of space after last bar
        xAxis.setCategoryMargin(0.1d); // percentage of space between categories
        xAxis.setMaximumCategoryLabelWidthRatio((float) 10);
        final ValueAxis yAxis = new NumberAxis(axis);
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);

        // define the plot
        final CategoryItemRenderer renderer = new StatisticalBarRenderer();
        plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
        chart = new JFreeChart(title,plot);
        chart.setBackgroundPaint(Color.white);

        // add the chart to a panel...
        chartPanel = new ChartPanel(chart, true, true, false, true, false);
        chartPanel.setPreferredSize(new java.awt.Dimension(700, 300));
        chartPanel.setMinimumDrawWidth(20*dataset.getRowCount());
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
        
        JPanel buttonPanel = new JPanel();
        dataPagesOptionBox = new JComboBox();
        dataPagesOptionBox.addItem("All");
        int numPages = dataPages.size();
        for (int i = 0; i < numPages - 1; i++)
        	dataPagesOptionBox.addItem("Page " + (i+1) + "/" + (numPages-1));
        dataPagesOptionBox.addActionListener(this);
        save = new JButton("Save");
        saveName = new JTextField(20);
        save.addActionListener(this);
        buttonPanel.add(new JLabel("Graphing options: "));
        buttonPanel.add(dataPagesOptionBox);
        buttonPanel.add(new JLabel("Enter save name:"));
        buttonPanel.add(saveName);
        buttonPanel.add(save);
        
        add(chartPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.PAGE_START);
    }

    private StatisticalCategoryDataset createDatasets() {
    	dataPages = new ArrayList<StatisticalCategoryDataset>();
        final DefaultStatisticalCategoryDataset all = new DefaultStatisticalCategoryDataset(); 
        DefaultStatisticalCategoryDataset page= new DefaultStatisticalCategoryDataset(); 
        for (int i = 0; i < Pmeans.size(); i++) {
        	if ( i != 0 && i % (INTERACTIONS_PER_PAGE*(7)) == 0) {
        		dataPages.add(page);
        		page = new DefaultStatisticalCategoryDataset();
        	}
        	page.add(Pmeans.get(i), Pstddev.get(i), Ptype.get(i), Plabel.get(i));
        	all.add(Pmeans.get(i), Pstddev.get(i), Ptype.get(i), Plabel.get(i));
        }
        dataPages.add(0, all);
        return all;
    }
       
    public void actionPerformed(ActionEvent e) {
    	Object source = e.getSource();
    	if (source == dataPagesOptionBox) {
    		plot.setDataset(dataPages.get(dataPagesOptionBox.getSelectedIndex()));
    		chartPanel.validate();
    	} else if (source == save) {
    		saveWindow();
    	}
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
    	    		log.append("Please enter a valid name under which to save the graph. (Do not use symbols: '/'.,'\')");
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
    
    // CONTANTS And Globals
    private static int INTERACTIONS_PER_PAGE = 30;
    
    private int controlNumber;
	private JTextArea log;
    private CategoryPlot plot;
    private JFreeChart chart;
    private JComboBox dataPagesOptionBox;
    private ArrayList<StatisticalCategoryDataset> dataPages;
    private ArrayList<Double> Pmeans;
    private ArrayList<Double> Pstddev;
    private ArrayList<String> Ptype;
    private ArrayList<String> Plabel;
    private ChartPanel chartPanel;
    private JButton save;
    private JTextField saveName;
    
}