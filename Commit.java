package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

class Commit implements Serializable {
	/**
	 * Prevent serialization failure with function modification
	 */
	private static final long serialVersionUID = -7969295885238112334L;

	public Commit() {
		childs = new HashMap<>();
	}

	String getMessage() {
		return message;
	}

	void setMessage(String message) {
		this.message = message;
	}

	long getCommitDate() {
		return commitDate;
	}

	void setCommitDate(long commitDate) {
		this.commitDate = commitDate;
	}

	void setCommitDate() {
		this.commitDate = new Date().getTime();
	}

	String getParentCommitID() {
		return parentCommitID;
	}

	void setParentCommitID(String parentCommitID) {
		this.parentCommitID = parentCommitID;
	}

	String getCommitID() {
		return commitID;
	}

	void setCommitID(String sha1Code) {
		this.commitID = sha1Code;
	}

	void resetCommitID() {
		this.commitID = Utils.sha1(this.toString());
	}

	String getVersion() {
		return String.format("%s_%d", branchID, version);
	}

	void setVersion(int version) {
		this.version = version;
	}

	void additionVersion() {
		version += 1;
	}

	String getBranchID() {
		return branchID;
	}

	void setBranchID(String branchID) {
		this.branchID = branchID;
	}

	String getMergeID() {
		return mergeID;
	}

	void setMergeID(String mergeID) {
		this.mergeID = mergeID;
	}

	Tree getContents() {
		return allContents;
	}

	void addChild(String branchID, String commitID) {
		childBranchCommits.put(branchID, commitID);
	}

	void initial() {
		commitDate = 0;
		message = "initial commit";
		branchID = Config.MASTER_STRING;
		commitID = Config.INIT_COMMIT_ID;
		parentCommitID = "";
		mergeID = "";
		version = 0;
	}

	void persistence() {
		File file = Utils.join(Config.COMMIT_FOLDER, commitID);
		Utils.writeObject(file, this);
	}

	void persistenceStage() {
		Utils.writeContents(Config.CURRENT_COMMIT_FILE, commitID);
	}

	boolean tracked(String filename) {
		return allContents.getBlobs().containsKey(filename);
	}

	HashMap<String, String> minus(Commit otherCommit) {
		return allContents.minus(otherCommit.getContents());
	}

	Blob getBlobByName(String fileName) {
		return getBlobByID(getBlobIDByName(fileName));
	}

	Blob getBlobByID(String blobID) {
		File file = Utils.join(Config.BLOBS_FOLDER, blobID);
		if (!file.exists()) {
			return null;
		}
		return Utils.readObject(file, Blob.class);
	}

	String getBlobIDByName(String fullName) {
		return allContents.getBlobs().get(fullName);
	}

	Commit newNextCommit(String branchString) {
		Commit childCommit = new Commit();
		childCommit.setBranchID(branchString);
		childCommit.setParentCommitID(this.commitID());
		childCommit.setVersion(this.version + 1);
		childCommit.setCommitDate();
		childCommit.resetCommitID();

		addChild(branchString, childCommit.commitID());
		childCommit.allContents.dump(this.allContents);

		return childCommit;
	}

	void cloneBlobs(Commit other) {
		this.allContents.dump(other.allContents);
	}

	String getChildCommitID(String branchID) {
		return childBranchCommits.get(branchID);
	}

	boolean hasChildBranch(String branchID) {
		return childBranchCommits.containsKey(branchID);
	}

	boolean hasNextCommit() {
		return this.childBranchCommits.size() > 0;
	}

	Collection<String> getChildCommits() {
		return this.childBranchCommits.values();
	}

