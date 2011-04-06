package jazsync;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
    private String Version="zsync: ";
    private String Filename="Filename: ";
    private String MTime="MTime: ";
    private String Blocksize="Blocksize: ";
    private String Length="Length: ";
    private String HashLengths="Hash-Lengths: ";
    private String URL="URL: ";
    private String SHA1="SHA-1: ";
    private SHA1 sha1;

    public HeaderMaker(File file, String url, String blocksize){
        Version+="0.0.0";
        Filename+=file.getName();
        MTime+=now("EEE, dd MMMMM yyyy hh:mm:ss z");
        Length+=file.length();
        if(url==null){
            URL+=file.toString(); //default
        } else {
            URL+=url;
        }
        if(blocksize==null){
            Blocksize+="2048"; //default
        } else {
            Blocksize+=blocksize;
        }
        HashLengths+="2,2,4";
        sha1 = new SHA1(file.toString());
        SHA1+=sha1.SHA1sum();
    }


    private String now(String dateFormat) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(cal.getTime());
    }

    public String getBlocksize() {
        return Blocksize;
    }

    public String getFilename() {
        return Filename;
    }

    public String getHashLengths() {
        return HashLengths;
    }

    public String getLength() {
        return Length;
    }

    public String getMTime() {
        return MTime;
    }

    public String getSHA1() {
        return SHA1;
    }

    public String getURL() {
        return URL;
    }

    public String getVersion() {
        return Version;
    }

    public SHA1 getSha1() {
        return sha1;
    }

    public String getHeader(){
        String all="";
        all+=Version+"\n";
        all+=Filename+"\n";
        all+=MTime+"\n";
        all+=Blocksize+"\n";
        all+=Length+"\n";
        all+=HashLengths+"\n";
        all+=URL+"\n";
        all+=SHA1;
        return all;
    }
}