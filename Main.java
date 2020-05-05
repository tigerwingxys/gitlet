package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 * 
 * @author
 */
public class Main {

	/**
	 * Usage: java gitlet.Main ARGS, where ARGS contains <COMMAND> <OPERAND>
	 * ....
	 */
	public static void main(String... args) {
		if (args.length < 1) {
			usage();
			return;
		}

		try {
			switch (args[0].toLowerCase()) {
			case "init":
				init();
				break;
			case "add":
				checkInitialized();
				add(args);
				break;
			case "commit":
				checkInitialized();
				commit(args);
				break;
			case "rm":
				checkInitialized();
				rm(args);
				break;
			case "log":
				checkInitialized();
				log();
				break;
			case "log-detail":
				checkInitialized();
				logDetail();
				break;
			case "global-log":
				checkInitialized();
				globalLog();
				break;
			case "find":
				checkInitialized();
				find(args);
				break;
			case "status":
				checkInitialized();
				status();
				break;
			case "checkout":
				checkInitialized();
				checkout(args);
				break;
			case "branch":
				checkInitialized();
				branch(args);
				break;
			case "rm-branch":
				checkInitialized();
				rmBranch(args);
				break;
			case "reset":
				checkInitialized();
				reset(args);
				break;
			case "merge":
				checkInitialized();
				merge(args);
				break;
			case "dump-commit":
				checkInitialized();
				dumpCommit(args);
				break;
			case "dump-current-commit":
				checkInitialized();
				Commit.loadStage().logDetail();
				break;
			case "global-log-detail":
				checkInitialized();
				globalLogDetail();
				break;
			case "dump-blob":
				checkInitialized();
				dumpBlob(args);
				break;
			default:
				Utils.message("No command with that name exists.");
				break;
			}
		} catch (GitletException e) {
			Utils.message(e.getMessage());
		}
	}

	static void dumpCommit(String... args) {
		if (args.length < 2) {
			return;
		}
		Commit.load(args[1]).logDetail();
	}

	static void dumpBlob(String... args) {
		if (args.length < 2) {
			return;
		}
		Utils.message(Blob.load(args[1]).toString());
	}

	static void checkInitialized() {
		if (!Config.GITLET_FOLDER.exists()) {
			throw Utils.error("Not in an initialized Gitlet directory");
		}
	}

	static void init() {
		if (Config.GITLET_FOLDER.exists()) {
			Utils.message(
					"A Gitlet version-control system already exists in the current directory.");
			return;
		}
		Config.GITLET_FOLDER.mkdir();
		Config.BLOBS_FOLDER.mkdirs();
		Config.COMMIT_FOLDER.mkdirs();
		Config.BRANCH_FOLDER.mkdirs();
		Config.STAGEAREA_FOLDER.mkdirs();
		/* Initialize the first commit */
		Commit initCommit = new Commit();
		initCommit.initial();

		/* Create a new commit */
		Commit nextCommit = initCommit.newNextCommit(Config.MASTER_STRING);

		/* Persistence both commits */
		initCommit.persistence();
		nextCommit.persistence();
		nextCommit.persistenceStage();

		/* Initialize branch */
		Branch branch = new Branch(Config.MASTER_STRING, initCommit.commitID());
		branch.persistence();

		/* Set the current branch */
		Branch.setCurrent(Config.MASTER_STRING);

	}

	static void add(String... args) {
		if (args.length != 2) {
			Utils.message("Incorrect operands.");
			return;
		}
		String fullName = Blob.uniquePath(args[1]);
		File file = Utils.join(Config.CWD, fullName);
		if (!file.exists()) {
			Utils.message("File does not exist.");
			return;
		}
		String concatName = Blob.concatPath(fullName);
		File addFile = Utils.join(Config.STAGEAREA_FOLDER, concatName);
		if (addFile.exists()) {
			Blob b = Blob.loadStage(addFile);
			if (b.getOperCmd().equals(Config.CMD_RM)) {
				File f = Utils.join(Config.CWD, b.getFullName());
				Utils.writeContents(f, b.getContent());
				Utils.restrictedDelete(addFile);
				Utils.message("File not stage for removal.");
				return;
			} else {
				throw Utils.error("File already tracked.");
			}
		}
		Commit aCommit = Commit.loadStage();
		if (aCommit.tracked(fullName)) {
			Utils.message("File already tracked.");
			return;
		}
		Blob blob = new Blob();
		blob.stageAdd(fullName, Utils.readContentsAsString(file), aCommit.getVersion());
	}

