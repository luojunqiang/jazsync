/* MetafileReader.java

   MetafileReader: Metafile reader class
   Copyright (C) 2011 Tomas Hlavnicka <hlavntom@fel.cvut.cz>

   This file is a part of Jazsync.

   Jazsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2 of the License, or (at
   your option) any later version.

   Jazsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jazsync; if not, write to the

      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA
 */

package jazsync.jazsync;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jarsync.ChecksumPair;

/**
 * Class used to read metafile
 * @author Tomáš Hlavnička
 */
public class MetaFileReader {

    /** File existency and completion flag */
    public int FILE_FLAG = 0;

    /** The short options. */
    private static final String OPTSTRING = "A:u:k:i:hV";

    /** The long options. */
    private static final LongOpt[] LONGOPTS = new LongOpt[] {
        new LongOpt("url",        LongOpt.REQUIRED_ARGUMENT, null, 'u'),
        new LongOpt("metafile",   LongOpt.REQUIRED_ARGUMENT, null, 'k'),
        new LongOpt("help",       LongOpt.NO_ARGUMENT, null, 'h'),
        new LongOpt("inputfile",  LongOpt.REQUIRED_ARGUMENT, null, 'i'),
        new LongOpt("version",    LongOpt.NO_ARGUMENT, null, 'V'),
    };


    private File metafile;
    private String filename;
    private ChainingHash hashtable;
    private int fileOffset;
    private int blockNum;

    /** Authentication variables */
    private String username;
    private String passwd;
    private boolean authing=false;

    /** Variables for header information from .zsync metafile */
    //------------------------------
    private String mf_version;
    private String mf_filename;
    private String mf_mtime;
    private int mf_blocksize;
    private long mf_length;
    private int mf_seq_num;
    private int mf_rsum_bytes;
    private int mf_checksum_bytes;
    private String mf_url;
    private String mf_sha1;
    private String auth;
    //------------------------------
    private String url;
    private String localMetafile;
    private boolean downMetaFile=false;
    private String extraInputFile;

    /** Option variables */

