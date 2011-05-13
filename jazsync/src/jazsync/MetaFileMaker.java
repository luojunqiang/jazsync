package jazsync;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import org.metastatic.rsync.ChecksumPair;
import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.Generator;
import org.metastatic.rsync.JarsyncProvider;

public class MetaFileMaker {
    
    /** Default block size */
    private static final int BLOCK_SIZE=2048;

    /** Default length of strong checksum (MD4) */
    private static final int STRONG_SUM_LENGTH=16;

    /** The short options. */
    private static final String OPTSTRING = "o:u:f:b:hVv";

    /** The long options. */
    private static final LongOpt[] LONGOPTS = new LongOpt[] {
        new LongOpt("blocksize",  LongOpt.REQUIRED_ARGUMENT, null, 'b'),
        new LongOpt("url",        LongOpt.REQUIRED_ARGUMENT, null, 'u'),
        new LongOpt("help",       LongOpt.NO_ARGUMENT, null, 'h'),
        new LongOpt("filename",   LongOpt.REQUIRED_ARGUMENT, null, 'f'),
        new LongOpt("version",    LongOpt.NO_ARGUMENT, null, 'V'),
        new LongOpt("outputfile", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
        new LongOpt("verbose",    LongOpt.NO_ARGUMENT, null, 'v')
    };

    private String url;
    private int blocksize;
    private int strongSumLength;
    private String filename;
    private File file;
    private String outputfile;
    private boolean noURL=true;
    private boolean newNameFile=false;
    private String header;
    
    
    public MetaFileMaker(String[] args) {
        Security.addProvider(new JarsyncProvider());
        blocksize=BLOCK_SIZE;
        
        //nutne optimalizace ke zmene delky kontrolnich souctu
        strongSumLength=STRONG_SUM_LENGTH; 

        Getopt g = new Getopt("jazsyncmake", args, OPTSTRING, LONGOPTS);
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'f':
                    filename = g.getOptarg();
                    break;
                case 'u':
                    url = g.getOptarg();
                    noURL=false;
                    break;
                case 'b':
                    try {
                        blocksize = Integer.parseInt(g.getOptarg());
                    } catch (NumberFormatException e) {
                        System.out.println("Blocksize must be a power of 2 (512, 1024, 2048, ...)");
                        System.exit(1);
                    }
                    break;
                case 'o':
                   outputfile = g.getOptarg();
                   newNameFile=true;
                   break;
                case '?':
                    System.out.println("Try 'jazsyncmake --help' for more info.");
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
                default:
                    System.out.print("getopt() returned " + c + "\n");
            }
        }

        //getting filename from arguments
        if (args.length > g.getOptind()) {
            file = new File(args[g.getOptind()]);
            if(file.isDirectory()){
                System.out.println("Open: Directory as argument; Operation not permitted");
                System.exit(1);
            } else if(!file.isFile()){
                System.out.println("Open: No such file");
                System.exit(1);
            }
            if(!newNameFile){
                outputfile=file.getName()+".zsync";
            }
        } else {
            System.out.println("No file specified in arguments");
            System.exit(1);
        }

        /**
         * zde by mela probehnout analyza velikosti souboru v pomeru k blocksize
         * a nasledny vypocet hash-lengths
         */

        //creating header and saving it into the created metafile
        HeaderMaker hm=new HeaderMaker(file,filename,url,blocksize,strongSumLength);
        header=hm.getFullHeader();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputfile));
            header.replaceAll("\n", System.getProperty("line.separator"));
            out.write(header);
            out.close();
        } catch (IOException e){
            System.out.println("Can not create .zsync metafile.");
            System.exit(1);
        }

        //appending block checksums into the metafile
        try {
            FileOutputStream fos=new FileOutputStream(outputfile, true);
            Configuration config = new Configuration();
            config.strongSum = MessageDigest.getInstance("MD4");
            config.weakSum = new Rsum();
            config.blockLength = blocksize;
            config.strongSumLength = strongSumLength;
            Generator gen = new Generator(config);
            List<ChecksumPair> list = new ArrayList<ChecksumPair>(Math.round((float)file.length() / (float)blocksize));
            list = gen.generateSums(file);
            for(ChecksumPair p : list){
                fos.write(intToBytes(p.getWeak()));
                fos.write(p.getStrong());
                //System.out.println(p.toString());
            }
        } catch (IOException ioe){
            System.out.println("IO problem in metafile checksums writing");
        } catch (NoSuchAlgorithmException nae){
            System.out.println("MD4 is not working");
        }

        if(noURL){
            System.out.println("No URL given, so I am including a relative "
                    + "URL in the .zsync file - you must keep the file being"
                    + " served and the .zsync in the same public directory. "
                    + "Use -u "+file.getName()+" to get this same result without this warning.");
        }
    }

    /**
     * Converting integer weakSum into byte array that zsync can read
     * (hton byte order)
     * @param number weakSum in integer form
     * @return converted byte array readable by zsync (hton byte order)
     */
    private byte[] intToBytes(int number){
        return new byte[] {
            (byte)((number << 16) >> 24), //[2]
            (byte)((number << 24) >> 24), //[3]
            (byte)( number >> 24),        //[0]
            (byte)((number << 8) >> 24)}; //[1]
    }

    /**
     * Prints out a help message
     * @param out Output stream (e.g. System.out)
     */
    private void help(PrintStream out) {
        out.println("Usage: jazsyncmake [OPTIONS] filename");
        out.println("");
        out.println("OPTIONS: * == option currently unimplemented");
        out.println("  -h, --help                     Show this help message");
        out.println("  -b, --blocksize=BYTES          Specifies blocksize");
        out.println("  -f, --filename=NEWNAME         Set new filename of output file");
        out.println("  -o, --outputfile=NEWNAME       Override the default filename and path of metafile");
        out.println("  -u, --url=URL                  Specifies the URL from which users can download the content");
        out.println("  -V, --version                  Show program version");
        out.println("* -v, --verbose                  Trace internal processing");
    }

    /**
     * Prints out a version message
     * @param out Output stream (e.g. System.out)
     */
    private void version(PrintStream out){
        out.println("Version: Jazsync v0.0.1 (jazsyncmake)");
        out.println("by Tomáš Hlavnička <hlavntom@fel.cvut.cz>");
    }
}
