package visualization;

public class Marker {
	public enum MarkerType {TARGET, POINTER};
	
	/**
	 * The id which indicates where to collocate the marker
	 */
	private Long id;
	
	/**
	 * The marker type
	 */
	private MarkerType type;
	
	/**
	 * Instantiate a new Marker, 
	 * 
	 * @param id the id which indicates where to collocate the marker
	 * @param type the marker type
	 */
	public Marker(Long id, MarkerType type) {
		this.id = id;
		this.type = type;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the type
	 */
	public MarkerType getType() {
		return type;
	}
	
	
}
