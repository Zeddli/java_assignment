package model;

/** Anything saved as one line in a .txt file. */
public interface Storable {
    String getId();
    String toLine();
}

