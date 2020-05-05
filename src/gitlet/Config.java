package gitlet;

import java.io.File;
import java.io.Serializable;

import com.sun.tools.classfile.Dependency.Finder;

class Config {
	static final String CMD_ADD = "A";
	static final String CMD_RM = "R";
	static final String CMD_MODIFY = "U";
	static final String CMD_MERGE_CONFLICT = "MC";
	static final String CMD_MERGE_ADD = "MA";
	static final String CMD_MERGE_RM = "MR";
	static final String CMD_NONE = "N";

	static final String CONCAT_CHAR = "~";
	/** Version designator. */
	static final String VERSION = "1.0";
	/** Name of resource containing usage message. */
	static final String USAGE = "src/gitlet/Help.txt";
	/** Current Working Directory. */
	static final File CWD = new File(".");
	/** Main gitlet folder. */
	static final String GITLET_STRING = ".gitlet";
	static final File GITLET_FOLDER = new File(CWD, GITLET_STRING);
	static final String INIT_COMMIT_ID = "0000000000000000000000000000000000000000";
	static final String MASTER_STRING = "master";
	static final String CURRENT_COMMIT_STRING = "current_commit";
	static final String MERGE_STRING = "<<<<<<< HEAD\n%s=======\n%s>>>>>>>";
	/** Folders and files used in .gitlet. */
	static final File BLOBS_FOLDER = new File(GITLET_FOLDER, "blobs");
	static final File COMMIT_FOLDER = new File(GITLET_FOLDER, "commits");
	static final File BRANCH_FOLDER = new File(GITLET_FOLDER, "branchs");
	static final File STAGEAREA_FOLDER = new File(GITLET_FOLDER, "stagearea");
	static final File CURRENT_BRANCH_FILE = new File(GITLET_FOLDER,
			"current_branch");
	static final File CURRENT_COMMIT_FILE = new File(STAGEAREA_FOLDER,
			CURRENT_COMMIT_STRING);
	static final File CONFIG_FILE = new File(GITLET_FOLDER, "config");
	static final File LOG_FILE= new File(GITLET_FOLDER,"logs");
	
	static final String HELP_TEXT = "Usage: java gitlet.Main Command [args], supported commands as below:\r\n" + 
			"\r\n" + 
			"  init                    Creates a new Gitlet version-control system in the current directory.\r\n" + 
			"  add [file name]         Adds a copy of the file to staging area.\r\n" + 
			"  commit [message]        Saves a snapshot of files in the current commit.\r\n" + 
			"  rm [file name]          Remove a file from repository.\r\n" + 
			"  log                     Displays information about each commit until the initial commit.\r\n" + 
			"  global-log              Displays information about all commits.\r\n" + 
			"  find [commit message]   Prints out the ids of all commits that have the given commit message.\r\n" + 
			"  status                  Displays branches, staged files, removed files, etc.\r\n" + 
			"  checkout [args]         Checkout files, args has 3 use cases:\r\n" + 
			"           -- [file name]\r\n" + 
			"           [commit id] -- [file name]\r\n" + 
			"           [branch name]\r\n" + 
			"  branch [branch name]    Creates a new branch with the given name.\r\n" + 
			"  rm-branch [branch name] Deletes the branch with the given name. \r\n" + 
			"  reset [commit id]       Checks out all the files tracked by the given commit.\r\n" + 
			"  merge [branch name]     Merges files from the given branch into the current branch.\r\n" + 
			"  help or ?               This text.";

}
