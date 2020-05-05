# Gitlet Design Document

**Name**:ChengXU

## Classes and Data Structures
###Blob
This class stores the contents of files, the name is SHA1 of the contents.
####Fields
*fullName  The relative path to work directory, e.g., "src/gitlet/Main.java".
*version   The blob version, e.g., "branchName_1".
*content   The work file content.
*blobID    Sha1 code
*operCmd   Operand, such as:
  *CMD_ADD = "A";
  *CMD_RM = "R";
  *CMD_MODIFY = "U";
  *CMD_MERGE_CONFLICT = "MC";
  *CMD_MERGE_ADD = "MA";
  *CMD_MERGE_RM = "MR";
  *CMD_NONE = "N"; 

###Commit
Combinations of log messages, other metadata (commit date, author, etc.), a reference to a tree, and references to parent commits. The repository also maintains a mapping from branch heads (in this course, we've used names like master, proj2, etc.) to references to commits, so that certain important commits have symbolic names.

####Fields
*commitID           The ID of this commit.
*branchID           The current branch when commiting.
*version            Each commiting, version plus one.
*message            The message of this commit.
*commitDate         The offset according to 00:00:00 UTC.
*parentCommitID     The parent commit ID.
*mergeID            The mergeID from other commit.
*childBranchCommits This commit is a split point, Key->Branch, Value->Next CommitID.
*allContents        All blobs of this commit.

The follow attributes are used in merge
*parent     Commit
*childs     HashMap<String, Commit>

###Tree
Directory structures mapping names to references to blobs and other trees (subdirectories). Here, the file of subdirectories use Blob.fullName to indicate, such as, "src/gitlet/Main.java".

####Fields
blobs       The only fields of this class, contains all blobs of the commit.

###Branch
####Fields
*branchID        The ID of this branch, "master" is the default branch.
*createDate      The offset according to 00:00:00 UTC.
*headCommit      The point of the head commit of this branch.
*status          The status of this branch, A-Active, D-Deactive.

## Algorithms
###blobID
Think about unique, use sha1(fullName, content) to code.

###The Split point
The Commit.mergeID field can fast locate the split point, the pseudo code:

    Commit getSplitCommit(Commit currHead, Commit otherHead) {
		if currBranchID equal otherBranchID return null
		set commit pointer A, B, C, D
		A point to currHead, C point to otherHead, B and D both null
		
		from A through to split point by A = A.getParentCommit, break point is A has a child otherBranchID branch, and count steps
		  if meet the mergeID is not empty, then B point this commit and break
		if B is not null, then from B through to otherHead, and count steps
		
		from C through to split point by C = C.getParentCommit, break point is C has a child currBranchID branch, and count steps
		  if meet the mergeID is not empty, then D point this commit and break
		if D is not null, then from D through to currHead, and count steps
		
		if B and D both not null, return the few steps ONE
		else if B and D only one not null, return the not null ONE
		else if A is the same with otherHead, then return it
		else if C is the same with currHead, then return it
		else return A
	}


## Persistence
###.gitlet/blobs/
All blobs are here.

###.gitlet/branch/
All branches are here, master is default, the store format such as:
master A 1588339886384 e1c91ad2246795674f4dabf9e5973aaf2425f5c1

###.gitlet/commits/
All commits are here, the initial commit ID is "00000000000000000000000000000000000", through it, we can find all commits one by one.

###.gitlet/stagearea/
The stage area. The additions, modifications, removals, merges are all here before commiting.
####.gitlet/stageare/current_commit
This file store the pointer of current commit.

###.gitlet/current_branch
Indicate the current branch.