    /**
     * Metafile constructor
     * @param args Arguments
     */
    public MetaFileReader(String[] args) {
        Getopt g = new Getopt("jazsync", args, OPTSTRING, LONGOPTS);
        int c;

        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'A':
                    auth=g.getOptarg();
                    parseAuthentication();
                    authing=true;
                    break;
                case 'i':
                    extraInputFile=g.getOptarg();
                    break;
                case 'k':
                    localMetafile=g.getOptarg();
                    downMetaFile=true;
                    break;
                case 'u':
                    url=g.getOptarg();
                    break;
                case 'h':
                    help(System.out);
                    System.exit(0);
                case 'V':
                    version(System.out);
                    System.exit(0);
                case '?':
                    System.out.println("Try 'jazsync --help' for more info.");
                    break;
                default:
                    System.out.print("getopt() returned " + c + "\n");
            }
        }

        //getting filename from arguments
        if (args.length > g.getOptind()) {
            filename = args[g.getOptind()];
            metafile=new File(filename);

            /*
             * zjistime jestli soubor na disku existuje, pokud ne, presvedcime
             * se zda to tedy je URL s definovanym http protokolem, pokud ne,
             * asi jde o soubor, ten vsak neexistuje -> konec programu
             */
            if(metafile.isFile()){
                readMetaFile();
                blockNum = Math.round((float)mf_length / (float)mf_blocksize);
                checkOutputFile();
                readChecksums();
            } else if (filename.startsWith("http://")) {
                HttpConnection http = new HttpConnection(filename);
                http.openConnection();
                if(authing==true){
                    http.setAuthentication(username, passwd);
                }
                http.sendRequest();
                http.getResponseHeader();
                byte[] mfBytes=http.getResponseBody(0);
                if(downMetaFile){
                    try {
                        OutputStream out = new FileOutputStream(new File(localMetafile));
                        out.write(mfBytes);
                    } catch (IOException ex) {
                        System.out.println("Can't write metafile locally, check your permissions");
                        System.exit(1);
                    }
                }
                http.closeConnection();
                readMetaFile(convertBytesToString(mfBytes));
                blockNum = Math.round((float)mf_length / (float)mf_blocksize);
                checkOutputFile();
                fillHashTable(mfBytes);
            } else {
                System.out.println(metafile+": No such file or directory");
                System.exit(1);
            }

        } else {
            System.out.println("No metafile specified in arguments");
            System.out.println("Try 'jazsync --help' for more info.");
            System.exit(1);
        }
    }



    /**
     * Method used to check outputfile if exist and is(not) complete
     */
    private void checkOutputFile(){
        File file = new File(mf_filename);
        if(file.isFile()){
            SHA1check(file);
        } else {
            if(extraInputFile!=null && new File(extraInputFile).exists()){
                SHA1check(new File(extraInputFile));
            } else {
                //nutne stahnout cely soubor
                FILE_FLAG = -1;
            }
        }
    }

    private void SHA1check(File file){
        SHA1 sha1=new SHA1(file.getPath());
        if(sha1.SHA1sum().equals(mf_sha1)){
                System.out.println("Read "+file.getName()+". Target 100.0% complete.\n"
                        + "verifying download...checksum matches OK\n"
                        + "used "+mf_length+" local, fetched 0");
                System.exit(0);
        } else {
            //soubor mame, ale neni kompletni
            FILE_FLAG = 1;
        }
    }


    /**
     * Parsing method for metafile headers, saving each value into separate variable.
     * @param s String containing metafile
     * @return Boolean value notifying whether header ended or not (true = end of header)
     */
    private boolean parseHeader(String s){
        String subs;
        int colonIndex;
        if(s.equals("")){
            //timto prazdnym radkem skoncil header, muzeme prestat cist
            return true;
        }
        colonIndex = s.indexOf(":");
        subs = s.substring(0, colonIndex);
        if(subs.equalsIgnoreCase("zsync")){
            mf_version=s.substring(colonIndex+2);
            //zkontrolujeme kompatibilitu
            if(mf_version.equals("0.0.4") || mf_version.equals("0.0.2")){
                System.out.println("This version is not compatible with zsync streams in versions up to 0.0.4");
                System.exit(1);
            }
        } else if (subs.equalsIgnoreCase("Filename")) {
            mf_filename=s.substring(colonIndex+2);
        } else if (subs.equalsIgnoreCase("MTime")) {
            mf_mtime=s.substring(colonIndex+2);
        } else if (subs.equalsIgnoreCase("Blocksize")) {
            mf_blocksize=Integer.parseInt(s.substring(colonIndex+2));
        } else if (subs.equalsIgnoreCase("Length")) {
            mf_length=Long.parseLong(s.substring(colonIndex+2));
        } else if (subs.equalsIgnoreCase("Hash-Lengths")) {
            int comma=s.indexOf(",");
            mf_seq_num=Integer.parseInt(s.substring((colonIndex+2), comma));
            int nextComma=s.indexOf(",",comma+1);
            mf_rsum_bytes=Integer.parseInt(s.substring(comma+1, nextComma));
            mf_checksum_bytes=Integer.parseInt(s.substring(nextComma+1));
            //zkontrolujeme validni hash-lengths
            if((mf_seq_num < 1 || mf_seq_num > 2) ||
               (mf_rsum_bytes < 1 || mf_rsum_bytes > 4) ||
               (mf_checksum_bytes < 3 || mf_checksum_bytes > 16)){
                System.out.println("Nonsensical hash lengths line "+s.substring(colonIndex+2));
                System.exit(1);
            }

        } else if (subs.equalsIgnoreCase("URL")) {
            mf_url=s.substring(colonIndex+2);
        } else if (subs.equalsIgnoreCase("Z-URL")) {
            //not implemented yet
        } else if (subs.equalsIgnoreCase("SHA-1")) {
            mf_sha1=s.substring(colonIndex+2);
        } else if (subs.equalsIgnoreCase("Z-Map2")) {
            //not implemented yet
        }
        return false;
    }

    /**
     * Method reads metafile from file and reads
     * it line by line, sending line String to parser.
     */
    private void readMetaFile(){
        try {
            BufferedReader in = new BufferedReader(new FileReader(metafile));
            String s;
            while ((s = in.readLine()) != null) {
                if(parseHeader(s)){
                    break;
                }
            }
            in.close();
        } catch (IOException e) {
            System.out.println("IO problem in metafile header reading");
        }
    }
    /**
     * Method reads metafile from String and reads
     * it line by line, sending line String to parser.
     * @param s Metafile in String form
     */
    private void readMetaFile(String s){
        try{
            BufferedReader in = new BufferedReader(new StringReader(s));
            while ((s = in.readLine()) != null) {
                if(parseHeader(s)){
                    break;
                }
            }
            in.close();
        } catch (IOException e) {
            System.out.println("IO problem in metafile header reading");
        }
    }

    /**
     * Method converts downloaded metafile from byte array into String and
     * saves offset where headers end and blocksums starts.
     * @param bytes
     * @return
     */
    private String convertBytesToString(byte[] bytes){
        for(int i=2;i<bytes.length;i++){
            if(bytes[i-2]==10 && bytes[i-1]==10){
                fileOffset=i;
                break;
            }
        }
        String header = new String(bytes);
        return header;
    }

    /**
     * Method that reads metafile from file and stores its content into byte array
     * and saves offset where headers end and blocksums starts.
     */
    private void readChecksums() {
        long length=metafile.length();
        if (metafile.length() > Integer.MAX_VALUE) {
                System.out.println("Metafile is too large");
                System.exit(1);
            }
        byte[] bytes = new byte[(int)length];

        try {
            InputStream is = new FileInputStream(metafile);
            int offset = 0;
            int n = 0;
            while (offset<bytes.length && (n=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += n;
            }

            // Presvedcime se, ze jsme precetli cely soubor
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file "+metafile.getName());
            }

            // Zavre stream
            is.close();
            } catch (IOException e ) {
                System.out.println("IO problem in metafile reading");
            }
        // urci offset, kde konci hlavicka a zacinaji kontrolni soucty
        fileOffset = 0;
        for(int i=2;i<bytes.length;i++){
            if(bytes[i-2]==10 && bytes[i-1]==10){
                fileOffset=i;
                break;
            }
        }
        fillHashTable(bytes);
    }

    /**
     * Fills a chaining hash table with ChecksumPairs
     * @param checksums Byte array with bytes of whole metafile
     */
    private void fillHashTable(byte[] checksums){
        int i=16;
        //spocteme velikost hashtable podle poctu bloku dat
        while((2 << (i-1)) > blockNum && i > 4) {
            i--;
        }
        //vytvorime hashtable o velikosti 2^i (max. 2^16, min. 2^4)
        hashtable = new ChainingHash(2 << (i-1));
        ChecksumPair p = null;
        Link item;
        int offset=0;
        int weakSum=0;
        int seq=0;
        int off=fileOffset;

        byte[] weak=new byte[4];
        byte[] strongSum=new byte[mf_checksum_bytes];

        while(seq < blockNum){

            for(int w=0;w<mf_rsum_bytes;w++){
                weak[w]=checksums[off];
                off++;
            }

            for(int s=0;s<strongSum.length;s++){
                strongSum[s]=checksums[off];
                off++;
            }

            //*********************************************
            weakSum=0;
            weakSum+=(weak[2] & 0x000000FF) << 24;
            weakSum+=(weak[3] & 0x000000FF) << 16;
            weakSum+=(weak[0] & 0x000000FF) << 8;
            weakSum+=(weak[1] & 0x000000FF);
            //*********************************************
            p = new ChecksumPair(weakSum,strongSum.clone(),offset,mf_blocksize,seq);
            offset+=mf_blocksize;
            seq++;
            item = new Link(p);
            hashtable.insert(item);
        }
    }

    /**
     * Method used to parsing authentication information (user:pass)
     */
    private void parseAuthentication(){
        username = auth.substring(auth.indexOf("=")+1, auth.indexOf(":"));
        passwd = auth.substring(auth.indexOf(":")+1);
    }

    /**
     * Prints out a help message
     * @param out Output stream (e.g. System.out)
     */
    private void help(PrintStream out) {
        out.println("Usage: jazsync [OPTIONS] {local metafilename | url}");
        out.println("");
        out.println("OPTIONS: * == option currently unimplemented");
        out.println("  -h, --help                     Show this help message");
        out.println("  -A USERNAME:PASSWORD           Specifies a username and password if there is authentication needed");
        out.println("  -i, --inputfile FILENAME       Specifies (extra) input file");
        out.println("  -k, --metafile FILENAME        Indicates that jazsync should download the metafile, with the given filename");
        out.println("  -u, --url URL                  Specifies original URL of local .zsync file in case that it contains a relative URL");
        out.println("  -V, --version                  Show program version");
    }

    /**
     * Prints out a version message
     * @param out Output stream (e.g. System.out)
     */
    private void version(PrintStream out){
        out.println("Version: Jazsync v0.7.0 (jazsync)");
        out.println("by Tomáš Hlavnička <hlavntom@fel.cvut.cz>");
    }

    /**
     * Returns value indicating whetever the authentication is neccessary
     * @return Boolean value
     */
    public boolean getAuthentication(){
        return authing;
    }

    /**
     * Authentication username
     * @return Username used for authentication
     */
    public String getUsername(){
        return username;
    }

    /**
     * Authentication password
     * @return Password used for authentication
     */
    public String getPassword(){
        return passwd;
    }

    /**
     * Returns hash table cotaining block checksums
     * @return Hash table
     */
    public ChainingHash getHashtable() {
        return hashtable;
    }

    /**
     * Returns number of blocks in complete file
     * @return Number of blocks
     */
    public int getBlockCount(){
        return blockNum;
    }

    /**
     * Returns metafile URL
     * @return Metafile URL in String format
     */
    public String getMetaFileURL(){
        return filename;
    }

    /**
     * Returns size of block
     * @return Size of the data block
     */
    public int getBlocksize() {
        return mf_blocksize;
    }

    /**
     * Length of used strong sum
     * @return Length of strong sum
     */
    public int getChecksumBytes() {
        return mf_checksum_bytes;
    }

    /**
     * Returns name of the file that we are trying to synchronize
     * @return Name of the file
     */
    public String getFilename() {
        return mf_filename;
    }

    /**
     * Returns length of complete file
     * @return Length of the file
     */
    public long getLength() {
        return mf_length;
    }

    /**
     * Last modified time of file stored in metafile stored
     * in string format ("EEE, dd MMM yyyy HH:mm:ss Z")
     * @return String form of mtime
     */
    public String getMtime() {
        return mf_mtime;
    }

    /**
     * Length of used weak sum
     * @return Length of weak sum
     */
    public int getRsumBytes() {
        return mf_rsum_bytes;
    }

    /**
     * Number of consequence blocks
     * @return Number of consequence blocks
     */
    public int getSeqNum() {
        return mf_seq_num;
    }

    /**
     * Returns SHA1sum of complete file
     * @return String containing SHA1 sum of complete file
     */
    public String getSha1() {
        return mf_sha1;
    }

    /**
     * Return URL of complete file
     * @return URL address in String format
     */
    public String getUrl() {
        return mf_url;
    }

    /**
     * Return URL as origin of local metafile (in case that metafile contains
     * relative URL to a file)
     * @return URL address in String format
     */
    public String getRelativeURL() {
        return url;
    }

    /**
     * Returns filename of seeding file
     * @return Filename of extra seeding file
     */
    public String getInputFile(){
        return extraInputFile;
    }

}