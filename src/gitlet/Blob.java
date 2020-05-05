package gitlet;

import java.io.File;
import java.io.Serializable;

class Blob implements Serializable {

	/**
	 * Prevent serialization failure with function modification
	 */
	private static final long serialVersionUID = -1612687192495533913L;
	public Blob() {
		this.fullName = "";
		this.version = "";
		this.content = "";
		this.blobID = "";
	}

	void dump(Blob blob) {
		this.fullName = blob.fullName;
		this.version = blob.version;
		this.content = blob.content;
		this.blobID = blob.blobID;
	}


	static Blob load(String blobID) {
		File file = Utils.join(Config.BLOBS_FOLDER, blobID);
		if (!file.exists()) {
			return null;
		}
		return Utils.readObject(file, Blob.class);
	}

	static Blob loadStageConcat(String concatName) {
		File file = Utils.join(Config.STAGEAREA_FOLDER, concatName);
		if (!file.exists()) {
			return null;
		}
		return Utils.readObject(file, Blob.class);
	}

	static Blob loadStageFull(String fullName) {
		return loadStageConcat(concatPath(fullName));
	}

	static Blob loadStage(File file) {
		if (!file.exists()) {
			return null;
		}
		return Utils.readObject(file, Blob.class);
	}

	void saveToWorkdir() {
		Utils.writeContents(Utils.join(Config.CWD, fullName), content);
	}
	void removeFromWorkDir() {
		Utils.join(Config.CWD, fullName).delete();
	}

	void persistenceStage() {
		String s = concatPath(fullName);
		File file = new File(Config.STAGEAREA_FOLDER, s);
		persistence(file);
	}
	
	void stageAdd(String fullName, String content, String version) {
		setFullName(fullName);
		setOperCmd(Config.CMD_ADD);
		setContent(content);
		setVersion(version);
		resetBlobID();
		persistenceStage();
	}
	void stageMergeAdd(String version) {
		setOperCmd(Config.CMD_MERGE_ADD);
		setVersion(version);
		persistenceStage();
	}
	void stageMergeRemove() {
		setOperCmd(Config.CMD_MERGE_RM);
		persistenceStage();
	}
	void stageMergeConflict(String currContent, String mergeContent, String version) {
		setVersion(version);
		setOperCmd(Config.CMD_MERGE_CONFLICT);
		setContent(String.format(Config.MERGE_STRING,currContent,mergeContent));
		resetBlobID();
		persistenceStage();
	}

	void persistence() {
		File file = Utils.join(Config.BLOBS_FOLDER, blobID);
		persistence(file);
	}

	void persistence(File file) {
		Utils.writeObject(file, this);
	}

	String getFullName() {
		return fullName;
	}

	void setFullName(String fullName) {
		this.fullName = fullName;
	}

	String getVersion() {
		return version;
	}

	void setVersion(String version) {
		this.version = version;
	}

	String getContent() {
		return content;
	}

	void setContent(String content) {
		this.content = content;
	}

	String getBlobID() {
		return blobID;
	}

	void setBlobID(String blobID) {
		this.blobID = blobID;
	}

	void resetBlobID() {
		this.blobID = Utils.sha1(this.fullName, this.content);
	}

	String getOperCmd() {
		return operCmd;
	}

	void setOperCmd(String operCmd) {
		this.operCmd = operCmd;
	}
	
	public String toString() {
		return String.format("%s-%s-%s-%s", fullName,operCmd,version,blobID);
	}

	/** Replace path separator with '~' */
	static String concatPath(String fullname) {
		return String.join(Config.CONCAT_CHAR, fullname.split("[\\|/]"));
	}

	static String concatPath(String path, String file) {
		return concatPath(path) + Config.CONCAT_CHAR + concatPath(file);
	}

	/** Process path separator to '/' */
	static String uniquePath(String filename) {
		String s = filename;
		if (filename.startsWith("./") || filename.startsWith(".\\")) {
			s = filename.substring(2);
		}
		return s.replace('\\', '/');
	}


	private String fullName = "";
	private String version = "";
	private String content = "";
	private String blobID = "";
	private String operCmd = Config.CMD_NONE;
}
