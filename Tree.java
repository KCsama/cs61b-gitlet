package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;


/**
* Directory structures mapping names to reference to blobs and
* other trees (subdirectories)(CS61B guidelines).
* @author Yuhan Dong
 */
public class Tree implements Serializable {

    /** The branch name.*/
    private String branchName;

    /** The parent tree.*/
    private Tree parent;

    /** Save the different branches.
     * String is the branch name and
     * Tree is the object.*/
    private HashMap<String, Tree> branches = new HashMap<>();

    /** The commitID of the head of the tree.*/
    private String commitId;

    /** The parent created by merge.*/
    private Tree mergeParent;

    /** The initial tree is consists of the branch
     * name master. It does not have split. No files,
     * just to initialize and create directories.
     */
    Tree() {
        branchName = "master";
        commitId = new Commits().getCommitID();
        parent = null;
        mergeParent = null;
        branches.put(branchName, this);
        File trees = new File(".gitlet/tree/treeNode");
        Utils.writeObject(trees, this);
        File master = new File(".gitlet/tree/master");
        Utils.writeObject(master, this);
    }

    /** Create a tree with all the information given
     * in the parameters. Create this tree and save it
     * to the directory.
     * @param inputbranchName The input branch name.
     * @param parentTree the parent tree
     * @param commitIds the commit ID of the head commit.
     */
    Tree(String inputbranchName, Tree parentTree,
         String commitIds) {
        this.branchName = inputbranchName;
        this.parent = parentTree;
        mergeParent = null;
        this.commitId = commitIds;
        if (this.parent != null) {
            branches.putAll(parentTree.getBranches());
        }
        branches.put(inputbranchName, this);
        File trees = new File(".gitlet/tree/treeNode");
        Utils.writeObject(trees, this);
        File branch = new File(".gitlet/tree/"
                + inputbranchName);
        Utils.writeObject(branch, this);
    }

    /**The constructor for creating just one more branch.
     * Does not switch the treeNode to itself.
     * @param inputbranchName The input branchName
     */
    Tree(String inputbranchName) {
        this.branchName = inputbranchName;
        Tree treeNode = Utils.readObject(
                new File(".gitlet/tree/treeNode"), Tree.class);
        this.parent = treeNode.getParent();
        mergeParent = null;
        this.commitId = treeNode.getCommitId();
        if (this.parent != null) {
            branches.putAll(parent.getBranches());
        }
        branches.put(inputbranchName, this);
        File branch = new File(".gitlet/tree/"
                + inputbranchName);
        Utils.writeObject(branch, this);
    }

    /**The new constructor just for merge.
     * Contains a mergeParent which is the
     * given branch.
     * @param treenode the current branch.
     * @param given the given branch.
     * @param mergecommitId the commit ID of newly commit created.
     */
    Tree(Tree treenode, Tree given, String mergecommitId) {
        this.mergeParent = given;
        this.branchName = treenode.branchName;
        this.parent = treenode;
        this.commitId = mergecommitId;
        if (this.parent != null) {
            branches.putAll(treenode.getBranches());
        }
        branches.put(treenode.getBranchName(), this);
        File trees = new File(".gitlet/tree/treeNode");
        Utils.writeObject(trees, this);
        File branch = new File(".gitlet/tree/"
                + treenode.getBranchName());
        Utils.writeObject(branch, this);
    }

    /** Return the branch name.*/
    public String getBranchName() {
        return branchName;
    }

    /** Return the parent tree.*/
    public Tree getParent() {
        return parent;
    }

    /** Return the branches.*/
    public HashMap<String, Tree> getBranches() {
        return branches;
    }

    /** Return the commit ID of the head.*/
    public String getCommitId() {
        return commitId;
    }

    /** Return the head commit.*/
    public Commits getCommit() {
        return Utils.readObject(
                new File(".gitlet/commits/" + getCommitId() + ".txt"),
                Commits.class);
    }

    /** Return the mergeParent. */
    public Tree getMergeParent() {
        return mergeParent;
    }
    /** Create a new branch and save it inside the hashmap.
     * @param newbranchName the new branch name.*/
    public void createBranch(String newbranchName) {
        new Tree(newbranchName);
    }

}
