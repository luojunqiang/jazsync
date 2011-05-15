package jazsync;

public class DataRange {
    private long start;
    private long end;

    public DataRange(long start, long end){
        this.start=start;
        this.end=end;
    }

    /**
     * Returns range in String format ("start-end")
     * @return Range of data in stream
     */
    public String getRange(){
        return start + "-" + end;
    }

    /**
     * Returns offset where block starts
     * @return Offset where block starts
     */
    public long getOffset(){
        return start;
    }
}
