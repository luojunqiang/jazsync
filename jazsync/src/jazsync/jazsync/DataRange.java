package jazsync.jazsync;

public class DataRange {
    private long start;
    private long end;

    public DataRange(long start, long end){
        this.start=start;
        this.end=end;
    }

    /**
     * Returns range in String format ("start-end"), ready to be put into
     * HTTP range request
     * @return Range of data in stream
     */
    public String getRange(){
        return start + "-" + end;
    }

    /**
     * Returns offset where block starts in the final file
     * @return Offset where block starts
     */
    public long getOffset(){
        return start;
    }
}
