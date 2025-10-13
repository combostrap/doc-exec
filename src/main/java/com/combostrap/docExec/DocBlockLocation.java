package com.combostrap.docExec;

/**
 * A location of a block
 */
public class DocBlockLocation {

    final int start;
    final int end;

    /**
     *
     * @param start the start position in number of characters in the file
     * @param end the end position in number of characters in the file
     */
    public DocBlockLocation(int start, int end) {
        this.start = start;
        this.end = end;
    }

    /**
     * @return the start position in number of characters in the file
     */
    public int getStart() {
        return start;
    }

    /**
     * @return the end position in number of characters in the file
     */
    public int getEnd() {
        return end;
    }
}
