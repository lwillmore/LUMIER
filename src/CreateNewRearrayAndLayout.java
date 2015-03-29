//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.EmptyStackException;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Collections;
//
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JFileChooser;
//import javax.swing.JLabel;
//import javax.swing.JList;
//import javax.swing.JPanel;
//import javax.swing.JTextArea;
//
//import java.util.*;
//
//public class CreateNewRearrayAndLayout implements ActionListener {
//
//
//	private static final int REPS = 3;
//	private static final String[] EMPTYC = {"pcDNA3.1", "pcDNA3.1"};
//	private static final String [] TANDEMC = {"pPAReni-Fire","pPAReni-Fire"};
//
//	
//	public CreateNewRearrayAndLayout(AnalyzeResults panel, JTextArea text, JPanel buttonPanel) {
//		log = text;
//		mainPanel = panel;
//		String[] preyControlOptions = {"Empty-Reni", "ReniCherry", "ReniMock"};
//		String[] baitControlOptions = {"Empty-Fire", "FireCherry", "FireMock"};
//		preyBox = new JList(preyControlOptions);
//		baitBox = new JList(baitControlOptions);
//		skipRepeatedControlBox = new JCheckBox();
//		buttonPanel.add(new JLabel("Bait controls: "));
//		buttonPanel.add(baitBox);
//		buttonPanel.add(new JLabel("Prey controls: "));
//		buttonPanel.add(preyBox);
//		buttonPanel.add(new JLabel("Skip repeated controls:"));
//		buttonPanel.add(skipRepeatedControlBox);
//		begin = new JButton("Continue");
//		buttonPanel.add(begin);
//
//		baitRearrayList = new ArrayList<RearrayEntry>();
//		preyRearrayList = new ArrayList<RearrayEntry>();
//
//		begin.addActionListener(this);
//		panel.validate();
//		log.append("EMPTY control set as: " + EMPTYC[0] + " + " + EMPTYC[1] +"\n");
//		log.append("POSITIVE control 1 set as: " + POSC1[0] + " + " + POSC1[1] + "\n");
//		log.append("POSITIVE control 2 set as: " + POSC2[0] + " + " + POSC2[1] + "\n");
//	}
//	
//	public void actionPerformed(ActionEvent f) {
//		if (f.getSource() == begin) {
//			preyControls = objectVectorToStringArray(preyBox.getSelectedValues());
//			baitControls = objectVectorToStringArray(baitBox.getSelectedValues());
//			if (preyControls.size() == 0 || baitControls.size() == 0) {
//				log.append("Please make a selection for bait and prey controls\n");
//			} else {
//				if (preyControls.size() != baitControls.size())
//					log.append("You have chosen an unequal number of bait and prey controls, but continuing anyway...");
//				layoutList = new ArrayList<String[][]>();
//				beginReading(mainPanel);
//			}
//		}
//	}
//	
//	
//	ArrayList<String> objectVectorToStringArray(Object[] input) {
//		ArrayList<String> toReturn = new ArrayList<String>();
//		log.append("You have selected:\n");
//		for (int i = 0; i < input.length; i++) {
//			String control = (String)input[i];
//			log.append(control + "\n");
//			toReturn.add(control);
//		}
//		return toReturn;
//	}
//	
//	private void beginReading(AnalyzeResults panel) {
//		
//		// JFileChooser boilerplate to select correct directory containing "final" interaction files
//		// The resulting files will be added here
//		chooser = new JFileChooser();
//		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//		chooser.setApproveButtonText("Select run folder containing final_Baits, final_Preys, and final_PPI files");
//	    int returnVal = chooser.showOpenDialog(panel);
//		if (returnVal == JFileChooser.APPROVE_OPTION) {
//			directory = chooser.getSelectedFile();
//			runLabel = directory.getName();
//			if (!runLabel.contains("run"))
//				log.append("The folder you have selected is not labeled with the run.\nFiles will not be labeled with the run number.\n");
//	    } else {
//	    	log.append("Please select file.\n");
//	    	return;
//	    }
//		
//		// Read through the files in the directory, using those that are appropriate
//		File[] filesInDirectory = directory.listFiles();
//		File baitsFile = null, preysFile = null, PPIFile = null;
//		int successfulReads = 0;
//		for (int i = 0; i < filesInDirectory.length; i++) {
//			if (filesInDirectory[i].getName().toLowerCase().contains("final_baits")) {
//				baitsFile = filesInDirectory[i];
//				successfulReads++;
//			} else if (filesInDirectory[i].getName().toLowerCase().contains("final_preys")) {
//				preysFile = filesInDirectory[i];
//				successfulReads++;
//			} else if (filesInDirectory[i].getName().toLowerCase().contains("final_ppi")) {
//				PPIFile = filesInDirectory[i];
//				successfulReads++;
//			}
//		}
//		
//		if (successfulReads != 3) {
//			log.append("Error in finding files:\n");
//			if (preysFile == null) log.append("final_Preys\n");
//			if (baitsFile == null) log.append("final_Baits\n");
//			if (PPIFile == null) log.append("final_PPI\n");
//		} else {
//			// Take in information from files
//			readFromFiles(baitsFile, preysFile);
//			writeToFiles(baitList, preyList, PPIFile);
//			log.append("Operation complete.\nYou may now close the program or return to the main menu.\n");
//		}
//	}
//	
//	private void readFromFiles(File bait, File prey) {
//		preyList = ninetySixWellMap(prey);
//		baitList = ninetySixWellMap(bait);
//	}
//	
//	
//	private void writeToFiles(Map<String, NinetySixWellEntry> baitList, Map<String, NinetySixWellEntry> preyList, File ppi) {
//		try {
//			final BufferedReader rd = new BufferedReader(new FileReader(ppi));
//			File baitsRearray = new File(directory.getAbsolutePath() + "/LUMIER_Baits_Rearray_" + runLabel+ ".csv");
//			File preysRearray = new File(directory.getAbsolutePath() + "/LUMIER_Preys_Rearray_" + runLabel+ ".csv");
//
//			baitsRearray.createNewFile();
//			preysRearray.createNewFile();
//
//			BufferedWriter bw = new BufferedWriter(new FileWriter(baitsRearray.getAbsoluteFile()));
//			BufferedWriter pw = new BufferedWriter(new FileWriter(preysRearray.getAbsoluteFile()));
//			
//			int counter = 0;
//			int plate = 1;
//			String currPlate[][] = new String[16][24];
//			layoutList.add(currPlate);
//			writeControls(bw, pw, plate);
//			
//			while(true) {
//				if (counter >= 384) {
//					plate ++;
//					currPlate = new String[16][24];
//					layoutList.add(currPlate);
//					writeControls(bw, pw, plate);
//					
//					reset(baitList, preyList);
//					counter = 0;
//				} else if ((counter >= 384 - 12 - REPS*(1 + preyControls.size() + baitControls.size())) ||
//						Arrays.asList(EMPTYC_WELLS).contains(counter) || 
//						Arrays.asList(PC1_WELLS).contains(counter) ||
//						Arrays.asList(PC2_WELLS).contains(counter)) {
//					counter++;
//				} else {
//				
//					String line = rd.readLine();
//					if (line == null) break; // EOF
//					
//					String[] elements = line.split("\t");
//					String currBait = elements[0];
//					String currPrey = elements[1];
//					
//					// IF IT'S A CONTROL SKIP IT
//					if ((Arrays.asList(EMPTYC).contains(elements[0]) ||
//							Arrays.asList(POSC1).contains(elements[0]) ||
//							Arrays.asList(EMPTYC).contains(elements[0])) && 
//							(Arrays.asList(EMPTYC).contains(elements[1]) ||
//									Arrays.asList(POSC1).contains(elements[1]) ||
//									Arrays.asList(EMPTYC).contains(elements[1])))
//						continue;
//					
//					// read bait field
//					NinetySixWellEntry baitEntry = baitList.get(currBait);
//					if (baitEntry == null) {
//						log.append("Error: " + currBait + " could not be found in the bait list. Skipping interaction: " + line + "\n");
//						continue;
//					}
//					
//					// read prey field
//					NinetySixWellEntry preyEntry = preyList.get(currPrey);
//					if (preyEntry == null) {
//						log.append("Error: " + currPrey + " could not be found in the prey list. Skipping interaction" + line + "\n");
//						continue;
//					}
//					
//					String comment = currBait + " + " + currPrey + "\t" + "Sample\t" + plate + "\n";
//					writeToRearray(true, currBait, baitEntry, plate, counter, false);
//					writeToRearray(false, currPrey, preyEntry, plate, counter, false);
//					putInLayout(plate, comment, counter, false);
//					counter = increment(counter, REPS);
//					if (!baitEntry.getHasControl()) {
//						for (int i = 0; i < baitControls.size(); i++) {
//							String control = baitControls.get(i);
//							NinetySixWellEntry baitControlEntry = baitList.get(control);
//							if (baitControlEntry == null) {
//								log.append("Error: Bait control " + control + " was not found in baitlist\n");
//								return;
//							}
//							writeToRearray(true, currBait, baitEntry, plate, counter, false);
//							writeToRearray(true, control, baitControlEntry, plate, counter, false);	
//							comment = currBait + " + " + control + "\t" + "Bait control\t" + plate + "\n";
//							putInLayout(plate, comment, counter, false);
//							counter = increment(counter, REPS);
//						}
//						if (skipRepeatedControlBox.isSelected()) baitEntry.setHasControl(true);
//					}
//					
//					if(!preyEntry.getHasControl()) {
//						for (int i = 0; i < preyControls.size(); i++) {
//							String control = preyControls.get(i);
//							NinetySixWellEntry preyControlEntry = baitList.get(control);
//							if (preyControlEntry == null) {
//								log.append("Error: Prey control " + control + " was not found in baitlist\n");
//								return;
//							}
//							writeToRearray(false, currPrey, preyEntry, plate, counter, false);
//							writeToRearray(true, control, preyControlEntry, plate, counter, false);
//							comment = control + " + " + currPrey + "\t" + "Prey control\t" + plate + "\n";
//							putInLayout(plate, comment, counter, false);
//							counter = increment(counter, REPS);
//						}
//						if (skipRepeatedControlBox.isSelected()) preyEntry.setHasControl(true);
//					}
//				}
//			}
//
//			rd.close();
//			writeToLayout();
//			sortRearray(bw, pw);
//		} catch (final IOException ex) {
//			throw new EmptyStackException();
//		}
//	}
//	
//	
//	void sortRearray(BufferedWriter bw, BufferedWriter pw) {
//		Collections.sort(baitRearrayList);
//		Collections.sort(preyRearrayList);
//
//		try {
//			for (int i = 0; i < baitRearrayList.size(); i++)
//				bw.write(baitRearrayList.get(i).rearray() + "\n");
//			
//			for (int i = 0; i < preyRearrayList.size(); i++)
//				pw.write(preyRearrayList.get(i).rearray() + "\n");
//			
//			bw.close();
//			pw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//
//	
//	/* Need to write the controls manually at the beginning of every new 384-well plate */
//	private void writeControls(BufferedWriter baitWriter, BufferedWriter preyWriter, int plateNum) {
//		writeControl(baitWriter, preyWriter, plateNum, EMPTYC, EMPTYC_WELLS);
//		writeControl(baitWriter, preyWriter, plateNum, POSC1, PC1_WELLS);
//		writeControl(baitWriter, preyWriter, plateNum, POSC2, PC2_WELLS);
//	}
//	
//	private void writeControl(BufferedWriter baitWriter, BufferedWriter preyWriter, int plateNum,
//			String[] controlNames, int[] controlWells) {
//		
//		String currBait = controlNames[0];
//		String currPrey = controlNames[1];
//		
//		// read bait field
//		NinetySixWellEntry baitEntry = baitList.get(currBait);
//		if (baitEntry == null) {
//			log.append("Error: " + currBait + " could not be found in the bait list. This is an absolute control. Progress terminated.\n");
//			return;
//		}
//		
//		// read prey field
//		
//		// note that empty control should only be found in the final_baits list, so rearraying for this
//		// is done exclusively in baits lists
//		NinetySixWellEntry preyEntry;
//		if (controlNames == EMPTYC) 
//			preyEntry = baitList.get(currPrey);
//		else 
//			preyEntry = preyList.get(currPrey);
//		if (preyEntry == null) {
//			log.append("Error: " + currPrey + " could not be found in the prey list. This is an absolute control. Progress terminated.\n");
//			return;
//		}
//		
//		for (int i = 0; i < controlWells.length; i++) {
//			int counter = controlWells[i];
//			writeToRearray(true, currBait, baitEntry, plateNum, counter, true);
//			
//			if (controlNames == EMPTYC) writeToRearray(true, currPrey, preyEntry, plateNum, counter, true);
//			else writeToRearray(false, currPrey, preyEntry, plateNum, counter, true);
//			
//			String comment = currBait + " + " + currPrey + "\t" + "Absolute control\t" + plateNum + "\n";
//			putInLayout(plateNum, comment, counter, true);		
//		}
//	}
//	
//	private void putInLayout(int plate, String comment, int counter, boolean isControl) {
//		int reps = REPS;
//		if (isControl) reps = 1;
//		for (int i = 0; i < reps; i++) {
//			String currPlate[][] = layoutList.get(plate-1);
//			int rownum384well = counter/24;
//			int colnum384well = counter%24; // counter is 0 indexed
//			if (rownum384well % 2 == 1) colnum384well = 23 - colnum384well;
//			
//			// currPlate[col][row] i.e. [16][24]
//			currPlate[rownum384well][colnum384well] = comment;
//			counter ++;
//		}
//	}
//	
//	private void writeToRearray(Boolean isBait, String name, NinetySixWellEntry e, int plate, int counter,
//			boolean isControl) {
//		int reps = REPS;
//		if (isControl) reps = 1;
//		for (int i = 0; i < reps; i++) {
//			int rownum384well = counter/24;
//			int colnum384well = counter%24; // counter is 0 indexed so the columns are as well
//			boolean oddRow = rownum384well % 2 == 1;
//			boolean oddCol = colnum384well % 2 == 1;
//			
//			if (oddRow) colnum384well = 23 - colnum384well;
//			
//			int destPlate = (plate-1)*4 + 1; //plate is 1-indexed
//			if (!oddRow && oddCol) 
//				destPlate++;
//			else if (oddRow && oddCol) 
//				destPlate += 2;
//			else if (oddRow && !oddCol)
//				destPlate += 3;
//			
//			char row = (char)('A' + rownum384well / 2);
//			int col = 1 + colnum384well / 2;
//			
//			
//			if (isBait)
//				baitRearrayList.add(new RearrayEntry (e, destPlate, "" + row + col));
//			else
//				preyRearrayList.add(new RearrayEntry (e, destPlate, "" + row + col));
//
//			//log.append("364 Well: Plate " + plate + ", Counter " + counter + " " + (char)('A' + rownum384well) +
//			//		"" + (colnum384well + 1) + " Dest" + empty0 + destPlate + ", " + row + col + '\n');
//			
//			counter++;
//		}
//	}
//	
//	private void writeToLayout() {
//		File layoutFile = new File(directory.getAbsolutePath() + "/LUMIER_Layout_" + runLabel+ ".txt");
//			try {
//				layoutFile.createNewFile();
//				BufferedWriter lw = new BufferedWriter(new FileWriter(layoutFile.getAbsoluteFile()));
//				lw.write("Sample\tComment\t384-Well plate\n");
//				for (int i = 0; i < layoutList.size(); i++) {
//					String[][] currPlate = layoutList.get(i);
//					for (int j = 0; j < 16; j++) {
//						for (int k = 0; k < 24; k++) {
//							String contents = currPlate[j][k];
//							if (contents == null) {
//								lw.write("Blank\tBlank\t" + (i+1) + "\n");
//							} else {
//								lw.write(contents);
//							}
//						}
//					}
//				}f
//				lw.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//	}
//	
//	// Creates map from bait name to their original location (on lysis plate)
//	private Map<String, NinetySixWellEntry> ninetySixWellMap(File file) {
//		Map<String, NinetySixWellEntry> toReturn = new HashMap<String, NinetySixWellEntry>();
//		try {
//			final BufferedReader rd = new BufferedReader(new FileReader(file));
//				while (true) {
//					String line = rd.readLine();
//					if (line == null) break;
//					String[] elements = line.split("\t");
//					toReturn.put(elements[0], 
//							new NinetySixWellEntry(elements[1], elements[2] + elements[3], false));
//				}
//			rd.close();
//			} catch (final IOException ex) {
//				throw new EmptyStackException();
//			}
//		return toReturn;
//	}
//	
//	// HAVE TO CHECK COUNTER TO MAKE SURE IT ISN"T IN THE THE CONTROL AREA
//	int increment(int before, int increaseBy) {
//		int toReturn = before + increaseBy;
//		if (toReturn >= EMPTYC_WELLS[0] && toReturn <= EMPTYC_WELLS[2])
//			toReturn = EMPTYC_WELLS[2] + 1;
//		return toReturn;
//	}
//	
//	private void reset(Map<String, NinetySixWellEntry> bait, Map<String, NinetySixWellEntry> prey) {
//		Iterator<String> bi = bait.keySet().iterator();
//		Iterator<String> pi = prey.keySet().iterator();
//		while (bi.hasNext())
//			bait.get(bi.next()).setHasControl(false);
//		while (pi.hasNext())
//			prey.get(pi.next()).setHasControl(false);
//	}
//	
//	private ArrayList<RearrayEntry> baitRearrayList;
//	private ArrayList<RearrayEntry> preyRearrayList;
//	private ArrayList<String[][]> layoutList;
//	private AnalyzeResults mainPanel;
//	private Map<String, NinetySixWellEntry> preyList;
//	private Map<String, NinetySixWellEntry> baitList;
//	private JCheckBox skipRepeatedControlBox;
//	private JList preyBox;
//	private JList baitBox;
//	private JButton begin;
//	private JTextArea log;
//	private JFileChooser chooser;
//	private File directory;
//	private String runLabel;
//	private ArrayList<String> baitControls;
//	private ArrayList<String> preyControls;
//}
//
///*private void sortRearrays() {
//// creates strings of full paths for old and new files
//String oldBait = directory.getAbsolutePath() + "/LUMIER_Baits_Rearray_" + runLabel+ ".csv";
//String oldPrey = directory.getAbsolutePath() + "/LUMIER_Preys_Rearray_" + runLabel+ ".csv";
//String newBait = directory.getAbsolutePath() + "/LUMIER_Baits_Rearray_SORTED_" + runLabel+ ".csv";
//String newPrey = directory.getAbsolutePath() + "/LUMIER_Preys_Rearray_SORTED_" + runLabel+ ".csv";
//
///* Execute bash script --> example:
// * pr -mts',' LUMIER_Baits_Rearray_run1.csv 
// * <(cut -d',' -f4 LUMIER_Baits_Rearray_run1.csv | cut -c2,3,4) | 
// * sort -t, -k6n | sort -t, -k3.5,4.1 -s | cut -d, -f1,2,3,4,5
// 
//String[] cmdBait = {"/bin/sh", "-c", 
// "sort -t, -k4.2,4.3n " + oldBait + " | sort -t, -k3.5,4.1 -s"} ;  //e.g test.sh -dparam1 -oout.txt
//
//String[] cmdPrey = {"/bin/sh", "-c", 
//		"sort -t, -k4.2,4.3n " + oldPrey + " | sort -t, -k3.5,4.1 -s"};  //e.g test.sh -dparam1 -oout.txt
//
//// executes programs
//try {
//	Process processBait = Runtime.getRuntime().exec(cmdBait);
//	Process processPrey = Runtime.getRuntime().exec(cmdPrey);
//	InputStream streamBait = processBait.getInputStream();
//	InputStream streamPrey = processPrey.getInputStream();
//	processBait.waitFor();
//	processPrey.waitFor();
//	
//	log.append("executed\n");
//	InputStreamReader bisr = new InputStreamReader(streamBait);
//    BufferedReader br = new BufferedReader(bisr);
//    
//	InputStreamReader pisr = new InputStreamReader(streamPrey);
//    BufferedReader pr = new BufferedReader(pisr);
//
//    BufferedWriter bw = new BufferedWriter(new FileWriter(new File(newBait)));
//	BufferedWriter pw = new BufferedWriter(new FileWriter(new File(newPrey)));
//	
//	while (true) {
//		String line = br.readLine();
//		if (line == null) break;
//		bw.write(line + "\n");
//	}
//	while (true) {
//		String line = pr.readLine();
//		if (line == null) break;
//		pw.write(line + "\n");
//	}
//	br.close();
//	bw.close();
//	pr.close();
//	pw.close();
//
//} catch (IOException e) {
//	// TODO Auto-generated catch block
//	log.append("failed on: " + cmdBait[2] + "\n");
//	log.append("failed on: " + cmdPrey[2] + "\n");
//	e.printStackTrace();
//} catch (InterruptedException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}
//}*/