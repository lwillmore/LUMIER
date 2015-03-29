/**CreateRearrayAndLayout
 * ----------------------
 * This class controls creating the rearray and layout files. The program must be given controls for bait and prey.
 * Then the user is asked to provide the file containing final PPI, bait, and prey arrangement files.
 * The appropriate outputs are then saved to the same folder.
 */


import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;


public class CreateRearrayAndLayout implements ActionListener {
	

	private static final int REPS = 3;

	public CreateRearrayAndLayout(AnalyzeResults panel, JTextArea text, JPanel buttonPanel) {
		log = text;
		mainPanel = panel;
		String[] preyControlOptions = {"Empty-Reni", "ReniYFP", "ReniMock"};
		String[] baitControlOptions = {"Empty-Fire", "FireYFP", "FireMock"};
		preyBox = new JList(preyControlOptions);
		baitBox = new JList(baitControlOptions);
		buttonPanel.add(new JLabel("Select one or more controls. Bait: "));
		buttonPanel.add(preyBox);
		buttonPanel.add(new JLabel("Prey: "));
		buttonPanel.add(baitBox);
		begin = new JButton("Continue");
		buttonPanel.add(begin);
		begin.addActionListener(this);
		panel.validate();
	}
	
	public void actionPerformed(ActionEvent f) {
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
		}
	}
	
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
	
	private void beginReading(AnalyzeResults panel) {
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
			readFromFiles(baitsFile, preysFile, PPIFile);
			layout();
			log.append("Operation complete.\nYou may now close the program or return to the main menu.\n");
		}
	}
	
	private void readFromFiles(File bait, File prey, File ppi) {
		preyList = ninetySixWellMap(prey);
		baitList = ninetySixWellMap(bait);
		rearray(baitList, preyList, ppi);
	}
	
	private void layout() {
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
	
	private void rearray(Map<String, NinetySixWellEntry> baitList, Map<String, NinetySixWellEntry> preyList, File ppi) {
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
				while (true) {
					if (counter > (96 - REPS*(2 + preyControls.size() + baitControls.size()))) {
						counter = reset(baitList, preyList);
						plate++;
					}
					String line = rd.readLine();
					if (line == null) break;
					String[] elements = line.split("\t");
					String currBait = elements[0];
					String currPrey = elements[1];
					NinetySixWellEntry baitEntry = baitList.get(currBait);
					if (baitEntry == null)
						log.append("Error: " + currBait + " could not be found in the bait list. Skipping interaction: " + line + "\n");
					NinetySixWellEntry preyEntry = preyList.get(currPrey);
					if (preyEntry == null)
						log.append("Error: " + currPrey + " could not be found in the prey list. Skipping interaction" + line + "\n");
					writeToRearray(bw, baitEntry, plate, counter);
					writeToRearray(pw, preyEntry, plate, counter);
					putInLayout(currBait, currPrey, plate, counter);
					counter += REPS;
					if (!baitEntry.getHasControl()) {
						for (int i = 0; i < baitControls.size(); i++) {
							String control = baitControls.get(i);
							writeToRearray(bw, baitEntry, plate, counter);
							NinetySixWellEntry baitControlEntry = baitList.get(control);
							if (baitControlEntry == null) {
								log.append("Error: Bait control " + control + " was not found in baitlist\n");
								return;
							}
							writeToRearray(bw, baitControlEntry, plate, counter);						
							baitEntry.setHasControl(true);
							putInLayout(currBait, control, plate, counter);
							counter += REPS;
						}
					}
					if (!preyEntry.getHasControl()) {
						for (int i = 0; i < preyControls.size(); i++) {
							String control = preyControls.get(i);
							writeToRearray(pw, preyEntry, plate, counter);
							NinetySixWellEntry preyControlEntry = baitList.get(control);
							if (preyControlEntry == null) {
								log.append("Error: Prey control " + control + " was not found in baitlist\n");
								return;
							}
							writeToRearray(bw, preyControlEntry, plate, counter);						
							preyEntry.setHasControl(true);
							putInLayout(currPrey, control, plate, counter);
							counter += REPS;
						}
					}
				}
			rd.close();
			bw.close();
			pw.close();
		} catch (final IOException ex) {
			throw new EmptyStackException();
		}
	}
	
	private void putInLayout(String bait, String prey, int smallPlate, int counter) {
		for (int i = 0; i < REPS; i++) {
			int largePlateIndex = (smallPlate-1)/4;
			String[][] currPlate;
			if (largePlateIndex >= layoutList.size()) {
				currPlate = new String[16][24];
				layoutList.add(currPlate);
			} else {
				currPlate = layoutList.get(largePlateIndex);
			}			
			int oldRow = 1 + counter / 12;
			int oldCol = 1 + counter % 12;
			int lower1 = 0;
			if (smallPlate%4 == 0 || smallPlate % 4 == 3) lower1 = 1;
			int newRow = oldRow*2 - 1 + lower1;
			int newCol = oldCol*2 + (smallPlate-1)%2 - 1;
			String comment = "Sample";
			for (int j = 0; j < baitControls.size(); j ++)
				if (prey.equals(baitControls.get(j))) comment = "Bait control";
			for (int j = 0; j < preyControls.size(); j++)
				if (prey.equals(preyControls.get(j))) comment = "Prey control";
			//log.append("small plate: " +smallPlate+ " rows: " + oldRow + "->" + newRow + " cols: " + oldCol + "->" + newCol +"\n");
			currPlate[newRow-1][newCol-1] = (bait + " + " + prey + "\t" + comment + "\t" + (largePlateIndex+1) + "\n");
			counter ++;
		}
	}
	
	private int reset(Map<String, NinetySixWellEntry> bait, Map<String, NinetySixWellEntry> prey) {
		Iterator<String> bi = bait.keySet().iterator();
		Iterator<String> pi = prey.keySet().iterator();
		while (bi.hasNext())
			bait.get(bi.next()).setHasControl(false);
		while (pi.hasNext())
			prey.get(pi.next()).setHasControl(false);
		return 0;
	}
	
	private void writeToRearray(BufferedWriter b, NinetySixWellEntry e, int plate, int counter) {
		for (int i = 0; i < REPS; i++) {
			try {
				String empty0 = "0";
				if (plate > 9) empty0 = "";
				char row = (char)('A' + counter / 12);
				int col = 1 + (counter % 12);
				b.write(e.getTypeAndPlate() + "," + e.getLocation() + "," +
						"Dest" + empty0 + plate + "," + row + col + ",5\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			counter++;
		}
	}
	
	private Map<String, NinetySixWellEntry> ninetySixWellMap(File file) {
		Map<String, NinetySixWellEntry> toReturn = new HashMap<String, NinetySixWellEntry>();
		try {
			final BufferedReader rd = new BufferedReader(new FileReader(file));
				while (true) {
					String line = rd.readLine();
					if (line == null) break;
					String[] elements = line.split("\t");
					toReturn.put(elements[0], 
							new NinetySixWellEntry(elements[1], elements[2]+ elements[3], false));
				}
			rd.close();
			} catch (final IOException ex) {
				throw new EmptyStackException();
			}
		return toReturn;
	}
	
	private ArrayList<String[][]> layoutList;
	private AnalyzeResults mainPanel;
	private Map<String, NinetySixWellEntry> preyList;
	private Map<String, NinetySixWellEntry> baitList;
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
