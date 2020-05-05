package gitlet;

import java.io.Serializable;
import java.util.HashMap;

class Tree implements Serializable{
	/**
	 * Prevent serialization failure with function modification
	 */
	private static final long serialVersionUID = 19431424064229843L;


	public Tree() {
		blobs = new HashMap<>();
	}

	public void dump(Tree tree) {
		blobs.clear();
		blobs.putAll(tree.blobs);
	}
	boolean tracked(String fullName) {
		return blobs.containsKey(fullName);
	}
	void addBlob(String fullName, String blobID) {
		blobs.put(fullName, blobID);
	}
	void removeBlob(String fullName) {
		blobs.remove(fullName);
	}
	HashMap<String,String> minus(Tree otherTree){
		HashMap<String, String> aMap = new HashMap<>();
		for (java.util.Map.Entry<String, String> entry : blobs.entrySet()) {
			if (!otherTree.tracked(entry.getKey())) {
				aMap.put(entry.getKey(), entry.getValue());
			}
		}
		return aMap;
	}
	
	
	HashMap<String, String> getBlobs() {
		return blobs;
	}


	/** <fullName, blobID> */
	private HashMap<String, String> blobs;
}
