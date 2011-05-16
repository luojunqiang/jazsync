package jazsync.jazsyncmake;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import jazsync.jazsync.SHA1;

public class HeaderMaker {
/*
zsync: 0.6.1
Filename: tinycore.iso
MTime: Sat, 06 Mar 2010 09:33:36 +0000
Blocksize: 2048
Length: 11483136
Hash-Lengths: 2,2,5
URL: http://i.iinfo.cz/files/root/240/tinycore.iso
SHA-1: 5944ec77b9b0f2d6b8212d142970117f5801430a
*/

    /** Nutne dodelat hlavicky tykajici se komprimovanych streamu
     * ++++ Z-URL, Z-Filename, Z-Map2, Recompress, Safe
     */
    private String Version="zsync: ";
    private String Filename="Filename: ";
    private String MTime="MTime: ";
    private String Blocksize="Blocksize: ";
    private String Length="Length: ";
    private String HashLengths="Hash-Lengths: ";
    private String URL="URL: ";
    private String ZURL="Z-URL: "; //nahradi URL v pripade gz
    private String SHA1="SHA-1: ";
    private String ZMAP2="Z-Map2: ";
    private SHA1 sha1;

    private int blocksize;
    private String filename;
    private long length;
    private String url;
    private int seq_num=1;
    private int rsum_bytes=4;
    private int checksum_bytes=16;
    private long mtime=0;

    public HeaderMaker(File file, String filename, String url, int blocksize, int checksum_bytes){
        Version+="jazsync";
        this.mtime=file.lastModified();
        if(filename==null){
            Filename+=file.getName();   //default
            this.filename=file.getName();
        } else {
            Filename+=filename;         //new file name
            this.filename=filename;
        }

        MTime+=now("EEE, dd MMM yyyy HH:mm:ss Z");
        Length+=file.length();
        length=file.length();

        if(url==null){
            URL+=file.getName();        //default
            this.url=file.getName();
        } else {
            URL+=url;                   //new url
            this.url=url;
        }

        if(blocksize==2048){
            Blocksize+="2048";          //default
        } else if (isPowerOfTwo(blocksize)) {
            Blocksize+=blocksize;       //new blocksize
            this.blocksize=blocksize;
        } else {
            System.out.println("Blocksize must be a power of 2 (512, 1024, 2048, ...)");
            System.exit(1);
        }
        
        this.checksum_bytes=checksum_bytes;
        HashLengths+=(this.seq_num+","+this.rsum_bytes+","+this.checksum_bytes);
        sha1 = new SHA1(file.toString());
        SHA1+=sha1.SHA1sum();
    }
    
    private boolean isPowerOfTwo(int number){
        boolean isPowerOfTwo = true;
        while(number>1){
            if(number%2 != 0){
                isPowerOfTwo = false;
                break;
            } else {
                number=number/2;
            }
        }
        return isPowerOfTwo;
    }


    private String now(String dateFormat) {
        Date date = new Date();
        date.setTime(mtime);
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat,Locale.US);
        return sdf.format(date);
    }

    public String getFullHeader(){
        StringBuilder sb = new StringBuilder("");
        sb.append(Version).append("\n");
        sb.append(Filename).append("\n");
        sb.append(MTime).append("\n");
        sb.append(Blocksize).append("\n");
        sb.append(Length).append("\n");
        sb.append(HashLengths).append("\n");
        sb.append(URL).append("\n");
        sb.append(SHA1).append("\n\n");
        String header = sb.toString();
        return header;
    }
}