	static void commit(String... args) {
		if (args.length > 2) {
			Utils.message("Incorrect operands.");
			return;
		}
		if (args.length != 2) {
			Utils.message("Please enter a commit message.");
			return;
		}
		Commit currentCommit = Commit.loadStage();
		Branch branch = Branch.loadCurrent();
		Commit headCommit = Commit.load(branch.getHead());
		if (!branch.getHead().equals(currentCommit.getParentCommitID())) {
			Utils.message(
					"Confict. Others has commited, please checkout the new version, and then commit again.");
			return;
		}

		prepare(currentCommit);

		currentCommit.setMessage(args[1]);
		currentCommit.setCommitDate();

		Commit nextCommit = currentCommit.newNextCommit(branch.getBranchID());
		nextCommit.setMessage("");
		currentCommit.persistence();
		clearStage();
		nextCommit.persistence();
		nextCommit.persistenceStage();

		branch.persistenceHead(currentCommit.commitID());

	}

	static boolean stageNotEmpty() {
		File[] files = Config.STAGEAREA_FOLDER.listFiles();
		return files.length > 1;
	}

	static void clearStage() {
		File[] files = Config.STAGEAREA_FOLDER.listFiles();
		for (File file : files) {
			if (file.getName().equals(Config.CURRENT_COMMIT_STRING)) {
				continue;
			}
			file.delete();
		}
	}

	static void checkModification(Commit aCommit) {
		HashMap<String, String> blobs = aCommit.getContents().getBlobs();
		for (java.util.Map.Entry<String, String> entry : blobs.entrySet()) {
			File file = Utils.join(Config.CWD, entry.getKey());
			if (!file.exists()) {
				continue;
			}
			String content = Utils.readContentsAsString(file);
			String newID = Utils.sha1(entry.getKey(), content);
			if (!newID.equals(entry.getValue())) {
				Blob blob = new Blob();
				blob.setVersion(aCommit.getVersion());
				blob.setOperCmd(Config.CMD_MODIFY);
				blob.setContent(content);
				blob.setBlobID(newID);
				blob.setFullName(entry.getKey());
				blob.persistenceStage();
			}
		}

	}

	static void prepare(Commit aCommit) {
		boolean changed = false;
		Tree aTree = aCommit.getContents();
		checkModification(aCommit);

		File[] files = Config.STAGEAREA_FOLDER.listFiles();
		if (files.length > 1) {
			changed = true;
		}
		for (File file : files) {
			if (file.getName().equals(Config.CURRENT_COMMIT_STRING)) {
				continue;
			}
			Blob blob = Blob.loadStage(file);
			switch (blob.getOperCmd()) {
			case Config.CMD_ADD:
			case Config.CMD_MODIFY:
				blob.persistence();
				aTree.addBlob(blob.getFullName(), blob.getBlobID());
				break;
			case Config.CMD_MERGE_CONFLICT:
			case Config.CMD_MERGE_ADD:
				blob.persistence();
				aTree.addBlob(blob.getFullName(), blob.getBlobID());
				blob.saveToWorkdir();
				break;
			case Config.CMD_MERGE_RM:
				aTree.removeBlob(blob.getFullName());
				blob.removeFromWorkDir();
				break;
			case Config.CMD_RM:
				aTree.removeBlob(blob.getFullName());
				break;

			default:
				break;
			}
		}
		if (!changed) {
			throw Utils.error("No changes added to the commit.");
		}
	}

	static void switchCommit(Commit currentCommit, Commit targetCommit) {
		for (String fullName : currentCommit.minus(targetCommit).keySet()) {
			File file = Utils.join(Config.CWD, fullName);
			if (file.exists()) {
				file.delete();
			}
		}
		for (java.util.Map.Entry<String, String> entry : targetCommit
				.getContents().getBlobs().entrySet()) {
			if (entry.getValue()
					.equals(currentCommit.getBlobIDByName(entry.getKey()))) {
				continue;
			}
			Utils.writeContents(Utils.join(Config.CWD, entry.getKey()),
					Blob.load(entry.getValue()).getContent());
		}
	}

