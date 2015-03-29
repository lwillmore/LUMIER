// STRUCTURE TO HOLD DATA FROM A SINGLE METRIC FOR A SINGLE INTERACTION
public class Luminescence {
	public Luminescence(double[][] d, int runs, int sets) {
		data = d;
		rows = runs;
		cols = sets;
		size = rows*cols;
	}
	
	public void addData(int run, int setnum, int value) {
		data[run][setnum] = value;
	}
	
	public double getSD() {
		double mn = getMean();
		double var = 0;
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < cols; j++)
				var += ((mn - data[i][j])*(mn - data[i][j]));
		return Math.sqrt(var/size);
	}
		
	public int getMean() {
		int sum = 0;
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < cols; j++)
				sum += data[rows][cols];
		return sum/size;
	}
	
	public double getSEM() {
		return getSD()/size;
	}
	
	public double[][] getData() {
		return data;
	}
	
	private double[][] data;
	private int size;
	private int rows;
	private int cols;
}
