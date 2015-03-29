/**AnalyzeFormatted
 * ----------
 * This class provides the functionality of reading in a formatted data workbook and allowing the user
 * to visualize the data from this workbook or to pick specific interactions to further analyze. The workbook
 * must contain a RawData sheet in which the formatted data is located. These data located on this sheet is then
 * used to a) create bar charts or box & whisker plots of the data or b) to choose interactions to transfer to a
 * new workbook with a RawData sheet containing only those specific interactions. This new sheet may then be used
 * to visualize the data of your choice. NOTE: Unbelievably incompetent JExel package with which I read from and
 * write to the date sheets does not allow for copying all digits of a value: all values are rounded to the
 * nearest thousandth, 0.001 precision.
 */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.util.*;

import jxl.*;
import jxl.read.biff.BiffException;
import jxl.write.*;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.biff.RowsExceededException;


public class AnalyzeFormatted implements ActionListener {
	/* constrctor*/
	public AnalyzeFormatted(AnalyzeResults panel, JTextArea text, JPanel buttonPanel) {
		log = text;
		mainPanel = panel;
		buttons = buttonPanel;
		chooseFile = new JButton("Open Formatted Data File");
		specificButton = new JButton("Create Workbook with Specific Data");
		generalButton = new JButton("Visualize Data");
		specificButton.addActionListener(this);
		generalButton.addActionListener(this);
		chooseFile.addActionListener(this);
		buttons.add(chooseFile);
	}
	