	static void rm(String... args) {
		if (args.length < 2) {
			Utils.message("Please enter a filename.");
			return;
		}
		String fullName = Blob.uniquePath(args[1]);
		File file = Utils.join(Config.CWD, fullName);
		if (!file.exists()) {
			Utils.message("File does not exist.");
			return;
		}
		String concatName = Blob.concatPath(fullName);
		File rmFile = Utils.join(Config.STAGEAREA_FOLDER, concatName);
		if (rmFile.exists()) {
			Blob b = Blob.loadStage(rmFile);
			if (b.getOperCmd().equals(Config.CMD_ADD)) {
				rmFile.delete();
				Utils.message("File not stage for addition.");
				return;
			} else if (b.getOperCmd().equals(Config.CMD_RM)) {
				Utils.message("File already stage for removal.");
				return;
			}
		}
		Commit aCommit = Commit.loadStage();
		if (!aCommit.tracked(fullName)) {
			throw Utils.error("No reason to remove the file.");
		}
		Blob blob = aCommit.getBlobByName(fullName);
		blob.setOperCmd(Config.CMD_RM);
		blob.persistenceStage();

		file.delete();

		Utils.message("File stage for removal OK.");

	}

	static void log() {
		Commit currentCommit = Commit.loadStage();
		Commit aCommit = Commit.load(currentCommit.getParentCommitID());
		while (true) {
			aCommit.log();
			if (aCommit.getParentCommitID() == null
					|| aCommit.getParentCommitID().length() == 0) {
				break;
			}
			System.out.println();
			aCommit = Commit.load(aCommit.getParentCommitID());
		}

	}
	static void logDetail() {
		Commit currentCommit = Commit.loadStage();
		Commit aCommit = Commit.load(currentCommit.getParentCommitID());
		while (true) {
			aCommit.logDetail();
			if (aCommit.getParentCommitID() == null
					|| aCommit.getParentCommitID().length() == 0) {
				break;
			}
			System.out.println();
			aCommit = Commit.load(aCommit.getParentCommitID());
		}

	}

	static void globalLog() {
		Commit initCommit = Commit.load(Config.INIT_COMMIT_ID);
		printLog(initCommit);
	}

	static void printLog(Commit aCommit) {
		if (aCommit.hasNextCommit()) {
			if (!aCommit.commitID().equals(Config.INIT_COMMIT_ID)) {
				System.out.println();
			}
			aCommit.log();
			for (String commitID : aCommit.getChildCommits()) {
				printLog(Commit.load(commitID));
			}
		}
	}

	static void globalLogDetail() {
		Commit initCommit = Commit.load(Config.INIT_COMMIT_ID);
		printLogDetail(initCommit);
	}

	static void printLogDetail(Commit aCommit) {
		if (aCommit.hasNextCommit()) {
			if (!aCommit.commitID().equals(Config.INIT_COMMIT_ID)) {
				System.out.println();
			}
			aCommit.logDetail();
			for (String commitID : aCommit.getChildCommits()) {
				printLogDetail(Commit.load(commitID));
			}
		}
	}

	static void find(String... args) {
		if (args.length != 2) {
			Utils.message("Incorrect operands.");
			return;
		}
		Commit currentCommit = Commit.loadStage();
		Commit aCommit = Commit.load(currentCommit.getParentCommitID());
		while (true) {
			if (aCommit.getMessage().contains(args[1])) {
				Utils.message(aCommit.commitID());
			}
			if (aCommit.getParentCommitID() == null
					|| aCommit.getParentCommitID().length() == 0) {
				break;
			}
			aCommit = Commit.load(aCommit.getParentCommitID());
		}

	}

