package gitlet;

import java.io.File;
import java.util.Date;
import java.util.Scanner;

class Branch {
	static final String BRANCH_ACTIVE = "A";
	static final String BRANCH_DEACTIVE = "D";

	Branch() {
	}

	Branch(String branch, String headCommitID) {
		branchID = branch;
		headCommit = headCommitID;
		createDate = new Date().getTime();
		status = BRANCH_ACTIVE;
	}

	void persistenceHead(String commit) {
		headCommit = commit;
		persistence();
	}

	String getHead() {
		return headCommit;
	}

	String getBranchID() {
		return branchID;
	}

	void setBranchID(String branchID) {
		this.branchID = branchID;
	}

	void finish() {
		status = BRANCH_DEACTIVE;
	}
	
	boolean isRemoved() {
		return status.equals(BRANCH_DEACTIVE);
	}

	public String toString() {
		return String.format("%s %s %d %s", branchID, status, createDate,
				headCommit);
	}

	public void fromString(String content) {
		Scanner scanner = new Scanner(content);
		branchID = scanner.next();
		status = scanner.next();
		createDate = scanner.nextLong();
		headCommit = scanner.next();
		scanner.close();
	}

	static String getCurrentBranchID() {
		return Utils.readContentsAsString(Config.CURRENT_BRANCH_FILE);
	}

	static Branch loadCurrent() {
		String brachName = Utils
				.readContentsAsString(Config.CURRENT_BRANCH_FILE);
		return load(brachName);
	}

	static Branch load(String branchID) {
		File file = Utils.join(Config.BRANCH_FOLDER, branchID);
		if (!file.exists()) {
			return null;
		}
		Branch branch = new Branch();
		branch.fromString(Utils.readContentsAsString(file));
		return branch;
	}

	static boolean exists(String branchID) {
		File file = Utils.join(Config.BRANCH_FOLDER, branchID);
		if (file.exists()) {
			if (Branch.load(branchID).status.equals(BRANCH_ACTIVE)) {
				return true;
			}
		}
		return false;
	}

	void persistence() {
		File file = Utils.join(Config.BRANCH_FOLDER, getBranchID());
		Utils.writeContents(file, this.toString());
	}

	static void setCurrent(String branchID) {
		Utils.writeContents(Config.CURRENT_BRANCH_FILE, branchID);
	}

	private String status = BRANCH_ACTIVE;
	private String branchID = "";
	private long createDate;
	private String headCommit;
}
