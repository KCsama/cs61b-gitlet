package gitlet;
import java.io.File;
import java.io.Serializable;

/**
Blobs is a class for the content of the files.
@author Yuhan Dong
 */
public class Blobs implements Serializable {
    /**Name of the file.*/
    private String _name;

    /**ID of the contents of the file.
    *Same contents will have the same ID.
     */
    private String id;

    /**Stores the content of the file as a String.*/
    private String content;

    /* *Blobs constructor.
    *Take in the name of the file, save the content
    *of the file as a sting. Generate a unique ID for
    *the associated file.
     */
    public Blobs(String name, File file) {
        this._name = name;
        content = Utils.readContentsAsString(file);
        id = Utils.sha1(content);
    }

    /**return the name of the file.*/
    public String getName() {
        return _name;
    }

    /**return the unique ID of the current blob.*/
    public String getId() {
        return id;
    }

    /**return the content pf the file.*/
    public String getContent() {
        return content;
    }
}
