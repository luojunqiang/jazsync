package jazsync;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import org.metastatic.rsync.ChecksumPair;

public class MetaFileReader {
    
    /** File existency and completion flag */
    public static int FILE_FLAG = 0;
     
    /** The short options. */
    private static final String OPTSTRING = "A:u:k:o:shVv";

    /** The long options. */
    private static final LongOpt[] LONGOPTS = new LongOpt[] {
        new LongOpt("url",        LongOpt.REQUIRED_ARGUMENT, null, 'u'),
        new LongOpt("metafile",   LongOpt.REQUIRED_ARGUMENT, null, 'k'),
        new LongOpt("help",       LongOpt.NO_ARGUMENT, null, 'h'),
        new LongOpt("inputfile",  LongOpt.REQUIRED_ARGUMENT, null, 'i'),
        new LongOpt("version",    LongOpt.NO_ARGUMENT, null, 'V'),
        new LongOpt("outputfile", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
        new LongOpt("suppress",   LongOpt.NO_ARGUMENT, null, 's'),
        new LongOpt("verbose",    LongOpt.NO_ARGUMENT, null, 'v')
    };

    private String url;
    private File metafile;
    private String filename;
    private ChainingHash hashtable;
    private int fileOffset;

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
    //------------------------------


    public MetaFileReader(String[] args) {
        Getopt g = new Getopt("jazsync", args, OPTSTRING, LONGOPTS);
        int c;
        Socket s;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'A':
                    System.out.println("Hostname mode (not implemented yet)");
                    break;
                case 'i':
                    System.out.println("Inputfile name (not implemented yet)");
                    break;
                case 'k':
                    System.out.println(".zsync filename (not implemented yet)");
                    break;
                case 'o':
                    System.out.println("Outputfile (not implemented yet)");
                    break;
                case 'u':
                    url=g.getOptarg();
                    break;
                case 's':
                    System.out.println("Suppress mode (not implemented yet)");
                    break;
                case 'h':
                    help(System.out);
                    System.exit(0);
                case 'V':
                    version(System.out);
                    System.exit(0);
                case 'v':
                    System.out.println("Verbose mode (not implemented yet)");
                    break;
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
                checkOutputFile();
                readChecksums();
            } else if (filename.startsWith("http://")) {   // HTTPS ????
                HttpConnection http = new HttpConnection(filename);
                http.openConnection();
                http.getResponseHeader();
                byte[] mfBytes=http.getMetafile();
                readMetaFile(convertBytesToString(mfBytes));
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

    private void checkOutputFile(){
        File file = new File(mf_filename);
        if(file.isFile()){
            SHA1 sha1=new SHA1(file.getPath());
            if(sha1.SHA1sum().equals(mf_sha1)){
                System.out.println("Read "+mf_filename+". Target 100.0% complete.\n"
                        + "verifying download...checksum matches OK\n"
                        + "used "+mf_length+" local, fetched 0");
                System.exit(0);
            } else {
                //soubor mame, ale neni kompletni
                FILE_FLAG = 1;
            }
        } else {
            //nutne stahnout cely soubor
            FILE_FLAG = -1;
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
            //timto prazdnym radkem zkoncil header, muzeme prestat cist
            return true;
        }
        colonIndex = s.indexOf(":");
        subs = s.substring(0, colonIndex);
        if(subs.equalsIgnoreCase("zsync")){
            mf_version=s.substring(colonIndex+2);
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
        while((2 << (i-1)) > ((mf_length/mf_blocksize)+1) && i > 4) {
            i--;
        }
        //vytvorime hashtable o velikosti 2^i (max. 2^16, min. 2^4)
        hashtable = new ChainingHash(2 << (i-1));
        ChecksumPair p;
        Link item;
        int offset=0;
        int weakSum=0;
        int seq=0;
        int off=fileOffset;

        byte[] weak=new byte[mf_rsum_bytes];
        byte[] strongSum=new byte[mf_checksum_bytes];

        while(seq < (mf_length/mf_blocksize)+1){

            for(int w=0;w<weak.length;w++){
                weak[w]=checksums[off];
                off++;
            }
            
            for(int s=0;s<strongSum.length;s++){
                strongSum[s]=checksums[off];
                off++;
            }

            //potreba predelat pro variabilni delku weakSum
            //*********************************************
            if(weak.length<4){
                System.out.println("Hash-length optimalizations are not "
                        + "implemented yet, length of weak "
                        + "checksums needs to be 4 bytes.");
                System.exit(1);
            }
            
            weakSum=0;
            weakSum+=(weak[2] & 0x000000FF) << 24;
            weakSum+=(weak[3] & 0x000000FF) << 16;
            weakSum+=(weak[0] & 0x000000FF) << 8;
            weakSum+=(weak[1] & 0x000000FF);
            //*********************************************
            
            p = new ChecksumPair(weakSum,strongSum,offset,mf_blocksize,seq);
            offset+=mf_blocksize;
            seq++;
            item = new Link(p);
            hashtable.insert(item);           
        }
    }  

    /**
     * Prints out a help message
     * @param out Output stream (e.g. System.out)
     */
    private void help(PrintStream out) {
        out.println("Usage: jazsync [OPTIONS] {filename | url}");
        out.println("");
        out.println("OPTIONS: * == option currently unimplemented");
        out.println("  -h, --help                     Show this help message");
        out.println("* -A HOSTNAME=USERNAME:PASSWD    Specifies a username and password to be used with the given hostname (can be used multiple times)");
        out.println("* -i, --inputfile=FILENAME       Specifies (extra) input files");
        out.println("* -k, --metafile=FILENAME        Indicates  that zsync should save the zsync file that it downloads, with the given filename");
        out.println("* -o, --outputfile=NEWNAME       Override the default output file name");
        out.println("* -u, --url=URL                  Specifies URL of .zsync file in case that it contains a relative URL");
        out.println("* -s, --suppress                 Suppress the progress bar, download rate and ETA display");
        out.println("  -V, --version                  Show program version");
        out.println("* -v, --verbose                  Trace internal processing");
    }

    /**
     * Prints out a version message
     * @param out Output stream (e.g. System.out)
     */
    private void version(PrintStream out){
        out.println("Version: Jazsync v0.0.1 (jazsync)");
        out.println("by Tomáš Hlavnička <hlavntom@fel.cvut.cz>");
    }

    public ChainingHash getHashtable() {
        return hashtable;
    }

    public URL getMetaFileURL() throws MalformedURLException{
        return new URL(filename);
    }

    public int getBlocksize() {
        return mf_blocksize;
    }

    public int getChecksumBytes() {
        return mf_checksum_bytes;
    }

    public String getFilename() {
        return mf_filename;
    }

    public long getLength() {
        return mf_length;
    }

    public String getMtime() {
        return mf_mtime;
    }

    public int getRsumBytes() {
        return mf_rsum_bytes;
    }

    public int getSeqNum() {
        return mf_seq_num;
    }

    public String getSha1() {
        return mf_sha1;
    }

    public String getUrl() {
        return mf_url;
    }

    public String getVersion() {
        return mf_version;
    }

}
