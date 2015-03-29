
public class NinetySixWellEntry {
	public NinetySixWellEntry(String typeAndPlate, String location, boolean hasControl) {
		t = typeAndPlate;
		l = location;
		hasC = hasControl;
	}
	
	
	public String getTypeAndPlate() {
		return t;
	}
	
	public String getLocation() {
		return l;
	}
	
	
	public boolean getHasControl() {
		return hasC;
	}
	
	public void setHasControl(boolean b) {
		hasC = b;
	}
	
	private String t;
	private String l;
	private boolean hasC;
}
