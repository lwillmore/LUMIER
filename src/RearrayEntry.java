public class RearrayEntry implements Comparable<RearrayEntry>{
	
	private static final int VOLUME = 5;
	
	public RearrayEntry(NinetySixWellEntry newEntry, int plate, String well) {
		entry = newEntry;
		newPlate = plate;
		newWell = well;
	}
	
	
	public int newPlate() {
		return newPlate;
	}
	
	public String newWell() {
		return newWell;
	}
	
	// Returns a line of text for the rearray file, which is CSV. Thus delimeters are ','
	public String rearray() {
		String destinationPlate;
		if (newPlate > 10)
			destinationPlate = "Dest" + newPlate;
		else
			destinationPlate = "Dest0" + newPlate;
		
		return this.entry.getTypeAndPlate() + "," + 
				this.entry.getLocation() + "," + 
				destinationPlate + "," +
				newWell + "," +
				VOLUME;
	}
	
	
	public int compareTo (RearrayEntry compEntry) {
		// ascending order (this one greater = positive)
		if (this.newPlate - compEntry.newPlate() == 0) {
			if (this.newWell.charAt(0) - compEntry.newWell().charAt(0) == 0)
				return Integer.parseInt(this.newWell.substring(1)) - Integer.parseInt(compEntry.newWell().substring(1));
			
			return this.newWell.charAt(0) - compEntry.newWell().charAt(0);
		}
		
		 return this.newPlate - compEntry.newPlate();
	}
	
	private NinetySixWellEntry entry;
	private int newPlate;
	private String newWell;
}
