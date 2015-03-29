import java.util.*;


/* holds data from an interaction */
public class Interaction {
	
	public Interaction() {
		name = "";
		comment = "";
		numPlates = 0;
		LumiMap = new HashMap<String, Luminescence>();
	}
	
	public void setName(String s) {
		name = s;
	}
	
	public void setComment(String c) {
		comment = c;
	}
	
	public void setNumPlates(int i) {
		numPlates = i;
	}
	
	public void addData(String s, double[][] data) {
		int rows = data.length;
		int cols = data[0].length;
		LumiMap.put(s, new Luminescence(data, rows, cols));
	}
	
	public String getName() {
		return name;
	}
	
	public String getComment() {
		return comment;
	}	
	
	public int getNumPlates() {
		return numPlates;
	}
	
	public Map<String, Luminescence> getData() {
		return LumiMap;
	}
	
	private String name;
	private String comment;
	private int numPlates;
	private Map<String, Luminescence> LumiMap;
}
