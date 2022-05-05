package gitlet;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Yuhan Dong
 */
public class Main {

    /**The path to the Current Working Directory.*/
    private static final File CWD = new File(".");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        switch (args[0]) {
        case "init":
            init();
            break;
        case "add":
            add(args);
            break;
        case "commit":
            validateDirectory();
            commit(args);
            break;
        case "log":
            log();
            break;
        case "global-log":
            globalLog();
            break;
        case "rm":
            rm(args);
            break;
        case "checkout":
            validateDirectory();
            checkout(args);
            break;
        case "find":
            validateDirectory();
            find(args);
            break;
        case "status":
            validateDirectory();
            status();
            break;
        case "branch":
            validateDirectory();
            branch(args);
            break;
        case "rm-branch":
            validateDirectory();
            rmBranch(args);
            break;
        case "reset":
            validateDirectory();
            reset(args);
            break;
        case "merge":
            validateDirectory();
            merge(args);
            break;
        default:
            System.out.println("No command with that name exists.");
            return;
        }
        return;
    }
    /** Initialize the gitlet system and creates directory
      * in the current directory. If gitlet exists, exit with
      * error message printed.
     */
    public static void init() {
        File gitlet = new File(".gitlet");
        if (gitlet.exists()) {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
            return;
        } else {
            new File(".gitlet").mkdirs();
            new File(".gitlet/blobs").mkdirs();
            new File(".gitlet/commits").mkdirs();
            new File(".gitlet/tree").mkdirs();
            new File(".gitlet/tree/reset").mkdirs();
            new File(".gitlet/staging").mkdirs();
            new File(".gitlet/staging/add").mkdirs();
            new File(".gitlet/staging/rm").mkdirs();
        }
        Tree initialTree = new Tree();
    }

    /**Adds a copy of the file to the staging folder,
     * If length < 2, exists with error message. If the
     * added file is identical to the current commit,
     * do not stage the file.
     * @param args The string input of the user.
     */
    public static void add(String[] args) {
        validateDirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        if (new File(args[1]).exists()) {
            File add = new File(".gitlet/staging/add/" + args[1]);
            File file = new File(CWD + "/" + args[1]);
            Utils.writeContents(add, Utils.readContentsAsString(file));
            Blobs blob = new Blobs(args[1], add);
            Tree treeNode = Utils.readObject(
                    new File(".gitlet/tree/treeNode"), Tree.class);
            if (new File(".gitlet/commits/"
                    + treeNode.getCommitId() + ".txt").exists()) {
                if (new File(".gitlet/staging/rm/" + args[1]).exists()) {
                    new File(".gitlet/staging/rm/" + args[1]).delete();
                }
                Commits headCommit = treeNode.getCommit();
                for (String fileName: headCommit.getBlob().keySet()) {
                    if (fileName.equals(args[1]) && blob.getId().equals(
                            headCommit.getBlob().get(fileName))) {
                        add.delete();
                    }
                }
            }
        } else {
            System.out.println("File does not exist.");
            return;
        }
    }

    /** Saves a snapshot of tracked files in the
     * current commit. The staging/add directory
     * will be cleared after a commit. Commit will
     * never remove, add, or change files in the working
     * directory. Head of the commit will become the
     * current commit right now.
     * @param args The commit message.
     */
    public static void commit(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        if (new File(".gitlet/staging/add").list().length == 0
                && new File(".gitlet/staging/rm").list().length == 0) {
            System.out.println("No changes added to the commit.");
            return;
        } else if (args[1].length() == 0) {
            System.out.println("Please enter a commit message.");
            return;
        }
        String message = args[1];
        String[] fileContained = new File(".gitlet/staging/add").list();
        String[] stagedRemoval = new File(".gitlet/staging/rm").list();
        Blobs[] blobs = new Blobs[fileContained.length];
        for (int i = 0; i < fileContained.length; i++) {
            File file = new File(".gitlet/staging/add/" + fileContained[i]);
            blobs[i] = new Blobs(fileContained[i], file);
            file.delete();
        }
        Tree treeNode = Utils.readObject(
                new File(".gitlet/tree/treeNode"), Tree.class);
        HashMap<String, String> parentAfterRM =
                new HashMap<String, String>();
        parentAfterRM.putAll(treeNode.getCommit().getBlob());
        for (int i = 0; i < stagedRemoval.length; i++) {
            parentAfterRM.remove(stagedRemoval[i]);
            File rm = new File(".gitlet/staging/rm/" + stagedRemoval[i]);
            rm.delete();
        }
        Commits head = new Commits(
                message, parentAfterRM, blobs);
        new Tree(treeNode.getBranchName(), treeNode, head.getCommitID());
    }

    /**Unstage the file if it is currently staged
     * in the add directory. Stage the file for removal
     * if the file is in the current commit and remove the file
     * from CWD.
     * @param args the input file name.
     */
    public static void rm(String[] args) {
        validateDirectory();
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        Tree treeNode = Utils.readObject(
                new File(".gitlet/tree/treeNode"), Tree.class);
        Commits currentCommit = treeNode.getCommit();
        File removal = new File(".gitlet/staging/add/" + args[1]);
        if (removal.exists()) {
            removal.delete();
        } else if (currentCommit.getBlob().containsKey(args[1])) {
            File stageRemoval = new File(".gitlet/staging/rm/" + args[1]);
            Utils.writeObject(stageRemoval,
                    currentCommit.getBlob().get(args[1]));
            new File(CWD + "/" + args[1]).delete();
        } else {
            System.out.println("No reason to remove the file.");
            return;
        }
    }

    /**If arg.length == 3, then checkout the file given and look
     * for it in the head commit and copy it to the current one.
     * If file does not exists, prints out an error message.
     * If args.length == 4, then checkout that version of the
     * committed file and replace the current one.
     * If args.length == 2, then checkout that branch with the
     * name given. If no branch with that name exists, prints out
     * an error message. If that branch is the current branch, prints
     * out an error message. If a working file is untracked in the
     * current working directory, prints out a message. If the directory
     * contains a file that is untracked in the branch commit, delete the
     * file and clear the staging area.
     * @param args Contains one of the 3 uses of checkout
     */
    public static void checkout(String[] args) {
        Tree treeNode = getTreeNode();
        if (args.length == 3 && args[1].equals("--")) {
            Commits head = treeNode.getCommit();
            HashMap<String, String> blobs = head.getBlob();
            if (blobs.containsKey(args[2])) {
                String id = blobs.get(args[2]);
                Blobs file = Utils.readObject(
                        new File(".gitlet/blobs/" + id + ".txt"), Blobs.class);
                Utils.writeContents(
                        new File(CWD + "/" + args[2]), file.getContent());
            } else {
                System.out.println("File does not exist in that commit.");
                return;
            }
        } else if (args.length == 4 && args[2].equals("--")) {
            if (readUID(args[1]) != null) {
                Commits commit = readUID(args[1]);
                HashMap<String, String> blobs = commit.getBlob();
                if (blobs.containsKey(args[3])) {
                    String id = blobs.get(args[3]);
                    Blobs file = Utils.readObject(
                            new File(".gitlet/blobs/" + id + ".txt"),
                            Blobs.class);
                    Utils.writeContents(
                            new File(CWD + "/" + args[3]), file.getContent());
                } else {
                    System.out.println("File does not exist in that commit.");
                    return;
                }
            } else {
                System.out.println("No commit with that id exists.");
                return;
            }
        } else if (args.length == 2) {
            if (!new File(".gitlet/tree/" + args[1]).exists()) {
                System.out.println("No such branch exists.");
                return;
            } else if (treeNode.getBranchName().equals(args[1])) {
                System.out.println("No need to checkout the current branch.");
                return;
            } else if (untrackedFiles(treeNode, Utils.readObject(new
                    File(".gitlet/tree/" + args[1]), Tree.class)).size() != 0) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it, or add and commit it first.");
            } else {
                Tree branch = Utils.readObject(new File(".gitlet/tree/"
                        + args[1]), Tree.class);
                Commits commit = treeNode.getCommit();
                File[] currentDirectory = CWD.listFiles(Utils.PLAIN_FILES);
                HashMap<String, String> blobs = new HashMap<>();
                blobs.putAll(branch.getCommit().getBlob());
                checkoutFiles(currentDirectory, blobs, commit);
                new Tree(args[1], branch.getParent(), branch.getCommitId());
                clearStagingArea();
            }
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    /**Starting at the head commit and display
     * information about each commit backwards
     * until the initial commit. Ignore other commits
     * along the way.
     * It will show the commit ID, the timestamp,
     * and the commit message.
     */
    public static void log() {
        validateDirectory();
        Tree treeNode = Utils.readObject(
                new File(".gitlet/tree/treeNode"), Tree.class);
        while (treeNode != null) {
            Commits head = treeNode.getCommit();
            System.out.println("===");
            System.out.println("commit " + head.getCommitID());
            System.out.println("Date: " + head.getTimeStamp());
            System.out.println(head.getMessage() + "\n");
            treeNode = treeNode.getParent();
        }
    }

    /**Similar function to log, but displays all
     * commits every made. The order does not matter.
     */
    public static void globalLog() {
        validateDirectory();
        File[] allCommits = new File(".gitlet/commits").listFiles();
        for (int i = 0; i < allCommits.length; i++) {
            Commits commit = Utils.readObject(allCommits[i], Commits.class);
            System.out.println("===");
            System.out.println("commit " + commit.getCommitID());
            System.out.println("Date: " + commit.getTimeStamp());
            System.out.println(commit.getMessage() + "\n");
        }
    }

    /**Finds all the commits with the given message.
     * If there are more than 1, print on a different line.
     * @param args The input commit message
     */
    public static void find(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        File[] allCommits = new File(".gitlet/commits").listFiles();
        int count = 0;
        for (int i = 0; i < allCommits.length; i++) {
            Commits commit = Utils.readObject(allCommits[i], Commits.class);
            if (commit.getMessage().equals(args[1])) {
                System.out.println(commit.getCommitID());
                count++;
            }
        }
        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**Displays what branches currently exist.
     * Display the staged Files and the removed
     * files. Modifications not staged for commit is
     * for files who are in the working directory that
     * is also in the commit, but the content is different.
     * The untracked files is the ones that is in the
     * working directory but not staged for add.
     */
    public static void status() {
        System.out.println("=== Branches ===");
        Tree treeNode = getTreeNode();
        for (String name : Utils.plainFilenamesIn(
                new File(".gitlet/tree"))) {
            if (name.equals(treeNode.getBranchName())) {
                System.out.println("*" + name);
            } else if (name.equals("treeNode")) {
                continue;
            } else {
                System.out.println(name);
            }
        }
        System.out.println("\n=== Staged Files ===");
        List<String> fileContained =
                Utils.plainFilenamesIn(new File(".gitlet/staging/add"));
        for (String name: fileContained) {
            System.out.println(name);
        }
        System.out.println("\n=== Removed Files ===");
        List<String> stagedRemoval =
                Utils.plainFilenamesIn(new File(".gitlet/staging/rm"));
        for (String removal: stagedRemoval) {
            System.out.println(removal);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        Commits currentCommit = treeNode.getCommit();
        System.out.println("\n=== Untracked Files ===");
        System.out.println();
    }

    /**Creates a new branch with the name given,
     * points to the current head commit. When creating
     * a new branch, the head branch does not immediately
     * switch to the new branch, but stays until checkout.
     * @param args The input branch name.
     */
    public static void branch(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        } else if (Utils.plainFilenamesIn(
                new File(".gitlet/tree")).contains(args[1])) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        Tree treeNode = getTreeNode();
        treeNode.createBranch(args[1]);
    }

    /**Deletes the branch with the given name.
     * Delete the pointer associated with the branch,
     * but not delete all commits that were created under
     * the branch.
     * @param args The input branch name.
     * */
    public static void rmBranch(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        Tree treeNode = getTreeNode();
        if (!new File(".gitlet/tree/" + args[1]).exists()) {
            System.out.println("A branch with that name does not exist.");
        } else if (treeNode.getBranchName().equals(args[1])) {
            System.out.println("Cannot remove the current branch.");
        } else {
            treeNode.getBranches().remove(args[1]);
            new File(".gitlet/tree/" + args[1]).delete();
        }
    }

    /**Checks out all the files with the given commit
     * ID. Removed the tracked files that are not present
     * in the commit. Moves the current branch's head to this
     * commit node. The staging area is cleared.
     * @param args the reset commit ID.
     * */
    public static void reset(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        Commits commit = readUID(args[1]);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Tree treeNode = getTreeNode();
        if (untrackedFiles(treeNode, commit)) {
            System.out.println("There is an untracked file "
                    + "in the way; delete it, or add and commit it first.");
            return;
        }
        HashMap<String, String> blobs = new HashMap<>();
        File[] currentDirectory = CWD.listFiles(Utils.PLAIN_FILES);
        blobs.putAll(commit.getBlob());
        checkoutFiles(currentDirectory, blobs, treeNode.getCommit());
        File reset = new File(".gitlet/tree/reset/" + treeNode.getCommitId());
        Utils.writeObject(reset, treeNode);
        outerloop:
        if (new File(".gitlet/tree/reset/" + commit.getCommitID()).exists()) {
            treeNode = Utils.readObject(new File(".gitlet/tree/reset/"
                            + commit.getCommitID()), Tree.class);
        } else {
            while (treeNode != null) {
                if (treeNode.getCommitId().equals(commit.getCommitID())) {
                    break outerloop;
                }
                treeNode = treeNode.getParent();
            }
        }
        new Tree(treeNode.getBranchName(), treeNode.getParent(), args[1]);
        clearStagingArea();
    }

    /**Merge the given branch into the current branch.
     *
     * @param args the given branch name.
     */
    public static void merge(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            return;
        }
        Tree treeNode = getTreeNode();
        if (new File(".gitlet/staging/add/").list().length != 0
                || new File(".gitlet/staging/rm").list().length != 0) {
            System.out.println("You have uncommitted changes.");
            return;
        } else if (!new File(".gitlet/tree/" + args[1]).exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (args[1].equals(treeNode.getBranchName())) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        } else if (untrackedFiles(treeNode, Utils.readObject(new File(
                ".gitlet/tree/" + args[1]), Tree.class)).size() != 0) {
            System.out.println("There is an untracked file "
                    + "in the way; delete it, or add and commit it first.");
            return;
        }
        Commits treeNodeCommit = treeNode.getCommit();
        Tree given = Utils.readObject(new File(".gitlet/tree/" + args[1]),
                Tree.class);
        Commits givenCommit = given.getCommit();
        String splitPoint = findSplitPoint(treeNode, given);
        if (splitPoint.equals(given.getCommitId())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        } else if (splitPoint.equals(treeNode.getCommitId())) {
            checkout(new String[]{"branch", args[1]});
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        Commits splitCommit = Utils.readObject(
                new File(".gitlet/commits/"
                        + splitPoint + ".txt"), Commits.class);
        boolean conflict =
                conflictMerge(splitCommit, givenCommit, treeNodeCommit);
        boolean sconflict = twoMerge(givenCommit, treeNodeCommit, splitCommit);
        mergeCommit(args[1], treeNode.getBranchName());
        if (conflict || sconflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /**Helper function for merge that compares
     * the three commits.
     * @param given given commits.
     * @param treeNode head commits.
     * @param split parent commits.
     * @return if there is a conflict.
     */
    public static boolean twoMerge(Commits given,
                                   Commits treeNode, Commits split) {
        HashMap<String, String> splitBlob = split.getBlob();
        HashMap<String, String> givenBlob = given.getBlob();
        HashMap<String, String> treeNodeBlob = treeNode.getBlob();
        boolean isConflict = false;
        for (String name: givenBlob.keySet()) {
            if (!splitBlob.containsKey(name)
                    && !treeNodeBlob.containsKey(name)) {
                String id = givenBlob.get(name);
                Blobs file = Utils.readObject(
                        new File(".gitlet/blobs/" + id + ".txt"), Blobs.class);
                Utils.writeContents(
                        new File(CWD + "/" + name), file.getContent());
                Utils.writeContents(new File(".gitlet/staging/add/" + name),
                        file.getContent());
            } else if (!splitBlob.containsKey(name)
                    && treeNodeBlob.containsKey(name)) {
                if (!givenBlob.get(name).equals(treeNodeBlob.get(name))) {
                    isConflict = true;
                    Blobs givenFile = Utils.readObject(new File(".gitlet/blobs/"
                            + givenBlob.get(name) + ".txt"), Blobs.class);
                    Blobs treeFile = Utils.readObject(new File(".gitlet/blobs/"
                            + treeNodeBlob.get(name) + ".txt"), Blobs.class);
                    Utils.writeContents(new File(CWD + "/" + name),
                            "<<<<<<< HEAD\n"
                            + treeFile.getContent() + "=======\n"
                                    + givenFile.getContent() + ">>>>>>>\n");
                    Utils.writeContents(new File(".gitlet/staging/add/" + name),
                            Utils.readContentsAsString(
                                    new File(CWD + "/" + name)));
                }
            }

        }
        return isConflict;
    }

    /** Add all nodes to the map.
     * @param map The map to be added.
     * @param node The head node
     * @param steps the step to the head node
     */
    public static void addToMap(HashMap<String, Integer> map,
                                Tree node, int steps) {
        if (node == null) {
            return;
        }
        String id = node.getCommitId();
        if (map.containsKey(id)) {
            if (steps < map.get(id)) {
                map.put(id, steps);
            }
        } else {
            map.put(id, steps);
        }
        if (node.getMergeParent() != null) {
            addToMap(map, node.getMergeParent(), steps + 1);
            addToMap(map, node.getParent(), steps + 1);
        } else {
            addToMap(map, node.getParent(), steps + 1);
        }
    }

    /** Add all nodes to arraylist.
     * @param list The list to be added.
     * @param node The tree node
     */
    public static void addToList(List<String> list, Tree node) {
        if (node == null) {
            return;
        }
        String id = node.getCommitId();
        list.add(id);
        if (node.getMergeParent() != null) {
            addToList(list, node.getMergeParent());
            addToList(list, node.getParent());
        } else {
            addToList(list, node.getParent());
        }
    }

    /**Find the split node in the branches.
     * @param treeNode the current treeNode.
     * @param given the given Branch.
     * @return the split node.
     */
    public static String findSplitPoint(Tree treeNode, Tree given) {
        HashMap<String, Integer> headtree = new HashMap<>();
        ArrayList<String> givenCommitId = new ArrayList<>();
        addToMap(headtree, treeNode, 0);
        addToList(givenCommitId, given);
        int min = 5;
        String id = "";
        for (String givenId: givenCommitId) {
            if (headtree.containsKey(givenId)) {
                if (min > headtree.get(givenId)) {
                    min = headtree.get(givenId);
                    id = givenId;
                }
            }
        }
        return id;
    }

    /**The helper function for merge. Go through
     * each circumstances given using the three
     * commits.
     * @param split The commits of the split Node.
     * @param given The given commits.
     * @param treeNode The current head treeNode.
     * @return True if conflict.
     */
    public static boolean conflictMerge(Commits split,
                                        Commits given, Commits treeNode) {
        boolean conflict = false;
        HashMap<String, String> splitBlob = split.getBlob();
        HashMap<String, String> givenBlob = given.getBlob();
        HashMap<String, String> treeNodeBlob = treeNode.getBlob();
        for (String name: splitBlob.keySet()) {
            if (givenBlob.containsKey(name) && treeNodeBlob.containsKey(name)) {
                Blobs givenFile = Utils.readObject(new File(".gitlet/blobs/"
                        + givenBlob.get(name) + ".txt"), Blobs.class);
                Blobs treeFile = Utils.readObject(new File(".gitlet/blobs/"
                        + treeNodeBlob.get(name) + ".txt"), Blobs.class);
                if (splitBlob.get(name).equals(treeNodeBlob.get(name))
                        && !splitBlob.get(name).equals(givenBlob.get(name))) {
                    Utils.writeContents(new File(CWD + "/" + name),
                            givenFile.getContent());
                    Utils.writeContents(new File(".gitlet/staging/add/" + name),
                            givenFile.getContent());
                } else if (!splitBlob.get(name).equals(treeNodeBlob.get(name))
                        && !splitBlob.get(name).equals(givenBlob.get(name))
                        && !givenBlob.get(name).equals(
                                treeNodeBlob.get(name))) {
                    conflict = true;
                    Utils.writeContents(new File(CWD + "/" + name),
                            "<<<<<<< HEAD\n" + treeFile.getContent() + "======="
                                     + "\n" + givenFile.getContent()
                                    + ">>>>>>>\n");
                    Utils.writeContents(new File(".gitlet/staging/add/" + name),
                            Utils.readContentsAsString(new File(CWD + "/"
                                    + name)));
                }
            } else if (givenBlob.containsKey(name)) {
                if (!splitBlob.get(name).equals(givenBlob.get(name))) {
                    conflict = true;
                    Blobs givenFile = Utils.readObject(new File(".gitlet/blobs/"
                            + givenBlob.get(name) + ".txt"), Blobs.class);
                    Utils.writeContents(new File(CWD + "/" + name),
                            "<<<<<<< HEAD\n=======\n"
                                    + givenFile.getContent() + ">>>>>>>\n");
                    Utils.writeContents(new File(".gitlet/staging/add/" + name),
                            Utils.readContentsAsString(new File(CWD + "/"
                                    + name)));
                }
            } else if (treeNodeBlob.containsKey(name)) {
                if (splitBlob.get(name).equals(treeNodeBlob.get(name))) {
                    new File(CWD + "/" + name).delete();
                } else {
                    conflict = true;
                    Blobs treeFile = Utils.readObject(new File(".gitlet/blobs/"
                            + treeNodeBlob.get(name) + ".txt"), Blobs.class);
                    Utils.writeContents(new File(CWD + "/" + name),
                            "<<<<<<< HEAD\n"
                            + treeFile.getContent() + "=======\n>>>>>>>\n");
                    write(new File(".gitlet/staging/add/" + name), Utils.
                            readContentsAsString(new File(CWD + "/" + name)));
                }
            }
        }
        return conflict;
    }

    /**
     * Write the contents into the file.
     * @param file given file
     * @param content given content
     */
    public static void write(File file, String content) {
        Utils.writeContents(file, content);
    }
    /**Commit the branches after merging
     * the two branches together.
     * @param given given branch name.
     * @param current current branch name.
     */
    public static void mergeCommit(String given, String current) {
        String[] fileContained = new File(".gitlet/staging/add").list();
        String[] stagedRemoval = new File(".gitlet/staging/rm").list();
        Blobs[] blobs = new Blobs[fileContained.length];
        for (int i = 0; i < fileContained.length; i++) {
            File file = new File(".gitlet/staging/add/" + fileContained[i]);
            blobs[i] = new Blobs(fileContained[i], file);
            file.delete();
        }
        Tree treeNode = Utils.readObject(
                new File(".gitlet/tree/treeNode"), Tree.class);
        HashMap<String, String> parentAfterRM =
                new HashMap<String, String>();
        parentAfterRM.putAll(treeNode.getCommit().getBlob());
        for (int i = 0; i < stagedRemoval.length; i++) {
            parentAfterRM.remove(stagedRemoval[i]);
            File rm = new File(".gitlet/staging/rm/" + stagedRemoval[i]);
            rm.delete();
        }
        Commits head = new Commits("Merged "
                + given + " into " + current + ".", parentAfterRM, blobs);
        new Tree(getTreeNode(),
                Utils.readObject(new File(".gitlet/tree/" + given),
                Tree.class), head.getCommitID());
    }

    /**Validate if the current directory contains
     * a .gitlet folder. If not, print the error
     * message.
     */
    public static void validateDirectory() {
        if (!new File(".gitlet").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    /**Give the treeNode where the head points to.
     * @return the treeNode.*/
    public static Tree getTreeNode() {
        return Utils.readObject(
                new File(".gitlet/tree/treeNode"), Tree.class);
    }

    /**Helper method used to track the untracked
     * files in the checkout branch.
     * @param treeNode the current branch.
     * @param checkout the checkout branch.
     * @return the list of untracked files.
     */
    public static List<String> untrackedFiles(Tree treeNode, Tree checkout) {
        List<String> untracked = Utils.plainFilenamesIn(CWD);
        List<String> copy = new ArrayList<>();
        copy.addAll(untracked);
        for (String name: untracked) {
            if (treeNode.getCommit().getBlob().containsKey(name)) {
                copy.remove(name);
            } else if (Utils.plainFilenamesIn(
                    new File(".gitlet/staging/add")).contains(name)) {
                copy.remove(name);
            } else if (Utils.plainFilenamesIn(
                    new File(".gitlet/staging/rm")).contains(name)) {
                copy.remove(name);
            } else if (!checkout.getCommit().getBlob().containsKey(name)) {
                copy.remove(name);
            }
        }
        return copy;
    }

    /**Helper method used to track the untracked
     * files in the checkout branch.
     * @param treeNode the current branch.
     * @param checkout the checkout commit.
     * @return the list of untracked files.
     */
    public static boolean untrackedFiles(Tree treeNode, Commits checkout) {
        List<String> untracked = Utils.plainFilenamesIn(CWD);
        for (String name: untracked) {
            if (!treeNode.getCommit().getBlob().containsKey(name)
                && checkout.getBlob().containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    /**Give the list of tracked files by the given tree.
     * @param treeNode The treenode given to find tracked Files.
     * @return  the list of tracked Files.*/
    public static List<String> trackedFiles(Tree treeNode) {
        List<String> tracked = Utils.plainFilenamesIn(CWD);
        for (String name: tracked) {
            if (!treeNode.getCommit().getBlob().containsKey(name)
                    && !Utils.plainFilenamesIn(
                            new File(".gitlet/staging/add")).contains(name)
                    && !Utils.plainFilenamesIn(
                        new File(".gitlet/staging/rm")).contains(name)) {
                tracked.remove(name);
            }
        }
        return tracked;
    }

    /**Clear the staging area.*/
    public static void clearStagingArea() {
        String[] fileContained = new File(".gitlet/staging/add/").list();
        String[] stagedRemoval = new File(".gitlet/staging/rm").list();
        for (int i = 0; i < fileContained.length; i++) {
            new File(".gitlet/staging/add/" + fileContained[i]).delete();
        }
        for (int i = 0; i < stagedRemoval.length; i++) {
            new File(".gitlet/staging/rm/" + stagedRemoval[i]).delete();
        }
    }

    /**Helper function for checkout all files in the
     * given commit.
     * @param blobs The hashmap of blobs.
     * @param commit the current commit.
     * @param currentDirectory The file directory with files array.
     */
    public static void checkoutFiles(File[] currentDirectory,
                                     HashMap<String, String> blobs,
                                     Commits commit) {
        for (File change: currentDirectory) {
            if (blobs.containsKey(change.getName())) {
                String id = blobs.get(change.getName());
                Blobs file = Utils.readObject(
                        new File(".gitlet/blobs/" + id + ".txt"), Blobs.class);
                Utils.writeContents(change, file.getContent());
                blobs.remove(change.getName());
            } else if (commit.getBlob().containsKey(change.getName())) {
                change.delete();
            }
        }
        for (String blob: blobs.keySet()) {
            String id = blobs.get(blob);
            Blobs file = Utils.readObject(
                    new File(".gitlet/blobs/" + id + ".txt"), Blobs.class);
            Utils.writeContents(new File(CWD + "/" + blob),
                    file.getContent());
        }
    }

    /**
     * Find the commits using a shorter ID.
     * @param arg the substring of commit ID.
     * @return the commits found
     */
    public static Commits readUID(String arg) {
        List<String> commits = Utils.plainFilenamesIn(
                new File(".gitlet/commits"));
        for (String id: commits) {
            if (arg.substring(0, 6).equals(id.substring(0, 6))) {
                return Utils.readObject(
                        new File(".gitlet/commits/" + id), Commits.class);
            }
        }
        return null;
    }
}
