package jazsync;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class MetaFileReader {

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
            //nezjistujeme jestli je to URL,
            //ale zkusime rovnou file, kdyz neni, tak pokus o otevreni spojeni
            if(metafile.isFile()){
                readMetaFile();
                //precteme metafile z disku
            } else {
                //zkusime to jako URL
            }
        } else {
            System.out.println("No metafile specified in arguments");
            System.out.println("Try 'jazsync --help' for more info.");
            System.exit(1);
        }
    }

    private void readMetaFile(){
        try {
            BufferedReader in = new BufferedReader(new FileReader(metafile));
            String s;
            String subs;
            int colonIndex;
            while ((s = in.readLine()) != null) {
                if(s.equals("")){
                    //timto radkem zkoncil header
                    
                    break; // namisto break spustime getChecksums a nacteme je do hash table
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
                    mf_length=Integer.parseInt(s.substring(colonIndex+2));
                } else if (subs.equalsIgnoreCase("Hash-Lengths")) {
                    int comma=s.indexOf(",");
                    mf_seq_num=Integer.parseInt(s.substring((colonIndex+2), comma));
                    int nextComma=s.indexOf(",",comma+1);
                    mf_rsum_bytes=Integer.parseInt(s.substring(comma+1, nextComma));
                    mf_checksum_bytes=Integer.parseInt(s.substring(nextComma+1));
                } else if (subs.equalsIgnoreCase("URL")) {
                    mf_url=s.substring(colonIndex+2);
                } else if (subs.equalsIgnoreCase("Z-URL")) {
                    //not implemented yet
                } else if (subs.equalsIgnoreCase("SHA-1")) {
                    mf_sha1=s.substring(colonIndex+2);
                } else if (subs.equalsIgnoreCase("Z-Map2")) {
                    //not implemented yet
                }
            }
            in.close();
        } catch (IOException e) {
            System.out.println("IO problem in metafile header reading");
        }
    }
    

    /**
     * Prints a help message
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
        out.println("* -u, --url=URL                  Specifies URL of origin .zsync file in case that it contains a relative URL");
        out.println("* -s, --suppress                 Suppress the progress bar, download rate and ETA display");
        out.println("  -V, --version                  Show program version");
        out.println("* -v, --verbose                  Trace internal processing");
    }

    /**
     * Prints a version message
     * @param out Output stream (e.g. System.out)
     */
    private void version(PrintStream out){
        out.println("Version: Jazsync v0.0.1 (jazsync)");
        out.println("by Tomáš Hlavnička <hlavntom@fel.cvut.cz>");
    }
}
