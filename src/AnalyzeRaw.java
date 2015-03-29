/**AnalyzeRaw
 * ----------
 * This class provides the functionality of reading data from the robot-created
 * excel file and matching this data with interaction labels provided in a layout
 * file. The raw data is then saved in an excel file format with options for
 * normalization. Means, SDs, and SEMs are calculated for every METRIC, which at
 * the moment is set to ReniIn, FireIn, ReniOut, and FireOut. Uses the JExel API
 * for excel file reading/writing functionality.
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.math.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import jxl.*;
import jxl.format.Colour;
import jxl.read.biff.BiffException;
import jxl.write.*;
import jxl.write.Number;
import jxl.write.biff.RowsExceededException;


public class AnalyzeRaw implements ActionListener {
	
	// GLOBAL VARIABLES
	private static final String[] METRICS = {"reniin", "firein", "reniout", "fireout"};
	private static final int REPS = 3;
	
	public AnalyzeRaw(AnalyzeResults mainPanel, JTextArea text, JPanel buttonPanel) {
		// initializes GUI by creating and adding buttons and so forth
		
		layoutList = new ArrayList<String[]>();
		if(sheetMap == null) sheetMap = new HashMap<String, Integer[]>();
		
		
		log = text;
		buttons = buttonPanel;
		panel = mainPanel;
		continueButton = new JButton("Continue");
		continueButton.addActionListener(this);
		showBlanks = new JCheckBox(": Display all blank values  |");
		volumeCorrection = new JCheckBox(": Correct for volume and surface area");
		
		
		JLabel label = new JLabel("Normalization options:");
		expressionNorm = new JCheckBox("Expression only");
		coIPNorm = new JCheckBox("IP only");
		coIPNorm.setSelected(true);
		allNorm = new JCheckBox("Expr. & IP");
		allNorm.setSelected(true);
		buttons.add(showBlanks);
		//buttons.add(label);
		//buttons.add(expressionNorm);
		//buttons.add(coIPNorm);
		//buttons.add(allNorm);
		
		//buttons.add(volumeCorrection);
		buttons.add(continueButton);
		panel.validate();
		log.append("Press continue to select a .xls file to read raw data from.\n*Note that .xlsx will not function properly\n");
	}
	
	
	
	//************* METHODS FOR READING VALUES INTO TEMPORARY DATA STRUCTORS****************
	/* 	Reads data from an entire workbook by iterating over all the sheets
		The sheets must be formatted such that they contain the name of the run, metric, plate, and run (a/b)*/
	private void readData(Workbook workbook) {
		String[] sheetNames = workbook.getSheetNames();
		// creates a new sheet map when the workbook is the first to be read in
		for (int k = 0; k < sheetNames.length; k ++) {
			String currName = sheetNames[k];
			boolean hasMetric = false;
			for (int i = 0; i < METRICS.length; i++)
				if (currName.toLowerCase().contains(METRICS[i])) hasMetric = true;
			if (!hasMetric) {
				log.append("Skipping sheet <<" + currName + ">>. Not in the correct format.[ex) reniOUT_2b]\n");
				continue;
			}
			if (currName.contains("run1_") || currName.contains("run2_")) useRunLabel = true;
			readSheet(currName);
		}
		// when there are additional data to be read (layout file exceeds the size of the amount of data read in)
		if (layoutList.size() > sheetMap.size()/METRICS.length/2)
			readData(getNextWorkbook());
	}
	
	/*	Asks user to retrieve that next workbook containing data when the layout file has more interactions than
		have so far been provided data for.*/
	private Workbook getNextWorkbook() {
		log.append("The layout file indicates that you must have more data. Choose next data file.\n");
		chooser.setApproveButtonText("Open next file");
		final FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel File", "xls");
	    chooser.setFileFilter(filter);
	    int returnVal = chooser.showOpenDialog(panel);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	dataFile = chooser.getSelectedFile();
		    try {
		    	wb = Workbook.getWorkbook(dataFile);
				return wb;
			} catch (BiffException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    } else {
	    	log.append("The layout file indicates that you must have more data. Choose.\n");
	    }
    	return getNextWorkbook();
	}
	
	/*	Reads the information from a layout file - includes interaction name comment. 
	 * 	This information is held in an arraylist of string vectors (each vector represents
	 * 	a plate and the strings are the interaction name and comment separated by tabs.*/
	private void readLayoutFile(File layoutFile) {
		char row = 'A';
		int plates = 0, count = 0, col = 1;
		String[] currPlate = new String[384]; 
		try {
			final BufferedReader rd = new BufferedReader(new FileReader(layoutFile));
			String line = rd.readLine();
			while (true) {
				line = rd.readLine();
				if (line==null) break;
				String[] components = line.split("\t");
				// starts a new plate when the plate label for the interaction exceeds the number of plates
				// currently held in the arraylist
				if (plates < Integer.parseInt(components[2])) {
					currPlate = new String[384];
					layoutList.add(currPlate);
					plates ++;
					count = 0;
				}
				row = (char) ('A' + count%384/24);
				col = 1 + count%384%24; 
				// Components [0] Sample (layout format), [1] Comment, [2] Row, [3] Column 
				String layoutElement = components[0] + "\t" + components[1] + "\t" + row + "\t" + col;
				
				// the layout file is set up such that interactions are in pairs of 3 and alternate
				// the switch statement is used so that the arraylist holds sets of interactions
				// contiguously.*Note that there is the necessary complimentary switch statement to
				// in reading data.
				switch (count%6) {
					case 0:	currPlate[count] = layoutElement;
					break;
					case 1: currPlate[count+2] = layoutElement;
					break;
					case 2: currPlate[count-1] = layoutElement;
					break;
					case 3: currPlate[count+1] = layoutElement;
					break;
					case 4: currPlate[count-2] = layoutElement;
					break;
					case 5: currPlate[count] = layoutElement;
					break;
				}				
				count ++;
			}
			rd.close();
		} catch (final IOException ex) {
			throw new EmptyStackException();
		}
	}
	
	/* Reads data from one sheet into an array of data which corresponds to its plate
	 * then the plate is added to the overall collection of data held in the sheetMap.
	 */
	private void readSheet(String currSheet) {
		Sheet sheetToRead = wb.getSheet(currSheet);
		Integer[] dataArr = new Integer[384];
		for (int i =0 ; i < 24; i++) {
			for (int j = 16; j < 32; j ++) {
				int additive = 0;
				switch (i%6) {
					case 1: additive+=2;
					break;
					case 2: additive-=1;
					break;
					case 3: additive+=1;
					break;
					case 4: additive-=2;
					break;
					default: additive = 0;
				}
				String contents = sheetToRead.getCell(i,j).getContents();
				if (contents.toLowerCase().contains("e") || contents.isEmpty()) contents = "0";
				dataArr[(j-16)*24 + i + additive] = Integer.parseInt(contents);
			}
		}
		sheetMap.put(currSheet.toLowerCase(), dataArr);
	}
	
	/* General functionality method for opening and reading in the file. Then analysis is performed.
	 * Calls for writing the data into the formatted file.
	 */
	private void openFile(AnalyzeResults panel) {
		// First half of this method includes all the ugly details in opening a correctly named file.
		chooser = new JFileChooser();
		chooser.setApproveButtonText("Open Excel File of Raw Results");
		final FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "Excel File", "xls");
	    chooser.setFileFilter(filter);
		log.append("Loading......\n");
	    int returnVal = chooser.showOpenDialog(panel);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	dataFile = chooser.getSelectedFile();
	    } else {
	    	log.append("Please click continue again and select files.\n");
	    	return;
	    }
	    directory = chooser.getCurrentDirectory();
		File[] filesInDirectory = directory.listFiles();
		File layoutFile = null;
		for (int i = 0; i < filesInDirectory.length; i++) {
			if (filesInDirectory[i].getName().toLowerCase().contains("lumier_layout")) {
				layoutFile = filesInDirectory[i];
				break;
			}
		}
		if (layoutFile == null)
			// throw error if there is no layout file located in the selected folder
			log.append("There is no layout file in the current folder.\nAnalysis terminated.\n");
		else
			createFormattedFile(layoutFile); // SENDS PROGRAM TO WRITING OF DATA SECTION
	}
	
	
	
	
	// ********************* METHODS TO WRITE OUT THE DATA *********************************
	private void createFormattedFile(File layoutFile) {
	    try {
	    	for (int i = 1; i <=2; i++) {
	    		runLabel = "run" + i;
				wb = Workbook.getWorkbook(dataFile);
				if (createNewWorkbook() == 0) return;
				readLayoutFile(layoutFile); // see reading methods
				readData(wb); // see reading methods
				writeData();
				//normalizeData(); // see normalization methods
				newWb.write();
				newWb.close();
				sheetMap.clear();
				layoutList.clear();
	    	}
			// a host of exceptions that need to be caught in order for jxl to shut the fuck up.
		} catch (BiffException e) {
			log.append("error0");
			e.printStackTrace();
		} catch (IOException e) {
			log.append("error1");
			e.printStackTrace();
		} catch (RowsExceededException e1) {
			log.append("error2");
			e1.printStackTrace();
		} catch (WriteException e1) {
			log.append("error3");
			e1.printStackTrace();
		}
		log.append("All done! :)\n");

//		log.append("Blank average: \n" + totalBlankFluorescence + " fluorescence / " + numBlanks +
//				" blanks = " + totalBlankFluorescence/numBlanks + "\n"+ "To visualize this data:\n" +
//						"1) Open and save the LUMIER_Analysis file externally.\n" +
//						"2) Return to Main Menu and click Analyze Results from Formatted Data.\n" +
//						"3) Open the file and choose to visualize Data.\n"); 
		// Blanks refer to the empty/cell only (??) wells of the 384-well plate that are labeled blank
	}
	
	/* Creates and saves a new excel workbook in the folder of the raw data file. The workbook
	 * is named by appending the run number to LUMIER_Analysis.
	 */
	private int createNewWorkbook() {
//		runLabel = dataFile.getName().split("_")[0].toLowerCase();
//		if (!runLabel.toLowerCase().split("_")[0].contains("run")) {
//			log.append("The file name must begin with the run number. ex) run1_data.xls\n");
//			log.append("Function canceled\n");
//			return 0;
//		}
		File analysisFile = new File(directory.getAbsolutePath() + "/LUMIER_Analysis_" + runLabel +  ".xls");
		try {
			analysisFile.createNewFile();
			newWb = Workbook.createWorkbook(analysisFile.getAbsoluteFile());
			dataSheet = newWb.createSheet("RawData", 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 1;
	}
	
	/* Huge method that writes all the data into the new formatted workbook. */
	private void writeData() throws RowsExceededException, WriteException, IOException {	
		File csvFile = new File(directory.getAbsolutePath() + "/LUMIER_Analysis_" + runLabel +  ".csv");
		csvFile.createNewFile();
		BufferedWriter cw = new BufferedWriter(new FileWriter(csvFile.getAbsoluteFile()));
		
		int row = 1;
		writeLabels();
		
		// iterate over all the plates
		for (int i = 0; i < layoutList.size(); i++) {
			plateBeginningRow = new int[layoutList.size()];
			plateBeginningRow[i] = row;
			int plate = i + 1;
			String[] plateSamples = layoutList.get(i);
			
			// iterate over 384 wells of the plate
			for (int j = 0; j < plateSamples.length; j++) {

				
				// write sample
				String[] components = plateSamples[j].split("\t");
				// Components [0] Sample ("Bait + Prey"), [1] Comment, [2] Row, [3] Column 
				int colNumber = Integer.parseInt(components[3]);
				char rowLetter = components[2].charAt(0);

				// TO GETOVERALL AVERAGE FLUORESCENCE FROM BLANK PLATES, DO NOT WRITE BLANK DATA INTO THE WORKBOOK
				if(components[0].equalsIgnoreCase("blank")) {
					displayBlankData(plate, j);
					continue;
				}
				// use hard-coded numbers :( but need to in order to format the data correctly
				Label interaction = new Label(0, row, components[0]);
				dataSheet.addCell(interaction);
				// write the bait and prey
				String[] baitAndPrey = components[0].split(" ");
				String b = baitAndPrey[0];
				String p = baitAndPrey[2];
				// switch bait and prey if the prey contains reni because empty-reni is currently considered prey (but it's not)
				// also switch bait and prey if run 2 and the comment (which is components[1]) 
				if (p.toLowerCase().contains("reni")) {// || (runLabel.contains("2") && components[1].equalsIgnoreCase("sample"))) {
					b = p;
					p = baitAndPrey[0];
				}
				cw.write(b);
				Label bait = new Label(1, row, b);
				dataSheet.addCell(bait);
				cw.write(","+p);
				
				Label prey = new Label(2, row, p);
				dataSheet.addCell(prey);
				// write comment
				cw.write(","+components[1]);
				Label comment = new Label(3, row, components[1]);
				dataSheet.addCell(comment);
				// write plate
				cw.write(","+plate);
				Number plateNum = new Number(4, row, plate);
				dataSheet.addCell(plateNum);
				// write plate
				cw.write(","+rowLetter);
				Label r = new Label(5, row, ""+rowLetter);
				dataSheet.addCell(r);
				// write plate
				cw.write(","+colNumber);
				Number c = new Number(6, row, colNumber);
				dataSheet.addCell(c);
				
				// write the data by getting information from each metric.
				// WARINING HARD CODED NUMBERS! 2 = number
				for (int k = 0; k < METRICS.length; k++) {
					String prefix = "";
					if (useRunLabel) prefix = runLabel + "_";
					Integer[] sheet = sheetMap.get(prefix + METRICS[k] + "_" + plate + "a");
					if (sheet == null) {
						log.append("Sheet \"" +prefix + METRICS[k] + "_" + plate + "a\" not found.\n");
					} else {					
						double value = sheet[j];
						cw.write(","+value);
						Number cell = new Number(7 + 2*k, row, value);
						dataSheet.addCell(cell);
						sheet = sheetMap.get(prefix +METRICS[k] + "_" + plate + "b");
						value = sheet[j];
						cw.write(","+value);
						cell = new Number(8 + 2*k, row, value);
						dataSheet.addCell(cell);
					}
				}
				cw.write("\n");
				row++;
			}
		}
		cw.flush();
		cw.close();
//		for (int i = 0; i < METRICS.length; i++)
//			calculateMeanSDSE(5 + 5*i, true);
	}
	
	// TO GETOVERALL AVERAGE FLUORESCENCE FROM BLANK PLATES, DOES NOT WRITE BLANK DATA INTO THE WORKBOOK
	private void displayBlankData(int plate, int j) {
		for (int k = 0; k < METRICS.length; k++) {
			String prefix = "";
			if (useRunLabel) prefix = runLabel + "_";
			Integer[] runaVec = sheetMap.get(prefix + METRICS[k] + "_" + plate + "a");
			Integer[] runbVec = sheetMap.get(prefix + METRICS[k] + "_" + plate + "b");
			if (runaVec == null || runbVec == null) {
				log.append("The sheets within the selected file are not labeled correctly. ex) reniIN_2a.xls\n");
				log.append("Function canceled\n");
				return;
			}
			int runa = runaVec[j];
			int runb = runbVec[j];
			if(showBlanks.isSelected()) 
				log.append(prefix + METRICS[k] + "_" + plate + ":\t" + runa + "\t" + runb +"\n");
			totalBlankFluorescence +=  runa + runb;
			numBlanks+=2;
		}
	}
	
	// Write the labels for each column in row 0.
	private void writeLabels() throws RowsExceededException, WriteException {
		Label sample = new Label(0, 0, "Layout Format");
		Label bait = new Label(1, 0, "Bait");
		Label prey = new Label(2, 0, "Prey");
		Label comment = new Label (3, 0, "Comment");
		Label plate = new Label(4, 0, "386-Well Plate");
		Label row = new Label(5, 0, "Row");
		Label col = new Label(6, 0, "Column");
		dataSheet.addCell(bait);
		dataSheet.addCell(prey);
		dataSheet.addCell(sample);
		dataSheet.addCell(comment);
		dataSheet.addCell(plate);
		dataSheet.addCell(row);
		dataSheet.addCell(col);
		labelMetric(7, METRICS);
	}
	
	/* The metrics are labeled each with trial a, trial b, the mean, std. dev., and std. error mean.
	 * Again the hard-coded numbers are shitty, but they correspond with the columns in the data workbook.
	 * THE 2 is HARD CODED AS THE NUMBER OF COLUMNS FOR EACH SET!!!
	 */
	private void labelMetric(int startCol, String[] labels) throws RowsExceededException, WriteException {
		for (int i = 0; i < labels.length; i++) {
			Label metric = new Label(startCol + 2*i, 0, labels[i] + "a");
			dataSheet.addCell(metric);
			metric = new Label(startCol + 1 + 2*i, 0, labels[i] + "b");
			dataSheet.addCell(metric);
//			metric = new Label(startCol + 2 + 5*i, 0, labels[i] + " Mean");
//			dataSheet.addCell(metric);
//			metric = new Label(startCol + 3 + 5*i, 0, labels[i] + " SD");
//			dataSheet.addCell(metric);
//			metric = new Label(startCol + 4 + 5*i, 0, labels[i] + " SEM");
//			dataSheet.addCell(metric);
		}
	}
	
	/* This method is nice as it doesn't use hard-coded numbers. This method writes formulas into the excel sheet,
	 * so that the user knows exactly where each of the values came from.
	 */
	private void calculateMeanSDSE(int col, boolean toColor) throws RowsExceededException, WriteException {
		int row = 1;
		while (true) {
			if (dataSheet.getCell(0, row).getContents().isEmpty()) break;
			if (!dataSheet.getCell(col, row).getContents().isEmpty()) {
				Formula mean = new Formula(col + 2, row, "AVERAGE(" + getRange(col, row) + ")");
				Formula sd =  new Formula(col + 3, row, "STDEV(" + getRange(col, row) + ")");
				Formula se = new Formula(col + 4, row, "STDEV(" + getRange(col, row) + ")/SQRT(" + (REPS*2) + ")");			
				dataSheet.addCell(mean);
				dataSheet.addCell(sd);
				dataSheet.addCell(se);
			}
			if (toColor) colorOutliers(col, row);
			row+= REPS;
		}
	}
	
	/* Colors the background of outlier data values red. The data value is considered an outlier based
	 * upon a modified Cook's Distance test. If the absolute value of the difference between the mean with
	 * the data point and the mean excluding the data point divided my the root mean squared error is greater
	 * than a certain value: here 1...
	 */
	private void colorOutliers(int col, int row) {
		for (int i = 0; i < 2; i++) {
			ArrayList<Double> values = new ArrayList<Double>();
			double total = 0;
			for (int j = 0; j < 3; j++) {
				double value = Double.parseDouble(dataSheet.getWritableCell(col + i, row + j).getContents());
				values.add(value);
				total += value;
			}
			double mean = total/3;
			double denom = getSEMtimesN(mean, values);
			for (int j = 0; j < 3; j++) {
				if (valueIsCloseToOneOther(values, j)) continue;
				WritableCell cell = dataSheet.getWritableCell(col + i, row + j);
				double cooksDistance = getCooksDistance(denom, mean, values, j);
				if (cooksDistance > MAX_COOKS_DISTANCE) 
					cell.setCellFormat(redFormat);
			}
		}
	}
	
	boolean valueIsCloseToOneOther(ArrayList<Double> values, int j) {
		double currValue = values.get(j);
		for (int i = 0; i < values.size(); i++) {
			double otherValue = values.get(i);
			if (i==j) continue;
			double error = 0.0;
			if (otherValue > currValue) error = (otherValue - currValue) / otherValue;
			else error = (currValue - otherValue) / currValue;
			if (error < 0.5) return true;
		}
		return false;
	}
	
	private double getSEMtimesN(double mean, ArrayList<Double> values) {
		double result = 0;
		for (int i = 0; i < values.size(); i++)
			result = (mean - values.get(i))*(mean - values.get(i));
		return result;
	}
	
	private double getCooksDistance(double denominator, double mean, ArrayList<Double> values, int index) {
		double numerator = 0;
		double totalWithoutGivenIndex = 0;
		int n = values.size();
		double value = values.get(index);
		double percentError = 0;
		//if (value > mean) percentError = (value-mean)/mean;
		//else percentError = (mean-value)/mean;
		//if (percentError < 0.2) return 0;
		//if (percentError > 1) return 500;
		for (int i = 0; i < n; i++)
			if (i != index) totalWithoutGivenIndex += values.get(i);
		double meanWithoutIndex = totalWithoutGivenIndex/(n-1);
		numerator = n * (mean-meanWithoutIndex) * (mean-meanWithoutIndex);
		if (value > meanWithoutIndex) {
			if ((value-meanWithoutIndex)/meanWithoutIndex > 1.25) return 500;
		} else {
			if ((meanWithoutIndex-value)/meanWithoutIndex > 0.6) return 500;
		}
		return 0;//numerator/denominator;
	}
	
	/* Returns a string representation of an excel range over all the data values for a single interaction (usually 6).
	 * Only works for getting this specific range of 6.
	 */
	private String getRange(int col, int row) {
		String toReturn = "";
		char secondLetter = (char)('A' + col%26);
		String firstLetter = "";
		if (col/26 > 0) firstLetter += (char)('A' + col/26 - 1);
		toReturn = firstLetter + secondLetter + (row+1) + ":";
		col++;
		secondLetter = (char)('A' + col%26);
		firstLetter = "";
		if (col/26 > 0) firstLetter += (char)('A' + col/26 - 1);
		toReturn += firstLetter + secondLetter + (row+REPS);
		return toReturn;
	}
	
	//************** METHODS TO EVALUATE THE DATA **********************************
	private void normalizeData() {
		// normalizes expression if all or just norm is checked.
		if (allNorm.isSelected()) {
			normalizeExpression();
			normalizeEC();
		} else {
			if (expressionNorm.isSelected()) normalizeExpression();
		}
		// normalize IP only after COIP normalization and only if the coip is checked off.
		if (coIPNorm.isSelected()) normalizeCoIP();
		writeEfficiency();
		writeReference("", false);
		writeReference("yfp", false);
		writeReference("mock", false);
		if (allNorm.isSelected()) {
			writeReference("", true);
			writeReference("yfp", true);
			writeReference("mock", true);
		}
	}
	
	// HARD-CODED ROWS. WATCH OUT!!
	private void normalizeExpression() {
		int col = lastCol();
		try {
			String[] labels = {"IP", "CoIP"};
			labelMetric(col,labels);	
			int row = 1;
			while (true) {
				if (dataSheet.getCell(0, row).getContents().isEmpty()) break;
				Formula renia = new Formula(col, row, "P" + (row+1) + "/F" + (row+1)); //reniouta/reniina
				Formula renib =  new Formula(col + 1, row, "Q" + (row+1) + "/G" + (row+1));	//renioutb/reniina
				Formula firea = new Formula(col+5, row, "U" + (row+1) + "/K" + (row+1)); //fireouta/fireina
				Formula fireb =  new Formula(col+5 + 1, row, "V" + (row+1) + "/L" + (row+1)); //fireoutb/fireinb
				dataSheet.addCell(renia);
				dataSheet.addCell(renib);
				dataSheet.addCell(firea);
				dataSheet.addCell(fireb);
				row++;
			}
			calculateMeanSDSE(col, false);
			calculateMeanSDSE(col+5, false);
		} catch (RowsExceededException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
	}
	
	// HARD-CODED ROWS. WATCH OUT!!
	private void normalizeCoIP() {
		int col = lastCol();
		try {
			String[] labels = {"IP_Norm"};
			labelMetric(col,labels);	
			int row = 1;
			while (true) {
				if (dataSheet.getCell(0, row).getContents().isEmpty()) break;
				Formula a = new Formula(col, row, "U" + (row+1) + "/P" + (row+1));
				Formula b =  new Formula(col + 1, row, "V" + (row+1) + "/Q" + (row+1));		
				dataSheet.addCell(a);
				dataSheet.addCell(b);
				row++;
			}
			calculateMeanSDSE(col, false);
		} catch (RowsExceededException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
	}
	
	// HARD-CODED ROWS. WATCH OUT!!
	private void normalizeEC() {
		int col = lastCol();
		try {
			String[] labels = {"Ex/IP_Norm"};
			labelMetric(col,labels);	
			int row = 1;
			while (true) {
				if (dataSheet.getCell(0, row).getContents().isEmpty()) break;
				Formula a = new Formula(col, row, "AE" + (row+1) + "/Z" + (row+1));
				Formula b =  new Formula(col + 1, row, "AF" + (row+1) + "/AA" + (row+1));		
				dataSheet.addCell(a);
				dataSheet.addCell(b);
				row++;
			}
			calculateMeanSDSE(col, false);
		} catch (RowsExceededException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
	}
	
	private void writeEfficiency() {
		int numRows = lastRow();
		int col = lastCol();
		String[] labels = {"IP_Efficiency(%)", "CoIP_Efficiency(%)"};
		String normComponent = "/5";
		if (volumeCorrection.isSelected()) normComponent = "/" + IP_NORM_FACTOR;
		normComponent += "*100";
		try {
			labelMetric(col, labels);
			for (int row =1; row < numRows; row++) {
				Formula IPeffiencya = new Formula(col, row, "Z" + (row+1) + normComponent);
				Formula IPeffiencyb = new Formula(col+1, row, "AA" + (row+1) + normComponent);
				dataSheet.addCell(IPeffiencya);
				dataSheet.addCell(IPeffiencyb);
				Formula coIPeffiencya = new Formula(col+5, row, "AE" + (row+1) + normComponent);
				Formula coIPeffiencyb = new Formula(col+6, row, "AF" + (row+1) + normComponent);
				dataSheet.addCell(coIPeffiencya);
				dataSheet.addCell(coIPeffiencyb);
			}
			calculateMeanSDSE(col, false);
			calculateMeanSDSE(col+5, false);
		} catch (RowsExceededException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
	}
	
	/* Writes out a reference column for bait and a reference column for prey. Calculates out/in ratio for each input metric.*/
	private void writeReference(String s, boolean normalized) {
		int numRows = lastRow()-1;
		int col = lastCol();
		String colString = "U", n ="";
		if (normalized) {
			colString = "AJ";
			n += "norm";
		}
		String[] labels = {"R_OB" + s + n, "R_OP" + s + n};
		try {
			labelMetric(col, labels);
			if (s.isEmpty()) s = "empty";
			for (int row =1; row < numRows; row++) {
				if(dataSheet.getCell(3, row).getContents().equalsIgnoreCase("sample")) {
					int baitRow =findRow(dataSheet.getCell(1, row).getContents(), s, numRows);
					int preyRow = findRow(s, dataSheet.getCell(2, row).getContents(), numRows);
					Formula rob = new Formula(col, row,  colString + (row + 1) + "/" + colString + (baitRow+1));
					if (baitRow == 0) rob = new Formula(col, row,  "0");
					Formula rop = new Formula(col+5, row,  colString + (row + 1) + "/" + colString + (preyRow+1));
					if (preyRow == 0) rop = new Formula(col+5, row,  "0");
					dataSheet.addCell(rob);
					dataSheet.addCell(rop);
					
					// Modulate the column names and read values from trial b column
					String secondColString = "";
					if (colString.length() == 1) secondColString += "V";
					else secondColString += "AK";
					rob = new Formula(col + 1, row, secondColString + (row + 1) + "/" + secondColString + (baitRow+1));
					if (baitRow == 0) rob = new Formula(col+1, row,  "0");
					rop = new Formula(col+6, row, secondColString + (row + 1) + "/" + secondColString + (preyRow+1));
					if (preyRow == 0) rop = new Formula(col+6, row,  "0");
					dataSheet.addCell(rob);
					dataSheet.addCell(rop);
				}
			}
			calculateMeanSDSE(col, false);
			calculateMeanSDSE(col+5, false);
		} catch (RowsExceededException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
	}
	
	// finds the first row of an interaction
	private int findRow(String bait, String prey, int totalRows) {
		for (int i =1; i < totalRows; i+=3) 
			if(dataSheet.getCell(1,i).getContents().toLowerCase().contains(bait.toLowerCase()) && 
					dataSheet.getCell(2, i).getContents().toLowerCase().contains(prey.toLowerCase()))
				return i;
		return 0;
	}
	
	// finds the last column
	private int lastCol() {
		int col = 0;
		while (!dataSheet.getCell(col,0).getContents().isEmpty()) col++;
		return col;
	}
	
	// finds the last row
	private int lastRow() {
		int row = 0;
		while (!dataSheet.getCell(0,row).getContents().isEmpty()) row++;
		return row;
	}
	

	
	
	
	
	
	
	
	
	
	
	
	
	// ****************** Action Event and Instance Variables ************************
	public void actionPerformed(ActionEvent f) {
		if (f.getSource() == continueButton) {
			useRunLabel = false;
			sheetMap.clear();
			layoutList.clear();
			redFormat =  new WritableCellFormat();
			try {
				redFormat.setBackground(Colour.RED);
			} catch (WriteException e) {
				e.printStackTrace();
			}
			openFile(panel);
		}
	}
	
	
	// CONSTANTS
	private static double IP_NORM_FACTOR = 4.5;
	private static double MAX_COOKS_DISTANCE = 3;
	private WritableCellFormat redFormat;

	private int[] plateBeginningRow;
	private AnalyzeResults panel;
	private JPanel buttons;
	private JButton continueButton;
	private JCheckBox expressionNorm;
	private JCheckBox coIPNorm;
	private JCheckBox allNorm;
	private JCheckBox showBlanks;
	private JCheckBox volumeCorrection;
	
	private JTextArea log;
	private JFileChooser chooser;
	private File dataFile;
	private File directory;
	
	private Workbook wb;
	private WritableWorkbook newWb;
	private WritableSheet dataSheet;
	private String runLabel;
	private boolean useRunLabel;
	private Map<String, Integer[]> sheetMap;
	private ArrayList<String[]> layoutList;
	private int numBlanks;
	private int totalBlankFluorescence;
}
