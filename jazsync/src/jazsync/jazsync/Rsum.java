package jazsync.jazsync;

import org.jarsync.RollingChecksum;

/**
 * Implementation of rolling checksum for zsync purposes
 * @author Tomáš Hlavnička
 */
public class Rsum implements RollingChecksum, Cloneable, java.io.Serializable {
    private short a;
    private short b;
    private int oldByte;
    private int blockLength;
    private byte[] buffer;

    /**
     * Constructor of rolling checksum
     */
    public Rsum(){
        a = b = 0;
        oldByte  = 0;
    }

    /**
     * Return the value of the currently computed checksum.
     *
     * @return The currently computed checksum.
     */
    @Override
    public int getValue() {
        return ((a & 0xffff) | (b << 16));
    }

    /**
     * Reset the checksum
     */
    @Override
    public void reset() {
        a = b = 0;
        oldByte = 0;
    }

    /**
     * Rolling checksum that takes single byte and compute checksum
     * of block from file in offset that equals offset of newByte 
     * minus length of block
     * 
     * @param newByte New byte that will actualize a checksum
     */
    @Override
    public void roll(byte newByte) {
        a -= unsignedByte(buffer[oldByte]);
        b -= blockLength * unsignedByte(buffer[oldByte]);
        a += unsignedByte(newByte);
        b += a;
        buffer[oldByte]=newByte;
        oldByte++;
        if(oldByte==blockLength){
            oldByte=0;
        }
    }

    /**
     * Update the checksum with an entirely different block, and
     * potentially a different block length.
     *
     * @param buf The byte array that holds the new block.
     * @param offset From whence to begin reading.
     * @param length The length of the block to read.
     */
    @Override
    public void check(byte[] buf, int offset, int length) {
        reset();
        int index=offset;
        for(int i=length;i>0;i--){
            a+=unsignedByte(buf[index]);
            b+=i*unsignedByte(buf[index]);
            index++;
        }
        System.out.println("a: "+a);
        System.out.println("b: "+b);
    }

    /**
     * Update the checksum with an entirely different block, and
     * potentially a different block length. This method is only used to
     * initialize rolling checksum.
     *
     * @param buf The byte array that holds the new block.
     * @param offset From whence to begin reading.
     * @param length The length of the block to read.
     */
    @Override
    public void first(byte[] buf, int offset, int length) {
        reset();
        int index=offset;
        for(int i=length;i>0;i--){
            a+=unsignedByte(buf[index]);
            b+=i*unsignedByte(buf[index]);
            index++;
        }
        blockLength=length;
        buffer = new byte[blockLength];
        System.arraycopy(buf, 0, buffer, 0, length);
    }

    /**
     * Returns "unsigned" value of byte
     *
     * @param b Byte to convert
     * @return Unsigned value of byte <code>b</code>
     */
    private int unsignedByte(byte b){
        if(b<0) return b+256;
        return b;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new Error();
        }
    }

    @Override
    public boolean equals(Object o) {
        return ((Rsum) o).a == a && ((Rsum) o).b == b;
    }
}
