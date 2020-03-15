package chord;

/*
 * Class used to contain a key value pair, used to store data in nodes
 */
public class Element {
	private int key;
	private String value;
	
	public Element(int key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public int getKey() {
		return key;
	}
	
	@Override
	public boolean equals(Object toCompare) {
		if(this.getClass() == toCompare.getClass()) {
			return this.key == ((Element)toCompare).getKey();
		}
		return super.equals(toCompare);
	}
	@Override
	public int hashCode() {
		return key;
	}
}
