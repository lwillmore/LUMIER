import java.util.*;
import jxl.*;


/* LumData holds the luminescence data from a single point */
public class LumData {
	
	/*constructor*/
	public LumData() {
		dataBase = new ArrayList<Interaction>();
	}

	public int size() {
		return dataBase.size();
	}
	
	public Interaction dataPoint(int i) {
		return dataBase.get(i);
	}
	
	
	public void loadDataFromFormatted(Sheet dataSheet) {
		int row = 0;
		ArrayList<String> lumLabels = getLumLabels(dataSheet);
		while (true) {
			if (dataSheet.getCell(0, row).getContents().isEmpty()) break;
			// iterate over all of the interactions to load data first about name, comment, and numplates
			Interaction interact = new Interaction();
			interact.setName(dataSheet.getCell(0, row).getContents());
			interact.setComment(dataSheet.getCell(2, row).getContents());
			interact.setNumPlates(Integer.parseInt(dataSheet.getCell(3, row).getContents()));
			// iterate over the luminescence data with all three 
			int col = 4;
			while (true) {
				if (dataSheet.getCell(col, 4).getContents().isEmpty()) break;
				int runs = 3, sets = 2;
				double[][] dataNumbers = new double[runs][sets];
				for (int i = 0; i < runs; i++) {
					for (int j = 0; j < sets; j ++) {
						dataNumbers[i][j] = Double.parseDouble(dataSheet.getCell(row + i, col + j).getContents());
					}
				}
				interact.addData(lumLabels.get(col/5), dataNumbers);
				col += 5;
			}
			row += 3;
			dataBase.add(interact);
		}
	}
	
	private ArrayList<String> getLumLabels(Sheet dataSheet) {
		ArrayList<String> toReturn = new ArrayList<String>();
		int col = 4;
		while (true) {
			String label = dataSheet.getCell(1, col).getContents();
			if (label.isEmpty()) break;
			toReturn.add(label);
			col +=5;
		}
		return toReturn;
	}
	
	private ArrayList<Interaction> dataBase;
}
