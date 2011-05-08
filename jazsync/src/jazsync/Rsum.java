package jazsync;

import org.metastatic.rsync.RollingChecksum;

public class Rsum implements RollingChecksum, Cloneable, java.io.Serializable {
    private short a;
    private short b;
    private int oldByte;
    private int blockLength;
    private byte[] buffer;

    public Rsum(){
        a = b = 0;
        oldByte  = 0;
    }

    @Override
    public int getValue() {
        return ((a & 0xffff) | (b << 16));
    }

    @Override
    public void reset() {
        a = b = 0;
        oldByte = 0;
    }

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

    @Override
    public void trim() {
//        a -= buffer[newByte % buffer.length];
//        b -= newByte * (buffer[oldByte % buffer.length]);
//        oldByte++;
//        newByte--;
    }

    @Override
    public void check(byte[] buf, int offset, int length) {
        reset();
        int index=offset;
        for(int i=length;i>0;i--){
            a+=unsignedByte(buf[index]);
            b+=i*unsignedByte(buf[index]);
            index++;
        }
    }

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
