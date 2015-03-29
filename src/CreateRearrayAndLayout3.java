import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.util.*;


public class CreateRearrayAndLayout3 implements ActionListener{
	
	private static final int REPS = 3;
	private static final int REPS_empty = 2;
	private static final int REPS_tandem = 4;
	private static final int TANDEM_COUNT_START = 358;
	private static final String[] LYSATEC = {"BAD", "BCL2L1"};
	private static final String[] TANDEMC = {"pPAReni-Fire", "pPAReni-Fire"};
	private static final String[] EMPTYC = {"pcDNA3.1", "pcDNA3.1"};

	
	public CreateRearrayAndLayout3(AnalyzeResults panel, JTextArea text, JPanel buttonPanel) {
		log = text;
		mainPanel = panel;
		bp = buttonPanel;
		canContinue = true; 
		getAbsoluteControls();
	}
	
	/* 
	 * Collect names of absolute positive and negative controls. 
	 */
	public void getAbsoluteControls() {
		posControl = new ArrayList <String []>();
		negControl = new ArrayList <String []>();
		
		log.append("Please provide positive and negative controls.\nThen select next to continue.\n");
		posNegBox = new JList(new String[] {"Positive", "Negative"});
		moreAbsCButton = new JButton("Add Control: Bait name");
		doneAbsCButton = new JButton("Next");
		nameField = new JTextField(20);

		moreAbsCButton.addActionListener(this);
		doneAbsCButton.addActionListener(this);

		bp.add(posNegBox);
		bp.add(nameField);
		bp.add(moreAbsCButton);
		bp.add(doneAbsCButton);
		mainPanel.validate();
	}
	
	/* 
	 * Collect the names of the regular controls. 
	 */
	private void getRegularControls() {
		// Removes the absolute control boxes
		bp.remove(posNegBox);
		bp.remove(nameField);
		bp.remove(moreAbsCButton);
		bp.remove(doneAbsCButton);
		mainPanel.validate();
		
		// Initialize the boxes for the regular controls
		String[] preyControlOptions = {"Empty-Reni", "ReniCherry", "ReniMock"};
		String[] baitControlOptions = {"Empty-Fire", "FireCherry", "FireMock"};
		preyBox = new JList(preyControlOptions);
		baitBox = new JList(baitControlOptions);
		skipRepeatedControlBox = new JCheckBox();
		bp.add(new JLabel("Bait controls: "));
		bp.add(baitBox);
		bp.add(new JLabel("Prey controls: "));
		bp.add(preyBox);
		bp.add(new JLabel("Skip repeated controls:"));
		bp.add(skipRepeatedControlBox);
		begin = new JButton("Continue");
		bp.add(begin);
		mainPanel.validate();

		// Collect the user's entries
		baitRearrayList = new ArrayList<RearrayEntry>();
		preyRearrayList = new ArrayList<RearrayEntry>();

		begin.addActionListener(this);
		log.append("EMPTY control set as: " + EMPTYC[0] + " + " + EMPTYC[1] +"\n");
		log.append("TANDEM Construct control set as: " + TANDEMC[0] + " + " + TANDEMC[1] +"\n");
		log.append("LYSATE control set as: " + LYSATEC[0] + " + " + LYSATEC[1] +"\n");
	}
	
	
	/*
	 * Main functionality:
	 * Begins by pulling in files from directory, throwing error if any files out of place.
	 * Calls readFromFiles and writeToFiles to complete the program.
	 */
	private void beginReading(AnalyzeResults panel) {
		
		// JFileChooser boilerplate to select correct directory containing "final" interaction files
		// The resulting files will be added here
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setApproveButtonText("Select run folder containing final_Baits, final_Preys, and final_PPI files");
	    int returnVal = chooser.showOpenDialog(panel);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			directory = chooser.getSelectedFile();
			runLabel = directory.getName();
			if (!runLabel.contains("run"))
				log.append("The folder you have selected is not labeled with the run.\nFiles will not be labeled with the run number.\n");
	    } else {
	    	log.append("Please select file.\n");
	    	return;
	    }
		