	static void status() {
		String[] branches = Config.BRANCH_FOLDER.list();
		String currentBranchID = Branch.getCurrentBranchID();
		Utils.message("=== Branches ===");
		for (String s : branches) {
			if (s.equals(currentBranchID)) {
				Utils.message("*%s", s);
			} else {
				Utils.message(s);
			}
		}

		ArrayList<String> stageFiles = new ArrayList<>();
		ArrayList<String> rmFiles = new ArrayList<>();
		ArrayList<String> notStageFiles = new ArrayList<>();
		ArrayList<String> untrackedFiles = new ArrayList<>();
		File[] files = Config.STAGEAREA_FOLDER.listFiles();
		for (File file : files) {
			if (file.getName().equals(Config.CURRENT_COMMIT_STRING)) {
				continue;
			}
			Blob blob = Blob.loadStage(file);
			if (blob.getOperCmd().equals(Config.CMD_RM)) {
				if (Utils.join(Config.CWD, blob.getFullName()).exists()) {
					untrackedFiles.add(blob.getFullName());
				} else {
					rmFiles.add(blob.getFullName());
				}
			} else {
				if (blob.getOperCmd().equals(Config.CMD_ADD) && !Utils
						.join(Config.CWD, blob.getFullName()).exists()) {
					notStageFiles.add(blob.getFullName() + " (deleted)");
				} else if (blob.getOperCmd().equals(Config.CMD_ADD)
						&& changed(blob.getFullName(), blob.getBlobID())) {
					notStageFiles.add(blob.getFullName() + " (modified)");
				} else {
					stageFiles.add(blob.getFullName());
				}
			}
		}

		Commit aCommit = Commit.loadStage();
		HashMap<String, String> blobs = aCommit.getContents().getBlobs();
		for (java.util.Map.Entry<String, String> entry : blobs.entrySet()) {
			File file = Utils.join(Config.CWD, entry.getKey());
			if (!file.exists()) {
				notStageFiles.add(entry.getKey() + " (deleted)");
			} else {
				String content = Utils.readContentsAsString(file);
				String newID = Utils.sha1(entry.getKey(), content);
				if (!newID.equals(entry.getValue())) {
					notStageFiles.add(entry.getKey() + " (modified)");
				}
			}
		}

		ArrayList<String> allFiles = new ArrayList<>();
		getAllFiles("", Config.CWD, allFiles);

		for (String string : allFiles) {
			if (!aCommit.tracked(string) && !Utils
					.join(Config.STAGEAREA_FOLDER, Blob.concatPath(string))
					.exists()) {
				untrackedFiles.add(string);
			}
		}

		System.out.println();
		Utils.message("=== Staged Files ===");
		for (String s : stageFiles) {
			Utils.message(s);
		}

		System.out.println();
		Utils.message("=== Removed Files ===");
		for (String s : rmFiles) {
			Utils.message(s);
		}

		System.out.println();
		Utils.message("=== Modifications Not Staged For Commit ===");
		for (String s : notStageFiles) {
			Utils.message(s);
		}

		System.out.println();
		Utils.message("=== Untracked Files ===");
		for (String s : untrackedFiles) {
			Utils.message(s);
		}
	}
	static void getAllFiles(String path, File dir, ArrayList<String> list) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.getName().equals(Config.GITLET_STRING)) {
				continue;
			}
			if (file.isFile()) {
				list.add(path.length() == 0 ? file.getName()
						: path + file.getName());
			} else {
				getAllFiles(path + file.getName() + "/", file, list);
			}
		}
	}

	static boolean changed(String fullName, String blobID) {
		return !blobID.equals(Utils.sha1(fullName,
				Utils.readContentsAsString(Utils.join(Config.CWD, fullName))));
	}

	static void checkout(String... args) {
		if (args.length < 2 || args.length > 4) {
			Utils.message("Incorrect operands.");
			return;
		}
		if (args.length == 2) {
			Branch targetBranch = Branch.load(args[1]);
			if (targetBranch == null || targetBranch.isRemoved()) {
				Utils.message("No such branch exists.");
				return;
			}
			if (Branch.getCurrentBranchID()
					.equals(targetBranch.getBranchID())) {
				Utils.message("No need to checkout the current branch.");
				return;

			}
			Commit currentCommit = Commit.loadStage();
			Commit targetCommit = Commit.load(targetBranch.getHead());
			for (String fullName : targetCommit.minus(currentCommit).keySet()) {
				File file = Utils.join(Config.CWD, fullName);
				if (file.exists()
						&& targetCommit.getBlobIDByName(fullName).equals(Utils
								.sha1(fullName, Utils.readContents(file)))) {
					Utils.message(
							"There is an untracked file in the way; delete it, or add and commit it first.");
					return;
				}
			}
			clearStage();
			switchCommit(currentCommit, targetCommit);
			Commit nextCommit = Commit.load(
					targetCommit.getChildCommitID(targetBranch.getBranchID()));
			nextCommit.persistenceStage();
			Branch.setCurrent(targetBranch.getBranchID());
		} else if (args.length == 3) {
			Branch branch = Branch.loadCurrent();
			Commit aCommit = Commit.load(branch.getHead());
			if (!aCommit.tracked(args[2])) {
				Utils.message("File does not exist in that commit.");
				return;
			}
			Blob blob = aCommit.getBlobByName(args[2]);
			blob.saveToWorkdir();
		} else {
			Commit aCommit = Commit.load(args[1]);
			if (aCommit == null) {
				Utils.message("No commit with that id exists.");
				return;
			}
			if (!aCommit.tracked(args[3])) {
				Utils.message("File does not exist in that commit.");
				return;
			}
			Blob blob = aCommit.getBlobByName(args[3]);
			blob.saveToWorkdir();
		}
	}

	static void branch(String... args) {
		if (args.length > 2) {
			Utils.message("Incorrect operands.");
			return;
		}
		if (args.length < 2) {
			Utils.message("Please enter a branch name.");
			return;
		}
		if (Branch.exists(args[1])) {
			Utils.message("A branch with that name already exists.");
			return;
		}
		Branch currBranch = Branch.loadCurrent();
		String newBranchID = args[1];
		Commit currentHead = Commit.load(currBranch.getHead());
		Commit bCommit = currentHead.newNextCommit(newBranchID);
		bCommit.setVersion(1);

		Branch newBranch = new Branch(newBranchID, currentHead.commitID());

		currentHead.persistence();
		bCommit.persistence();
		newBranch.persistence();
	}

	static void rmBranch(String... args) {
		if (args.length != 2) {
			Utils.message("Incorrect operands.");
			return;
		}
		if (args[1].equals(Config.MASTER_STRING)) {
			Utils.message("Cannot remove the [master] branch.");
			return;
		}
		if (!Branch.exists(args[1])) {
			Utils.message("A branch with that name does not exist.");
			return;
		}
		Branch branch = Branch.loadCurrent();
		if (branch.getBranchID().equals(args[1])) {
			Utils.message("Cannot remove the current branch.");
			return;
		}

		Branch rBranch = Branch.load(args[1]);
		rBranch.finish();
		rBranch.persistence();
	}

	static void reset(String... args) {
		if (args.length == 1) {
			clearStage();
			return;
		}
		String commitID =  args[1];
			if (!Commit.exists(commitID)) {
				Utils.message("No commit with that id exists.");
				return;
			}
			
		Commit currentCommit = Commit.loadStage();
		Commit targetCommit = Commit.load(commitID);

		for (String fullName : targetCommit.minus(currentCommit).keySet()) {
			File file = Utils.join(Config.CWD, fullName);
			if (file.exists()) {
				Utils.message(
						"There is an untracked file in the way; delete it, or add and commit it first.");
				return;
			}
		}
		Branch branch = Branch.loadCurrent();
		branch.persistenceHead(targetCommit.commitID());

		switchCommit(currentCommit, targetCommit);

		Commit nextCommit = targetCommit.newNextCommit(branch.getBranchID());

		targetCommit.persistence();
		clearStage();
		nextCommit.persistence();
		nextCommit.persistenceStage();
		

	}

	static void merge(String... args) {
		if (stageNotEmpty()) {
			Utils.message("You have uncommitted changes.");
			return;
		}
		if (args.length != 2) {
			Utils.message("Incorrect operands.");
			return;
		}
		Branch currentBranch = Branch.loadCurrent();
		Branch branch = Branch.load(args[1]);
		if (branch == null) {
			Utils.message("A branch with that name does not exist.");
			return;
		}
		String currBranchID = Branch.getCurrentBranchID();
		if (branch.getBranchID().equals(currBranchID)) {
			Utils.message(" Cannot merge a branch with itself.");
			return;
		}
		Commit currentCommit = Commit.loadStage();
		Commit mergeCommit = Commit.load(branch.getHead());
		for (String fullName : mergeCommit.minus(currentCommit).keySet()) {
			File file = Utils.join(Config.CWD, fullName);
			if (file.exists()) {
				Utils.message(
						"There is an untracked file in the way; delete it, or add and commit it first.");
				return;
			}
		}
		Commit currentHead = Commit.load(currentCommit.getParentCommitID());
		Commit splitCommit = Commit.getSplitCommit(currentHead, mergeCommit);
		/*
		 * If the split point is the same commit as the given branch, then we do
		 * nothing
		 */
		if (splitCommit.commitID().equals(branch.getHead())) {
			Utils.message("Given branch is an ancestor of the current branch.");
			return;
		}
		String mergeMessage = String.format("Merged [%s] into [%s].",
				branch.getBranchID(), currentCommit.getBranchID());
		/* If the split point is the current branch, fast forward. */
		if (splitCommit.commitID().equals(currentCommit.getParentCommitID())) {
			switchCommit(currentCommit, mergeCommit);
			currentCommit.cloneBlobs(mergeCommit);
			currentCommit.setMessage(mergeMessage);
			currentCommit.setCommitDate();
			currentCommit.setMergeID(mergeCommit.commitID());
			Commit aCommit = currentCommit.newNextCommit(currBranchID);

			currentCommit.persistence();
			aCommit.persistence();
			aCommit.persistenceStage();

			currentBranch.persistenceHead(currentCommit.commitID());

			Utils.message("Current branch fast-forwarded.");
			return;
		}
		for (java.util.Map.Entry<String, String> entry : mergeCommit
				.getContents().getBlobs().entrySet()) {
			String fullName = entry.getKey();
			String blobID = entry.getValue();
			if (blobID.equals(currentHead.getBlobIDByName(fullName))) {
				continue;
			}
			Blob mergeBlob = mergeCommit.getBlobByName(fullName);
			if (splitCommit.tracked(fullName)) {
				Blob currBlob = currentHead.getBlobByName(fullName);
				if (splitCommit.checkChanged(branch.getBranchID(), fullName)) {
					if (currBlob == null) {
						/* Conflict, current removed and merge changed */
						mergeBlob.stageMergeConflict("", mergeBlob.getContent(),
								currentCommit.getVersion());
					} else if (currBlob != null && splitCommit
							.checkChanged(currBranchID, fullName)) {
						/* Conflict, both changed */
						mergeBlob.stageMergeConflict(currBlob.getContent(),
								mergeBlob.getContent(),
								currentCommit.getVersion());
					} else {
						/* Add, current not changed and merge changed */
						mergeBlob.stageMergeAdd(currentCommit.getVersion());
					}
				}
			} else {
				if (currentHead.tracked(fullName)) {
					Blob currBlob = currentHead.getBlobByName(fullName);
					/* Conflict, absent in split point, both tracked */
					mergeBlob.stageMergeConflict(currBlob.getContent(),
							mergeBlob.getContent(), currentCommit.getVersion());

				} else {
					/*
					 * Add, absent in the split point, and present only in the
					 * given branch
					 */
					mergeBlob.stageMergeAdd(currentCommit.getVersion());
				}
			}

		}
		for (java.util.Map.Entry<String, String> entry : splitCommit
				.minus(mergeCommit).entrySet()) {
			if (currentHead.tracked(entry.getKey())) {
				Blob currBlob = currentHead.getBlobByName(entry.getValue());
				if (!splitCommit.checkChanged(currBranchID, entry.getKey())) {
					/*
					 * Remove, unmodified in the current, and absent in the
					 * given branch
					 */
					currBlob.stageMergeRemove();
				} else {
					/* Conflict, current changed and merge removed */
					currBlob.stageMergeConflict(currBlob.getContent(), "",
							currentCommit.getVersion());
				}
			}
		}
		prepare(currentCommit);

		currentCommit.setMessage(mergeMessage);
		currentCommit.setCommitDate();
		currentCommit.setMergeID(mergeCommit.commitID());

		Commit nextCommit = currentCommit.newNextCommit(currBranchID);
		nextCommit.setMessage("");
		currentCommit.persistence();
		clearStage();
		nextCommit.persistence();
		nextCommit.persistenceStage();

		currentBranch.persistenceHead(currentCommit.commitID());

	}

	/** Print brief description of the command-line format. */
	static void usage() {
		Utils.message(Config.HELP_TEXT);
	}

}
