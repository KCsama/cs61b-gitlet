package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
* Commits class will consist of a log message, timestamp,
* a mapping of file names to blob references, a parent reference,
* and (for merges) a second parent reference.(Reference to CS61B
* project guidelines).
* @author Yuhan Dong
 */
public class Commits implements Serializable {

    /** Contains the commit message.*/
    private String _message;

    /** The unique ID of the commits.*/
    private String commitID;

    /** The Time of the commits.*/
    private Date timeStamp;

    /** The hashmap of blob where the key
     * is the name of the file, and the definition
     * is the Blob ID.
     */
    private HashMap<String, String> blob
            = new HashMap<String, String>();

    /** The initial commit which contains the
     * default information.
     * Commit message: initial commit.
     * Date: 00:00:00 UTC, Thursday, 1 January 1970.
     * Write the object into the file.
     */
    public Commits() {
        timeStamp = new Date(0);
        _message = "initial commit";
        commitID = Utils.sha1((Object) Utils.serialize(this));
        File committedFile = new File(".gitlet/commits/" + this.getCommitID()
                + ".txt");
        Utils.writeObject(committedFile, this);
    }

    /** Create Commit and a file for commit containing the
     * message of this commit. The file name is the commitID.
     * Put the parent blob into the commit to save the snapshot
     * and if they have the same name of the blob, replace that
     * blob.
     * @param message The message of the commit.
     * @param parentBlob The parentBlob to inherit from.
     * @param blobs The new blobs to commit and save.
     */
    public Commits(String message, HashMap<String, String> parentBlob,
                   Blobs[] blobs) {
        this._message = message;
        timeStamp = new Date();
        blob.putAll(parentBlob);
        for (Blobs value : blobs) {
            blob.put(value.getName(), value.getId());
            File blobFile = new File(".gitlet/blobs/" + value.getId() + ".txt");
            Utils.writeObject(blobFile, value);
        }
        commitID = Utils.sha1((Object) Utils.serialize(this));
        File committedFile = new File(".gitlet/commits/" + this.getCommitID()
                + ".txt");
        Utils.writeObject(committedFile, this);
    }

    /** Return the commit message.*/
    public String getMessage() {
        return _message;
    }

    /** Return the commit ID.*/
    public String getCommitID() {
        return commitID;
    }

    /** Return the timeStamp in the ordered format.*/
    public String getTimeStamp() {
        DateFormat format = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        return format.format(timeStamp);
    }

    /** Return the blobs.*/
    public HashMap<String, String> getBlob() {
        return blob;
    }

}