	void log() {
		Utils.message("===");
		Utils.message("commit %s", commitID());
		if (mergeID != null && mergeID.length() != 0) {
			Utils.message("Merge: %s %s", parentCommitID.substring(0, 7),
					mergeID.substring(0, 7));
		}
		String timestamp = new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy Z",
				Locale.ENGLISH).format(new Date(this.commitDate));
		Utils.message(timestamp.replace('+', '-'));
		Utils.message(message);
	}

	static boolean exists(String commitID) {
		File file = Utils.join(Config.COMMIT_FOLDER, commitID);
		return file.exists();
	}

	static Commit load(String commitID) {
		File file = Utils.join(Config.COMMIT_FOLDER, commitID);
		if (!file.exists()) {
			return null;
		}
		return Utils.readObject(file, Commit.class);
	}

	static Commit loadStage() {
		String commitID = Utils
				.readContentsAsString(Config.CURRENT_COMMIT_FILE);
		return load(commitID);
	}

	String commitID() {
		return commitID;
	}

	void addParent(Commit parentCommit) {
		this.parent = parentCommit;
		if (parentCommit.childs == null) {
			parentCommit.childs = new HashMap<>();
		}
		parentCommit.childs.put(this.branchID, this);
	}

	Commit getChild(String branchID) {
		return childs == null ? null : childs.get(branchID);
	}

	boolean checkChanged(String branchID, String fullName) {
		String blobID = getBlobIDByName(fullName);
		Commit aCommit = getChild(branchID);
		while (aCommit != null) {
			String chgID = aCommit.getBlobIDByName(fullName);
			if (blobID != chgID) {
				return true;
			}
			aCommit = aCommit.getChild(branchID);
		}
		return false;
	}

	static Commit getSplitCommit(String currID, String otherID) {
		return getSplitCommit(load(currID), load(otherID));
	}

	static Commit getSplitCommit(Commit currHead, Commit otherHead) {
		String currBranch = currHead.getBranchID();
		String otherBranch = otherHead.getBranchID();
		if (currBranch.equals(otherBranch)) {
			return null;
		}
		int aLen = 0, bLen = 0, cLen = 0, dLen = 0;
		Commit a = currHead;
		Commit b = null, bsplitCommit = null;
		Commit tmp;
		while (true) {
			if (a.hasChildBranch(otherBranch)
					|| a.commitID().equals(Config.INIT_COMMIT_ID)) {
				break;
			}
			if (b == null && !a.getMergeID().isEmpty()) {
				b = Commit.load(a.getMergeID());
				if (b.getBranchID().equals(otherBranch)) {
					a.addParent(b);
					bLen = aLen + 1;
					break;
				}
			}
			tmp = Commit.load(a.getParentCommitID());
			a.addParent(tmp);
			a = tmp;
			aLen++;
		}
		if (b != null) {
			bsplitCommit = b;
			while (true) {
				if (b.commitID().equals(otherHead.commitID())) {
					break;
				}
				tmp = Commit.load(b.getChildCommitID(otherBranch));
				tmp.addParent(b);
				b = tmp;
				bLen++;
			}
		}
		Commit c = otherHead;
		Commit d = null, dsplitCommit = null;
		while (true) {
			if (c.hasChildBranch(currBranch)
					|| c.commitID().equals(Config.INIT_COMMIT_ID)) {
				break;
			}
			if (d == null && !c.getMergeID().isEmpty()) {
				d = Commit.load(c.getMergeID());
				if (d.getBranchID().equals(currBranch)) {
					c.addParent(d);
					dLen = cLen + 1;
					break;
				}
			}
			tmp = Commit.load(c.getParentCommitID());
			c.addParent(tmp);
			c = tmp;
			cLen++;
		}
		if (d != null) {
			dsplitCommit = d;
			while (true) {
				if (d.commitID().equals(currHead.commitID())) {
					break;
				}
				tmp = Commit.load(d.getChildCommitID(currBranch));
				tmp.addParent(d);
				d = tmp;
				dLen++;
			}
		}
		if (b != null && d != null) {
			return bLen < dLen ? bsplitCommit : dsplitCommit;
		} else if (b != null || d != null) {
			return b != null ? bsplitCommit : dsplitCommit;
		} else if (a.commitID().equals(otherHead.commitID())) {
			return a;
		} else if (c.commitID().equals(currHead.commitID())) {
			return c;
		} else {
			c = c.getChild(otherBranch);
			c.addParent(a);
			return a;
		}
	}

	public String toString() {
		return String.format("%s-%s-%s-%d-%d", commitID, branchID,
				parentCommitID, version, commitDate);
	}

	void logDetail() {
		Utils.message("commit %s-%s-%d-%s", commitID, branchID, version,
				new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy Z",
						Locale.ENGLISH).format(new Date(commitDate)));
		Utils.message(">Message: %s", message);
		Utils.message(">ParentID: %s", parentCommitID);
		Utils.message(">MergeID: %s", mergeID);
		Utils.message(">Child branch:");
		for (java.util.Map.Entry<String, String> entry : childBranchCommits
				.entrySet()) {
			Utils.message("  %8s - %s", entry.getKey(), entry.getValue());
		}
		Utils.message(">All tracked files:");
		for (java.util.Map.Entry<String, String> entry : allContents.getBlobs()
				.entrySet()) {
			Blob blob = Blob.load(entry.getValue());
			Utils.message("  %s", blob.toString());
		}
	}

	private String message = "";
	private long commitDate = 0;
	// private String author;
	/** The parent CommmitID */
	private String parentCommitID = "";
	/** Merge from other CommitID. */
	private String mergeID = "";
	/** This commit is a branch point, Key->Branch, Value->Next CommitID. */
	private HashMap<String, String> childBranchCommits = new HashMap<>();
	/** The committing branchID */
	private String branchID = "";
	/** All blobs of this commit */
	private Tree allContents = new Tree();
	private String commitID = "";
	private int version;

	/** The follow attributes are used in merge */
	private transient Commit parent = null;
	private transient HashMap<String, Commit> childs;
}
