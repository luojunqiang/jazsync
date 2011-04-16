package jazsync;

import org.metastatic.rsync.RollingChecksum;

public class Rsum implements RollingChecksum, Cloneable, java.io.Serializable {
    private short a;
    private short b;
    private short k,l;
    private byte[] block;
    private int char_offset=0;

    public Rsum(){
        a=b=0;
        k=0;
    }

    @Override
    public int getValue() {
        return ((a & 0xffff) | (b << 16));
    }

    @Override
    public void reset() {
        a = b = k = l = 0;
    }

    @Override
    public void roll(byte bt) {
        a -= block[k] + char_offset;
        b -= l * (block[k] + char_offset);
        a += bt + char_offset;
        b += a;
        block[k] = bt;
        k++;
        if (k == l) k = 0;
    }

    @Override
    public void trim() {
        a -= block[k % block.length] + char_offset;
        b -= l * (block[k % block.length] + char_offset);
        k++;
        l--;
    }

    @Override
    public void check(byte[] buf, int offset, int length) {
        reset();
        int index=0;
        for(int i=length;i>0;i--){
            a+=unsignedByte(buf[index]);
            b+=i*unsignedByte(buf[index]);
            index++;
        }
    }

    public int unsignedByte(byte b){
        if(b<0) return b+256;
        return b;
    }

    @Override
  public Object clone() {
    try
      {
        return super.clone();
      }
    catch (CloneNotSupportedException cnse)
      {
        throw new Error();
      }
  }

    @Override
  public boolean equals(Object o) {
    return ((Rsum)o).a == a && ((Rsum)o).b == b;
  }
}