	/* 
	 * Detects when a button is pushed and reacts appropriately.
	 */
	public void actionPerformed(ActionEvent g) {
		Object source = g.getSource();
		if (source == chooseFile) {
			chooser = new JFileChooser();
			chooser.setApproveButtonText("Open");
			final FileNameExtensionFilter filter = new FileNameExtensionFilter(
			        "Excel File", "xls");
		    chooser.setFileFilter(filter);
		    int returnVal = chooser.showOpenDialog(mainPanel);
		    	if (returnVal == JFileChooser.APPROVE_OPTION) {
		    		file = chooser.getSelectedFile();
					buttons.remove(chooseFile);
					mainPanel.validate();
					buttons.add(generalButton);
					buttons.add(specificButton);
					mainPanel.validate();
		    	} else {
		    		log.append("Please select file.\n"); // If cancel is pressed in the window to open the excel file.
		    	}
		} else if (source == specificButton) {
			isGraphingGeneral = false;
			createGUI();
			openFile();
		} else if (source == generalButton){
			isGraphingGeneral = true;
			createGUI();
		} else if (source == search && !vectorField.getText().trim().isEmpty()) {
			String searched = vectorField.getText();
			interactionsToChooseFrom = findInteractionsWith(searched.trim().toLowerCase());
			if (interactionsToChooseFrom.isEmpty()) log.append("No interactions found containing " + searched + "\n");
			else searchToSelection();
		} else if (source == clearAll) {
			selectedInteractions.clear();
			log.append("Interaction selections cleared\nChosen interactions displayed below:\n");
		} else if (source == selectInteractionButton) {
			setInteractionAsSelected();
		} else if (source == backToSearch) {
			selectionToSearch();
		} else if (source == finalizeSheet) {
			if (isGraphingGeneral) {
				openFile();
				makeGraphs(workbook, dataSheet);
			} else	{
				if (sheetNameField.getText().isEmpty() || sheetNameField.getText().contains("."))
					log.append("Please enter a valid save name and re-submit.\n");
				else {
					WritableWorkbook specificBook = prepareNewWorkbook();
					try {
						specificBook.write();
						specificBook.close();
					} catch (WriteException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	//************* FUNCTIONS FOR GRAPHING ONLY A SELECT FEW INTERACTIONS ***************
	
	/* Changes menu options from selecting from a few options of interactions to the menu
	 * in which the user may type a search option.
	 */
	private void selectionToSearch() {
		mainPanel.remove(selectionPanel);
		mainPanel.validate();
		mainPanel.add(searchPanel, BorderLayout.PAGE_END);
		mainPanel.validate();
	}
	
	/* Changes menu options from directly inputing the search to selecting from
	 * options that match that search
	 */
	private void searchToSelection() {
		selectionMenu.removeAllItems();
		Iterator<String> it = interactionsToChooseFrom.iterator();
		while (it.hasNext())
			selectionMenu.addItem(it.next());
		vectorField.setText("");
		mainPanel.remove(searchPanel);
		mainPanel.validate();
		mainPanel.add(selectionPanel, BorderLayout.PAGE_END);
		mainPanel.validate();
	}
	
	/* When an item is selected it is then appended to the log as well as
	 * placed in the set of selectedInteractions.
	 */
	private void setInteractionAsSelected() {
		String selected = (String)selectionMenu.getSelectedItem();
		selectedInteractions.add(selected);
		log.append(selected + "\n");
	}
	
	
	/* Performs the function of searching for interactions that contain
	 * characters  (in order) that the user inputs to search by.
	 */
	private Set<String> findInteractionsWith(String target) {
		Set<String> toReturn = new HashSet<String>();
		int row = 1;
		while(true) {
			String interaction = dataSheet.getCell(0, row).getContents();
			String comment = dataSheet.getCell(3, row).getContents();
			if (interaction.isEmpty()) break;
			// Included in the options to choose if the interaction matches the name in the 
			// interaction and if the interaction is a sample as opposed to a controls
			// as the controls are automatically included in the new sheet if an interaction
			// is selected.
			if (interaction.toLowerCase().contains(target) && !comment.contains("ontrol"))
				toReturn.add(interaction);
			row++;
		}
		return toReturn;
	}
	
	/* Creates a worksheet where data from selected interactions will be placed.
	 * The worksheet will be named as specified by the sheetNameField.
	 */
	private WritableWorkbook prepareNewWorkbook() {
		Set<Integer> rowsAddedToNewSheet = new HashSet<Integer>();
		WritableWorkbook specificDataBook = createNewBook(sheetNameField.getText().trim());
		WritableSheet specificDataSheet = specificDataBook.createSheet("RawData", 0);
		Iterator<String> it = selectedInteractions.iterator();
		copyRow(specificDataSheet, 0, rowsAddedToNewSheet);
		while (it.hasNext()) copyInteractionAndControls(specificDataSheet, it.next(), rowsAddedToNewSheet);
		return specificDataBook;
	}
	
	private void copyInteractionAndControls(WritableSheet newSheet, String interaction, Set<Integer> rowsAlreadyAdded) {
		String[] controls = {"empty", "yfp", "mock"};
		int row = 1;
		while (true) {
			String currInteraction = dataSheet.getCell(0, row).getContents();
			if (currInteraction.isEmpty()) break; // shouldn't ever happen... but just in case put this here
			if (currInteraction.equals(interaction)) {
				for (int j = 0; j < 3; j++)
					copyRow(newSheet, row+j, rowsAlreadyAdded);
				for (int i = 0; i < controls.length; i++) {
					int baitControlRow = getRowFor(dataSheet.getCell(1,row).getContents(), controls[i], row);
					int preyControlRow = getRowFor(controls[i], dataSheet.getCell(2,row).getContents(), row);
					if (baitControlRow != 0)
						for (int j = 0; j < 3; j++)
							copyRow(newSheet, baitControlRow + j, rowsAlreadyAdded);
					if (preyControlRow != 0)
						for (int j = 0; j < 3; j++)
							copyRow(newSheet, preyControlRow + j, rowsAlreadyAdded);
				}
				break;
			}
			row++;
		}
	}
	
	private void copyRow(WritableSheet newSheet, int row, Set<Integer> rowsAlreadyAdded) {
		if (rowsAlreadyAdded.contains(row)) return; // do not include if a control is not found or if the control has already been included
		int col = 0;
		int rowInNew = rowsAlreadyAdded.size();
		while (true) {
			String cellContents = dataSheet.getCell(col, row).getContents();
			if (dataSheet.getCell(col, 0).getContents().isEmpty()) break;
			if (!cellContents.isEmpty()) {
				try {
					char[] charArray = cellContents.toCharArray();
					// if first and last character of contents are digits format as number otherwise as label
					if (Character.isDigit(charArray[0]) && Character.isDigit(charArray[charArray.length-1])) {
						Number cell = new Number(col, rowInNew, Double.parseDouble(cellContents));
						newSheet.addCell(cell);
					} else {
						Label cell = new Label(col, rowInNew, cellContents);
						newSheet.addCell(cell);
					}
				} catch (RowsExceededException e) {
					e.printStackTrace();
				} catch (WriteException e) {
					e.printStackTrace();
				}
			}
			col++;
		}
		rowsAlreadyAdded.add(row);
	}
	
	private WritableWorkbook createNewBook(String name) {
		log.append("Created new workbook under the name \"" + name + "\"\n");
		log.append("To visialize this data:\n1) Open and save the file externally.\n2) Return to Main Menu and click Analyze Results from Formatted Data." +
				"\n3) Open " + name + " and choose to Visualize Data.\n");
		try {
			return Workbook.createWorkbook(new File(file.getParentFile().getPath() + "/" + name + ".xls"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//******************* FUNCTIONS ESSENTIAL FOR LOADING INFORMATION FROM THE FILE ***************************
	private void openFile() {
	    try {
	    	Workbook in = Workbook.getWorkbook(file);
			workbook = Workbook.createWorkbook(new File(file.getParentFile().getPath() + "/LUMIER_GraphData.xls"), in);
			dataSheet = findDataSheet();
			if (dataSheet == null)
				log.append("File does not contain Raw Data sheet.\n"); // in the event that no raw data sheet is found.
		// Stupid mandatory errors to catch when reading in a file
		} catch (BiffException e) {
			log.append("Error: Empty or invalid file selected.\n");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Within the excel file searches for the raw data sheet by searching within the names for
	 * "raw". Returns the sheet if found, and otherwise returns null.
	 */
	private Sheet findDataSheet() {
		Sheet[] allSheets = workbook.getSheets();
		log.append("Successfully opened "+ file.getName() + "\n");
		for (int i = 0; i < allSheets.length; i++) 
			if (allSheets[i].getName().toLowerCase().contains("raw")) return allSheets[i];
		log.append("No Raw Data worksheet found within " + file.getName() + "\n");
		return null;
	}
	
	
	private void createGUI() {
		buttons.remove(generalButton);
		buttons.remove(specificButton);
	
		finalizeSheet = new JButton("Submit");
		finalizeSheet.addActionListener(this);
		buttons.add(finalizeSheet);
		mainPanel.validate();
		
		if (!isGraphingGeneral) {
			// FOR CREATING A NEW SHEET AND INITIALIZING SEARCH FOR INTERACTIONS TO FILL SHEET WITH
			sheetNameField = new JTextField(20);
			saveSheetLabel = new JLabel("Save as:");
			buttons.add(saveSheetLabel);
			buttons.add(sheetNameField);
			
			// FOR SELECTING INTERACTIONS AFTER SEARCHED FOR
			selectionPanel = new JPanel();
			selectionMenu = new JComboBox();
			selectInteractionButton = new JButton("Select");
			backToSearch = new JButton("Back/Continue");
			selectInteractionButton.addActionListener(this);
			backToSearch.addActionListener(this);
			selectionPanel.add(new JLabel("Select interaction: "));
			selectionPanel.add(selectionMenu);
			selectionPanel.add(selectInteractionButton);
			selectionPanel.add(backToSearch);
			
			// FOR SEARCHING FOR INTERACTIONS
			JLabel label = new JLabel("Search for vector: ");
			searchPanel = new JPanel();
			vectorField = new JTextField(20);
			search = new JButton("Search");
			clearAll = new JButton("Clear All Selected");
			search.addActionListener(this);
			clearAll.addActionListener(this);
			searchPanel.add(label);
			searchPanel.add(vectorField);
			searchPanel.add(search);
			searchPanel.add(clearAll);
			mainPanel.add(searchPanel, BorderLayout.PAGE_END);
			mainPanel.validate();
			
			selectedInteractions = new HashSet<String>();
			
		} else {
			//FOR GRAPHING ALL DATA
			graphOptions = new JPanel();
			graphOptions.setLayout(new BoxLayout(graphOptions, BoxLayout.PAGE_AXIS));
			String[] barStrings = {"Bait expression", "Prey expression", "Bait IP", "Prey CoIP", "IP Efficieny", "CoIP Efficieny", 
					"Bait IP norm.", "Prey CoIP norm.", "CoIP norm to IP"};
			String[] whiskerStrings = {"Bait expression", "Prey expression", "Bait IP", "Prey CoIP", "IP Efficieny", "CoIP Efficieny", 
					"Bait IP norm.", "Prey CoIP norm."};
			
			doNotOpenGraphsBox = new JCheckBox(" <-Suppress graph windows");
			barChartList = new JList(barStrings);
			whiskerPlotList = new JList(whiskerStrings);
			graphOptions.add(new JLabel("Bar chart options:"));
			graphOptions.add(barChartList);
			graphOptions.add(new JLabel("Box & whisker plot options:"));
			graphOptions.add(whiskerPlotList);
			graphOptions.add(doNotOpenGraphsBox);
			mainPanel.add(graphOptions, BorderLayout.EAST);
			mainPanel.validate();
		}
	}
	
	
	
	//************************************** GRAPHING FUNCTIONALITY!!!! ***********************************
	/* Plots and displays all graphs that the user selects.*/
	
	private void makeGraphs(WritableWorkbook book, Sheet sheet) {
		int[] selectedBarIndices = barChartList.getSelectedIndices();
		int[] selectedWhiskerIndices = whiskerPlotList.getSelectedIndices();
		String[] metricsToGraph = {"reniin", "firein", "reniout", "fireout",  "ip_efficiency", "coip_efficiency", "ip", "coip", "ex/ip"}; // labels in the excel file
		String[] axisLabels = {"Luminescence", "Luminescence", "Luminescence", "Luminescence", "ReniOut/RenIn (%)", "FireOut/FireIn (%)", 
				"ReniOut/ReniIn", "FireOut/FireIn", "CoIp/IP"};
		for (int i = 0; i < selectedBarIndices.length; i++) {
			int metricIndex = selectedBarIndices[i];
			if (metricIndex%2 == 0) graphBar(book, sheet, metricsToGraph[metricIndex], axisLabels[metricIndex], true);
			else graphBar(book, sheet, metricsToGraph[metricIndex], axisLabels[metricIndex], false);
		}
		for (int j = 0; j < selectedWhiskerIndices.length; j++) {
			int metricIndex = selectedWhiskerIndices[j];
			if (metricIndex%2 == 0) graphWhisker(book, sheet, metricsToGraph[metricIndex], axisLabels[metricIndex], true);
			else graphWhisker(book, sheet, metricsToGraph[metricIndex], axisLabels[metricIndex], false);
		}
		//////////// WATCH OUT WRITES OUT TO TEST HERE /////////////////
		try {
			book.removeSheet(0);
			book.write();
			book.close();
		} catch (WriteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Graphs a bar plot by sending the graph window the means, std error means, types, and labels of of each datapoint,
	 * based upon information found under the metric name.
	 */
	private void graphBar(WritableWorkbook book, Sheet sheet, String metricName, String axisLabel, boolean forBait) {
		String title = metricName.toUpperCase();
		ArrayList<Double> means = new ArrayList<Double>();
		ArrayList<Double> stdErrorMean = new ArrayList<Double>();
		ArrayList<String> type = new ArrayList<String>(); 
		ArrayList<String> label = new ArrayList<String>();
		int numControls = fillArraysForGraphing(dataSheet, metricName, forBait, means, stdErrorMean, type, label);
		makeBarGraphSheet(book, metricName, label, type, means, stdErrorMean);
		if (doNotOpenGraphsBox.isSelected()) return;
		JFrame frame = new JFrame("LUMIER Bar Graph");
		GraphWindow graphPanel = new GraphWindow(log, title, axisLabel, numControls, means, stdErrorMean, type, label);
		frame.add(graphPanel);
		frame.pack();
		frame.setVisible(true);	
	}
	
	// returns the number of controls found in the data (empty, mock, and yfp means the return value would be 3, NOT 6)
	private int fillArraysForGraphing(Sheet sheet, String metricName, boolean forBait,
			ArrayList<Double> means, ArrayList<Double> stdErrorMean, ArrayList<String> type, ArrayList<String> labels) {
		int col = getColumnFor(metricName, 0);
		log.append(metricName + "_BarGraph" + ": data cols " + stringForCol(col+2) + ", " + stringForCol(col+4) + "\n");
		int hasEmptyControl = 0;
		int hasYFPControl = 0;
		int hasMockControl = 0;
		for (int row = 1; !sheet.getCell(col,row).getContents().isEmpty(); row+=3) {
			String bait = sheet.getCell(1,row).getContents();
			String prey = sheet.getCell(2,row).getContents();
			String label = bait + " + " + prey;
			if (label.toLowerCase().contains("yfp") || label.toLowerCase().contains("mock") || label.toLowerCase().contains("empty")) continue;
			labels.add(label);
			if (forBait) type.add("IP");
			else type.add("CoIP");
			means.add(safeDoubleFromCell(sheet.getCell(col+2, row)));
			stdErrorMean.add(safeDoubleFromCell(sheet.getCell(col+4, row)));
			if(fillArraysWithControlData(row, bait, prey, "Empty", label, col, means, stdErrorMean, labels, type)) hasEmptyControl = 1;
			if(fillArraysWithControlData(row, bait, prey, "YFP", label, col, means, stdErrorMean, labels, type)) hasYFPControl = 1;
			if(fillArraysWithControlData(row, bait, prey, "Mock", label, col, means, stdErrorMean, labels, type)) hasMockControl = 1;
		}
		return hasEmptyControl + hasYFPControl + hasMockControl;
	}
	
	private boolean fillArraysWithControlData(int startRow, String bait, String prey, String controlName, String label, int col,
			ArrayList<Double> means, ArrayList<Double> stdErrorMean, ArrayList<String> labels, ArrayList<String> type) {
		int preyControlRow = 0;
		int baitControlRow = 0;
		baitControlRow = getRowFor(bait, controlName, startRow);
		preyControlRow = getRowFor(controlName, prey, startRow);
		// do everything twice. once for bait and once for prey controls
		labels.add(label);
		labels.add(label);
		type.add("Bait + "+ controlName + " Control");
		type.add("Prey + "+ controlName + " Control");
		if (baitControlRow == 0) {
			means.add(0.0);
			stdErrorMean.add(0.0);
			means.add(0.0);
			stdErrorMean.add(0.0);
			return false;
		} else {
			means.add(safeDoubleFromCell(dataSheet.getCell(col+2, baitControlRow)));
			stdErrorMean.add(safeDoubleFromCell(dataSheet.getCell(col+4, baitControlRow)));
			means.add(safeDoubleFromCell(dataSheet.getCell(col+2, preyControlRow)));
			stdErrorMean.add(safeDoubleFromCell(dataSheet.getCell(col+4, preyControlRow)));
			return true;
		}
	}
	
	private void makeBarGraphSheet(WritableWorkbook book,
			String metric, ArrayList<String> labels, ArrayList<String> types, ArrayList<Double> means, ArrayList<Double> stdErrorMeans) {
		WritableSheet sheet = book.getSheet(metric + "_barChart");
		if (sheet == null) sheet = book.createSheet(metric + "_barChart", workbook.getNumberOfSheets());
		for (int i = 0; i < labels.size(); i++) {
			Label label = new Label(0, i, labels.get(i));
			Label type = new Label(1, i, types.get(i));
			Number mean = new Number(2, i, means.get(i));
			Number stdErrorMean = new Number(3, i, stdErrorMeans.get(i));
			try {
				sheet.addCell(label);
				sheet.addCell(type);
				sheet.addCell(mean);
				sheet.addCell(stdErrorMean);
			} catch (RowsExceededException e) {
				e.printStackTrace();
			} catch (WriteException e) {
				e.printStackTrace();
			}
		}
	}
	
	/* Graphs box and whisker plot for the metric name given. Will look for controls in bait or prey name column
	 * depending upon whether the boolean forBait is true or false. 
	 */
	private void graphWhisker(WritableWorkbook book, Sheet sheet, String metricName, String axisLabel, boolean forBait) {
		String title = metricName.toUpperCase();
		int valueCol = getColumnFor(metricName, 0);
		//if (metricName.equals("ip")) valueCol += 5; // "ip" is picked up first at the ex/ip norm, which is 5 cols before what we want
		log.append(metricName + "_WhiskerPlot" + ": data cols " + stringForCol(valueCol) + ", " + stringForCol(valueCol+1) + "\n");
		int labelCol = 2;
		if (!forBait) labelCol = 1;
		// these values are necessary for plotting a  box and whisker plot using the JFreeChart API
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<Double> emptyP = new ArrayList<Double>();
		ArrayList<Double> yfpP = new ArrayList<Double>(); 
		ArrayList<Double> mockP = new ArrayList<Double>();
		fillArraysForWhiskerPlot(sheet, valueCol, labelCol, values,emptyP, yfpP, mockP);
		makeWhiskerPlotSheet(book, metricName, values, emptyP, yfpP, mockP);
		if (doNotOpenGraphsBox.isSelected()) return;
		JFrame frame = new JFrame("LUMIER Whisker Graph");
		WhiskerWindow graphPanel = new WhiskerWindow(log, title, axisLabel, values, emptyP, yfpP, mockP);
		frame.add(graphPanel);
		frame.pack();
		frame.setVisible(true);

	}
	
	private void fillArraysForWhiskerPlot(Sheet sheet, int valueCol, int labelCol, 
			ArrayList<Double> values, ArrayList<Double> empty, ArrayList<Double> yfp, ArrayList<Double> mock) {
		for (int row = 1; !sheet.getCell(0,row).getContents().isEmpty(); row++) {
			double valuea = safeDoubleFromCell(sheet.getCell(valueCol, row));
			double valueb = safeDoubleFromCell(sheet.getCell(valueCol+1, row));
			String name = dataSheet.getCell(labelCol, row).getContents().toLowerCase();	
			if (name.contains("empty")) {
				empty.add(valuea);
				empty.add(valueb);
			} else if (name.contains("yfp")) {
				yfp.add(valuea);
				yfp.add(valueb);
			} else if (name.contains("mock")) {
				mock.add(valuea);
				mock.add(valueb);
			} else {
				values.add(valuea);
				values.add(valueb);
			}
		}
	}
	
	private void makeWhiskerPlotSheet(WritableWorkbook book, String metric, 
			ArrayList<Double> values, ArrayList<Double> empty, 
			ArrayList<Double> yfp, ArrayList<Double> mock) {
		WritableSheet sheet = book.getSheet(metric + "_WhiskerPlot");
		if (sheet == null) sheet = book.createSheet(metric + "_WhiskerPlot", workbook.getNumberOfSheets());
		int row = 0;
		row = arrayIntoWhiskerSheet("sample", values, row, sheet);
		
		if (!empty.isEmpty()) row = arrayIntoWhiskerSheet("empty", empty, row, sheet);
		if (!yfp.isEmpty())	row = arrayIntoWhiskerSheet("yfp", yfp, row, sheet);
		if (!mock.isEmpty()) row = arrayIntoWhiskerSheet("mocka", mock, row, sheet);
	}
	
	private int arrayIntoWhiskerSheet(String title, ArrayList<Double> values, int row, WritableSheet sheet) {
		Iterator<Double> it = values.iterator();
		while (it.hasNext()) {
			Label sampleLabel = new Label(0, row, title);
			Number value = new Number(1, row, it.next());
			try {
				sheet.addCell(sampleLabel);
				sheet.addCell(value);
			} catch (RowsExceededException e) {
				e.printStackTrace();
			} catch (WriteException e) {
				e.printStackTrace();
			}
			row++;
		}
		return row;
	}
	
	//********************** GENERAL HELPER FUNCTIONS ****************************************
	private String stringForCol(int col) {
		String toReturn = "";
		char secondLetter = (char)('A' + col%26);
		String firstLetter = "";
		if (col/26 > 0) firstLetter += (char)('A' + col/26 - 1);
		toReturn = firstLetter + secondLetter;
		return toReturn;
	}
	
	/* Searches for the row containing the bait and prey labeled as indicated. begins the search at the indicated row.
	 * Conducts a linear search first descending from indicated row. then ascending. If there are multiple hits for
	 * the given bait and prey, then the closest row to the start row is used. Returns 0 if the bait prey string is not
	 * found at all.
	 */
	private int getRowFor(String bait, String prey, int startRow) {
		int i, j;
		int last = lastRow();
		// searches following the startRow
		for (i = startRow; i < last; i+=3) 
			if(dataSheet.getCell(1,i).getContents().toLowerCase().contains(bait.toLowerCase()) && 
					dataSheet.getCell(2, i).getContents().toLowerCase().contains(prey.toLowerCase()))
				break;
		// searches ascending from the startRow
		for (j = startRow; j > 0; j-=3)
			if(dataSheet.getCell(1,j).getContents().toLowerCase().contains(bait.toLowerCase()) && 
					dataSheet.getCell(2, j).getContents().toLowerCase().contains(prey.toLowerCase()))
				break;
		
		if (i < last && j > 0)
			if (i-startRow > startRow-j) return j; // if j is closer to start row
			else return i; // if i is
		else 
			if (j > 0) return j; // if only j is greater than 0
			else if (i < last) return i; // if only i is less than the last row
			else return 0; // if the bait and prey combo is not found at all, return 0.
	}
	
	int getColumnForExact(String s) {
		int col;
		for (col = 0; !dataSheet.getCell(col,0).getContents().isEmpty(); col++)
			if (dataSheet.getCell(col,0).getContents().equalsIgnoreCase(s))
				return col;
		return col;
	}
	
	int getColumnFor(String s, int col) {
		while (!dataSheet.getCell(col,0).getContents().isEmpty()) {
			if (dataSheet.getCell(col,0).getContents().toLowerCase().contains(s.toLowerCase()))
				return col;
			col++;
		}
		return col;
	}
	
	private int lastRow() {
		int row = 0;
		while (!dataSheet.getCell(0,row).getContents().isEmpty()) row++;
		return row;
	}
	
	private double safeDoubleFromCell(Cell cell) {
		String contents = cell.getContents();
		String allNumbers = "0123456789.-,";
		for (int i = 0; i < contents.length(); i++) {
			if (!allNumbers.contains("" + contents.charAt(i))) {
				log.append("Attempt to change " + contents + " to a number failed. Returned 0.\n");
				return 0;
			}
		}
		return Double.parseDouble(contents);
	}

	
	
	//*********** LOTS OF GUI ELEMENTS ****************
	private JPanel mainPanel;
	private JButton generalButton;
	private JButton specificButton;
	private boolean isGraphingGeneral;
	
	private JPanel selectionPanel;
	private JComboBox selectionMenu;
	private JButton selectInteractionButton;
	private JButton backToSearch;
	private JButton finalizeSheet;
	
	private JLabel saveSheetLabel;
	private JTextField sheetNameField;
	
	private JPanel searchPanel;
	private JTextField vectorField;
	private JButton search;
	private JButton clearAll;
	
	private JPanel buttons;
	private JButton chooseFile;
	private JTextArea log;
	private JFileChooser chooser;
	private File file;
	private WritableWorkbook workbook;
	private Sheet dataSheet;
	
	private JPanel graphOptions;
	private JList barChartList;
	private JList whiskerPlotList;
	private JCheckBox doNotOpenGraphsBox;
	
	private Set<String> selectedInteractions;
	private Set<String> interactionsToChooseFrom;
}