		// Read through the files in the directory, using those that are appropriate
		File[] filesInDirectory = directory.listFiles();
		File baitsFile = null, preysFile = null, PPIFile = null;
		int successfulReads = 0;
		for (int i = 0; i < filesInDirectory.length; i++) {
			if (filesInDirectory[i].getName().toLowerCase().contains("final_baits")) {
				baitsFile = filesInDirectory[i];
				successfulReads++;
			} else if (filesInDirectory[i].getName().toLowerCase().contains("final_preys")) {
				preysFile = filesInDirectory[i];
				successfulReads++;
			} else if (filesInDirectory[i].getName().toLowerCase().contains("final_ppi")) {
				PPIFile = filesInDirectory[i];
				successfulReads++;
			}
		}
		
		if (successfulReads != 3) {
			log.append("Error in finding files:\n");
			if (preysFile == null) log.append("final_Preys\n");
			if (baitsFile == null) log.append("final_Baits\n");
			if (PPIFile == null) log.append("final_PPI\n");
		} else {
			// Take in information from files
			readFromFiles(baitsFile, preysFile);
			writeToFiles(baitList, preyList, PPIFile);
			log.append("Operation complete.\nYou may now close the program or return to the main menu.\n");
		}
	}
	
	
	/*
	 * Maps ninety-six well plates from file
	 */
	private void readFromFiles(File bait, File prey) {
		preyList = ninetySixWellMap(prey);
		baitList = ninetySixWellMap(bait);
	}
	
	
	/*
	 *  Creates map from bait name to their original location (on lysis plate)
	 */
	private Map<String, NinetySixWellEntry> ninetySixWellMap(File file) {
		Map<String, NinetySixWellEntry> toReturn = new HashMap<String, NinetySixWellEntry>();
		try {
			final BufferedReader rd = new BufferedReader(new FileReader(file));
				while (true) {
					String line = rd.readLine();
					if (line == null) break;
					String[] elements = line.split("\t");
					toReturn.put(elements[0], 
							new NinetySixWellEntry(elements[1], elements[2] + elements[3], false));
				}
			rd.close();
			} catch (final IOException ex) {
				throw new EmptyStackException();
			}
		return toReturn;
	}
	
	
	/*
	 * Big fat writing to the file function. By creating a layout list first and writing to rearray.
	 * Then there is a call to write the layout file.
	 */
	private void writeToFiles(Map<String, NinetySixWellEntry> baitList, Map<String, NinetySixWellEntry> preyList, File ppi) {
		try {
			final BufferedReader rd = new BufferedReader(new FileReader(ppi));
			File baitsRearray = new File(directory.getAbsolutePath() + "/LUMIER_Baits_Rearray_" + runLabel+ ".csv");
			File preysRearray = new File(directory.getAbsolutePath() + "/LUMIER_Preys_Rearray_" + runLabel+ ".csv");

			baitsRearray.createNewFile();
			preysRearray.createNewFile();

			BufferedWriter bw = new BufferedWriter(new FileWriter(baitsRearray.getAbsoluteFile()));
			BufferedWriter pw = new BufferedWriter(new FileWriter(preysRearray.getAbsoluteFile()));
			
			int counter = 0;
			int plate = 1;
			String currPlate[][] = new String[16][24];
			layoutList.add(currPlate);
			writeControls(bw, pw, plate);
			
			while(true) {
				String line = rd.readLine();
				if (line == null) break; // EOF
				// If there is no room for the next interaction set on the current plate, start a new plate.
				if (counter + REPS*(2 + baitControls.size() + preyControls.size()) 
						>= absoluteControlCountStart) {
					plate ++;
					currPlate = new String[16][24];
					layoutList.add(currPlate);
					writeControls(bw, pw, plate);
					
					reset(baitList, preyList);
					counter = 0;
				}
				
				String[] elements = line.split("\t");
				String currBait = elements[0];
				String currPrey = elements[1];
				
				// IF IT'S A CONTROL SKIP IT
				if (isAbsoluteControl(elements))
					continue;
				
				// read bait field
				NinetySixWellEntry baitEntry = baitList.get(currBait);
				if (baitEntry == null) {
					log.append("Error: " + currBait + " could not be found in the bait list. Skipping interaction: " + line + "\n");
					continue;
				}
				
				// read prey field
				NinetySixWellEntry preyEntry = preyList.get(currPrey);
				if (preyEntry == null) {
					log.append("Error: " + currPrey + " could not be found in the prey list. Skipping interaction" + line + "\n");
					continue;
				}
				
				String comment = currBait + " + " + currPrey + "\t" + "Sample\t" + plate + "\n";
				writeToRearray(true, currBait, baitEntry, plate, counter, REPS);
				writeToRearray(false, currPrey, preyEntry, plate, counter, REPS);
				putInLayout(plate, comment, counter, REPS);
				counter += REPS;
				
				// DO CONTROLS
				if (!baitEntry.getHasControl()) {
					for (int i = 0; i < baitControls.size(); i++) {
						String control = baitControls.get(i);
						NinetySixWellEntry baitControlEntry = baitList.get(control);
						if (baitControlEntry == null) {
							log.append("Error: Bait control " + control + " was not found in baitlist\n");
							return;
						}
						writeToRearray(true, currBait, baitEntry, plate, counter, REPS);
						writeToRearray(true, control, baitControlEntry, plate, counter, REPS);	
						comment = currBait + " + " + control + "\t" + "Bait control\t" + plate + "\n";
						putInLayout(plate, comment, counter, REPS);
						counter += REPS;
					}
					if (skipRepeatedControlBox.isSelected()) baitEntry.setHasControl(true);
				}
				
				if(!preyEntry.getHasControl()) {
					for (int i = 0; i < preyControls.size(); i++) {
						String control = preyControls.get(i);
						NinetySixWellEntry preyControlEntry = baitList.get(control);
						if (preyControlEntry == null) {
							log.append("Error: Prey control " + control + " was not found in baitlist\n");
							return;
						}
						writeToRearray(false, currPrey, preyEntry, plate, counter, REPS);
						writeToRearray(true, control, preyControlEntry, plate, counter, REPS);
						comment = control + " + " + currPrey + "\t" + "Prey control\t" + plate + "\n";
						putInLayout(plate, comment, counter, REPS);
						counter += REPS;
					}
					if (skipRepeatedControlBox.isSelected()) preyEntry.setHasControl(true);
				}
			}
			

			rd.close();
			writeToLayout();
			sortAndWriteRearray(bw, pw);
		} catch (final IOException ex) {
			throw new EmptyStackException();
		}
	}
	
	/* 
	 * Need to write the controls manually to the end of every new 384-well plate 
	 */
	private void writeControls(BufferedWriter baitWriter, BufferedWriter preyWriter, int plateNum) {
		boolean passedTandem = false;
		// 1) Last = tandem control
		int counter = TANDEM_COUNT_START; // the counter is 0-indexed, defines where the stuff starts
		writeControl(baitWriter, preyWriter, plateNum, TANDEMC, counter, REPS_tandem);
		
		// 2) lysate control (remember this guy needs all the regular controls)
		counter = 384 - (1 + baitControls.size() + preyControls.size())*REPS;
		writeControl(baitWriter, preyWriter, plateNum, LYSATEC,counter, REPS);
		
		// 3) Next = empty control
		counter -= REPS_empty;
		writeControl(baitWriter, preyWriter, plateNum, EMPTYC, counter, REPS_empty);
		
		// 4)  all the negative controls (last to first order)
		for (int i = negControl.size() - 1; i >= 0; i--) {
			counter -= (1 + baitControls.size() + preyControls.size())*REPS; // regular controls
			if (!passedTandem && counter <= TANDEM_COUNT_START) {
				counter -= REPS_tandem;
				passedTandem = true;
			}
			writeControl(baitWriter, preyWriter, plateNum, negControl.get(i), counter, REPS);
		}
		
		// 5) Second to last = all the positive controls (last to first order)
		for (int i = posControl.size() - 1; i >= 0; i--) {
			counter -= (1 + baitControls.size() + preyControls.size())*REPS; // regular controls
			if (!passedTandem && counter <= TANDEM_COUNT_START) {
				counter -= REPS_tandem;
				passedTandem = true;
			}
			writeControl(baitWriter, preyWriter, plateNum, posControl.get(i), counter, REPS);
		}
		
		// set the limit for normal counts to the current counter
		absoluteControlCountStart = counter;
	}
	
	/*
	 * Writes an absolute control to the plate.
	 */
	private void writeControl(BufferedWriter baitWriter, BufferedWriter preyWriter, int plateNum,
			String[] controlNames, int counter, int reps) {
		
		String currBait = controlNames[0];
		String currPrey = controlNames[1];
		
		// read bait field
		NinetySixWellEntry baitEntry = baitList.get(currBait);
		if (baitEntry == null) {
			log.append("Error: " + currBait + " could not be found in the bait list. This is an absolute control. Progress terminated.\n");
			return;
		}
		
		// read prey field
		
		// note that empty or tandem control should only be found in the final_baits list, so rearraying for this
		// is done exclusively in baits lists
		NinetySixWellEntry preyEntry;
		if (controlNames == EMPTYC || controlNames == TANDEMC) 
			preyEntry = baitList.get(currPrey);
		else 
			preyEntry = preyList.get(currPrey);
		if (preyEntry == null) {
			log.append("Error: " + currPrey + " could not be found in the prey list. This is an absolute control. Progress terminated.\n");
			return;
		}
		
		if (controlNames != LYSATEC) {
			writeToRearray(true, currBait, baitEntry, plateNum, counter, reps);
			if (controlNames == EMPTYC || controlNames == TANDEMC) writeToRearray(true, currPrey, preyEntry, plateNum, counter, reps);
			else writeToRearray(false, currPrey, preyEntry, plateNum, counter, reps);
		}
		String comment = currBait + " + " + currPrey + "\t" + "Absolute control\t" + plateNum + "\n";
//		log.append(comment);
		putInLayout(plateNum, comment, counter, reps);	
		
		
		// Do controls for Positive, Negative, and Lysate controls
		if (reps == REPS) {
			if (counter <= TANDEM_COUNT_START && counter + reps > TANDEM_COUNT_START)
				counter += 4;
			counter += reps;
			for (int i = 0; i < baitControls.size(); i++) {
				String control = baitControls.get(i);
				NinetySixWellEntry baitControlEntry = baitList.get(control);
				if (baitControlEntry == null) {
					log.append("Error: Bait control " + control + " was not found in baitlist\n");
					return;
				}
				if (controlNames != LYSATEC) {
					writeToRearray(true, currBait, baitEntry, plateNum, counter, reps);
					writeToRearray(true, control, baitControlEntry, plateNum, counter, reps);
				}
				comment = currBait + " + " + control + "\t" + "Bait control\t" + plateNum + "\n";
				putInLayout(plateNum, comment, counter, reps);
				if (counter <= TANDEM_COUNT_START && counter + reps > TANDEM_COUNT_START)
					counter += 4;
				counter += reps;
			}
		
			for (int i = 0; i < preyControls.size(); i++) {
				String control = preyControls.get(i);
				NinetySixWellEntry preyControlEntry = baitList.get(control);
				if (preyControlEntry == null) {
					log.append("Error: Prey control " + control + " was not found in baitlist\n");
					return;
				}
				if (controlNames != LYSATEC) {
					writeToRearray(false, currPrey, preyEntry, plateNum, counter, reps);
					writeToRearray(true, control, preyControlEntry, plateNum, counter, reps);
				}
				comment = control + " + " + currPrey + "\t" + "Prey control\t" + plateNum + "\n";
				putInLayout(plateNum, comment, counter, reps);
				if (counter <= TANDEM_COUNT_START && counter + reps > TANDEM_COUNT_START)
					counter += 4;
				counter += reps;
			}
		}
	}
	
	/* 
	 * Converts the counter number of the 384 well plate to and converts it to an index in 96-well
	 */
	private void writeToRearray(Boolean isBait, String name, NinetySixWellEntry e, int plate, int counter, int reps) {
		for (int i = 0; i < reps; i++) {
			if (counter == TANDEM_COUNT_START && reps != REPS_tandem) counter +=4;
			int rownum384well = counter/24;
			int colnum384well = counter%24; // counter is 0 indexed so the columns are as well
			boolean oddRow = rownum384well % 2 == 1;
			boolean oddCol = colnum384well % 2 == 1;
			
			if (oddRow) colnum384well = 23 - colnum384well;
			
			int destPlate = (plate-1)*4 + 1; //plate is 1-indexed
			if (!oddRow && oddCol) 
				destPlate++;
			else if (oddRow && oddCol) 
				destPlate += 2;
			else if (oddRow && !oddCol)
				destPlate += 3;
			
			char row = (char)('A' + rownum384well / 2);
			int col = 1 + colnum384well / 2;
			
			
			if (isBait)
				baitRearrayList.add(new RearrayEntry (e, destPlate, "" + row + col));
			else
				preyRearrayList.add(new RearrayEntry (e, destPlate, "" + row + col));

//			log.append("364 Well: Plate " + plate + ", Counter " + counter + " " + (char)('A' + rownum384well) +
//					"" + (colnum384well + 1) + " Dest " + destPlate + ", " + row + col + '\n');
//			
			counter++;
		}
	}
	
	/*
	 * Sorts the rearrayed entries and writes to the files
	 */
	void sortAndWriteRearray(BufferedWriter bw, BufferedWriter pw) {
		Collections.sort(baitRearrayList);
		Collections.sort(preyRearrayList);

		try {
			for (int i = 0; i < baitRearrayList.size(); i++)
				bw.write(baitRearrayList.get(i).rearray() + "\n");
			
			for (int i = 0; i < preyRearrayList.size(); i++)
				pw.write(preyRearrayList.get(i).rearray() + "\n");
			
			bw.close();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Gives an location in the 384-well plate.
	 */
	private void putInLayout(int plate, String comment, int counter, int reps) {
		for (int i = 0; i < reps; i++) {
			if (counter == TANDEM_COUNT_START && !comment.contains(TANDEMC[0])) counter +=4;
			String currPlate[][] = layoutList.get(plate-1);
			int rownum384well = counter/24;
			int colnum384well = counter%24; // counter is 0 indexed
			if (rownum384well % 2 == 1) colnum384well = 23 - colnum384well;
			
			// currPlate[col][row] i.e. [16][24]
			currPlate[rownum384well][colnum384well] = comment;
			counter ++;
		}
	}
	
	/* 
	 * Write the layout information to file.
	 */
	private void writeToLayout() {
		File layoutFile = new File(directory.getAbsolutePath() + "/LUMIER_Layout_" + runLabel+ ".txt");
			try {
				layoutFile.createNewFile();
				BufferedWriter lw = new BufferedWriter(new FileWriter(layoutFile.getAbsoluteFile()));
				lw.write("Sample\tComment\t384-Well plate\n");
				for (int i = 0; i < layoutList.size(); i++) {
					String[][] currPlate = layoutList.get(i);
					for (int j = 0; j < 16; j++) {
						for (int k = 0; k < 24; k++) {
							String contents = currPlate[j][k];
							if (contents == null) {
								lw.write("Blank\tBlank\t" + (i+1) + "\n");
							} else {
								lw.write(contents);
							}
						}
					}
				}
				lw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	/*
	 * ACTION EVENTS :: For absolute controls, regular controls, etc.
	 */
	public void actionPerformed(ActionEvent f) {
		// Begin analysis
		if (f.getSource() == begin) {
			preyControls = objectVectorToStringArray(preyBox.getSelectedValues());
			baitControls = objectVectorToStringArray(baitBox.getSelectedValues());
			if (preyControls.size() == 0 || baitControls.size() == 0) {
				log.append("Please make a selection for bait and prey controls\n");
			} else {
				if (preyControls.size() != baitControls.size())
					log.append("You have chosen an unequal number of bait and prey controls, but continuing anyway...");
				layoutList = new ArrayList<String[][]>();
				beginReading(mainPanel);
			}
		
		// Absolute control actions
		} else if (f.getSource() == moreAbsCButton) {
			String controlName = nameField.getText();
			if (controlName.length() == 0) {
				log.append ("Please enter a name for your control or contrinue.\n");
				return;
			}
			
			if (posNegBox.getSelectedValue() == null) {
				log.append("Please select the control type: Positive or Negative.\n");
				return;
			}
			
			String[] controlPair = {controlName, ""};
			
			// If you are now entering the first of a pair
			if (canContinue) {
				if (posNegBox.getSelectedIndex() == 0) {
					posControl.add(controlPair);
					addToPositive = true;
				} else {
					negControl.add(controlPair);
					addToPositive = false;
				}
				canContinue = false; 
				bp.remove(doneAbsCButton);
				nameField.setText("");
				moreAbsCButton.setText("Add Control: Prey name");
				bp.validate();
			// If you are now entering the second of a pair.
			} else {
				if (addToPositive) {
					int numControls = posControl.size();
					posControl.get(numControls - 1)[1] = controlName; 
					log.append("Added positive control " + numControls + " :" + posControl.get(numControls - 1)[0] + 
							" + "+ posControl.get(numControls - 1)[1] + "\n");
				} else {
					int numControls = negControl.size();
					negControl.get(numControls - 1)[1] = controlName;
					log.append("Added neagtive control " + numControls + " :" + negControl.get(numControls - 1)[0] +
							" + "+ negControl.get(numControls - 1)[1] + "\n");
				}
				canContinue = true;
				bp.add(doneAbsCButton);
				nameField.setText("");
				moreAbsCButton.setText("Add Control: Bait name");
				bp.validate();
			}
		
		// When you are done adding the regular controls, add the other controls and continue
		} else if (f.getSource() == doneAbsCButton) {
			getRegularControls();
		}
	}
	
	/*
	 * Changes the labels of objects in an array into a input
	 */
	ArrayList<String> objectVectorToStringArray(Object[] input) {
		ArrayList<String> toReturn = new ArrayList<String>();
		log.append("You have selected:\n");
		for (int i = 0; i < input.length; i++) {
			String control = (String)input[i];
			log.append(control + "\n");
			toReturn.add(control);
		}
		return toReturn;
	}
	
	
	/*
	 * Returns if the interaction represented in elements is found in any of the control groups: 
	 * EMPTY
	 * TANDEM
	 * LYSATE
	 * POSITIVE
	 * NEGATIVE
	 */
	boolean isAbsoluteControl(String[] elements) {
		boolean isHardControl = ((Arrays.asList(EMPTYC).contains(elements[0]) ||
				Arrays.asList(TANDEMC).contains(elements[0]) ||
				Arrays.asList(LYSATEC).contains(elements[0])) &&
				(Arrays.asList(EMPTYC).contains(elements[1]) ||
				 Arrays.asList(TANDEMC).contains(elements[1]) ||
				 Arrays.asList(LYSATEC).contains(elements[1])));
		boolean result = isHardControl || 
		                 posControl.contains(elements) || 
		                 negControl.contains(elements);
		return result;
				 
	}
	
	/* 
	 * Resets all bat and prey entries so that they all need a new control.
	 * Should be called whenever starting a new 384-well plate.
	 */
	private void reset(Map<String, NinetySixWellEntry> bait, Map<String, NinetySixWellEntry> prey) {
		Iterator<String> bi = bait.keySet().iterator();
		Iterator<String> pi = prey.keySet().iterator();
		while (bi.hasNext())
			bait.get(bi.next()).setHasControl(false);
		while (pi.hasNext())
			prey.get(pi.next()).setHasControl(false);
	}
	
	
	
	private JPanel bp;
	
	// for adding absolute controls
	private JList posNegBox;
	private JTextField nameField;
	private JButton moreAbsCButton;
	private JButton doneAbsCButton;
	private boolean canContinue; 
	private boolean addToPositive;
	
	private ArrayList <String []> posControl;
	private ArrayList <String []> negControl;
	private ArrayList<RearrayEntry> baitRearrayList;
	private ArrayList<RearrayEntry> preyRearrayList;
	private ArrayList<String[][]> layoutList;
	private AnalyzeResults mainPanel;
	
	private int absoluteControlCountStart;
	
	private Map<String, NinetySixWellEntry> preyList;
	private Map<String, NinetySixWellEntry> baitList;
	private JCheckBox skipRepeatedControlBox;
	
	private JList preyBox;
	private JList baitBox;
	private JButton begin;
	private JTextArea log;
	private JFileChooser chooser;
	private File directory;
	private String runLabel;
	private ArrayList<String> baitControls;
	private ArrayList<String> preyControls;
}
