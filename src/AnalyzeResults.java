/**AnalyzeResults
 * --------------
 * Provides main function and the primary GUI upon which all other functionality is dependent.
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class AnalyzeResults extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 6297745681345358113L;
	
	// Sets up the common panel that is used for the entirety of the program. (in order to preserve the common log).
	public static void main(String[] args) {
		JFrame frame = new JFrame("LUMIER Analysis 2.0");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new AnalyzeResults());
		frame.pack();
		frame.setVisible(true);
	}
	
	// constructor creates the initial layout and log pain. adds buttons for 3 major functions:
	// 1) create rearray and layout files
	// 2) format raw data
	// 3) analyze formatted data 
	public AnalyzeResults() {
		super( new BorderLayout());
		log = new JTextArea(25, 80);
		log.setMargin(new Insets(5,5,5,5));
		log.setEditable(false);
		logScrollPane = new JScrollPane(log);
		buttonPanel = new JPanel();
		rawData = new JButton("Get Results from Raw Data");
		formattedData = new JButton("Analyze Results from Formatted Data");
		createRearray = new JButton("Create Rearray and Layout Files");
		addButton(createRearray);
		addButton(rawData);
		//addButton(formattedData);
		add(buttonPanel, BorderLayout.PAGE_START);
		add(logScrollPane, BorderLayout.CENTER);
		back = new JButton("Main Menu");
		log.append("Please select one of the above options to begin.\n");
	}
	
	private void addButton(JButton button) {
		button.addActionListener(this);
		buttonPanel.add(button);
	}

	@SuppressWarnings("unused")
	/* Each button represents a function. Each function is carried out by a different type.
	 * The types are: AnalyzeRaw (creates formatted file), AnalyzeFormatted (allows for visualizing data from formatted file),
	 * and CreateRarrayAndLayout (creates the rearray and layout files needed to perform the actually assay.)
	 * In each instance the main panel and textArea are passed as parameters, so that they can be modified and error messages
	 * can be printed to a common space.
	 */
	public void actionPerformed(final ActionEvent e) {
		final Object selected = e.getSource();
		if (selected == rawData) {
			buttonPanel.removeAll();
			validate();
			AnalyzeRaw ar =  new AnalyzeRaw(this, log, buttonPanel);
			addButton(back);
			validate();
		} else if (selected == formattedData) {
			buttonPanel.removeAll();
			validate();
			AnalyzeFormatted af =  new AnalyzeFormatted(this, log, buttonPanel);
			addButton(back);
			validate();
		} else if (selected == createRearray) {
			buttonPanel.removeAll();
			validate();
			
			// CHANGED HERE TO ENCORPORATED NEW LAYOUT ARRANGEMENT!!!!!!!!!!!!!!!!!!
			CreateRearrayAndLayout3 create = new CreateRearrayAndLayout3(this, log, buttonPanel);
			
			
			addButton(back);
			validate();
		} else if (selected == back) {
			// clears all elements from the main panel and then adds back the main menu buttons + main menu panel
			buttonPanel.removeAll();
			validate();
			buttonPanel.add(createRearray);
			buttonPanel.add(rawData);
			//buttonPanel.add(formattedData);
			validate();
			removeAll();
			validate();
			add(logScrollPane, BorderLayout.CENTER);
			add(buttonPanel, BorderLayout.PAGE_START);
			validate();
		}
	}
	
	
	/* Instance variables: interactors and constants. */
	private JButton createRearray;
	private JButton rawData;
	private JButton formattedData;
	private JButton back;
	private JPanel buttonPanel;
	private JTextArea log;
	private JScrollPane logScrollPane;
}