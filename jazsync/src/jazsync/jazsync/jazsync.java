package jazsync.jazsync;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.PrintStream;
import jazsync.jazsyncmake.MetaFileMaker;

public class jazsync {
    /** The short options */
    private static final String OPTSTRING = "o:f:b:A:r:u:k:i:Vmh";

    /** The long options. */
    private static final LongOpt[] LONGOPTS = new LongOpt[] {
        new LongOpt("make",        LongOpt.NO_ARGUMENT, null, 'm'),
        new LongOpt("help",        LongOpt.NO_ARGUMENT, null, 'h'),
        new LongOpt("url",        LongOpt.REQUIRED_ARGUMENT, null, 'u'),
        new LongOpt("metafile",   LongOpt.REQUIRED_ARGUMENT, null, 'k'),
        new LongOpt("inputfile",  LongOpt.REQUIRED_ARGUMENT, null, 'i'),
        new LongOpt("ranges",     LongOpt.REQUIRED_ARGUMENT, null, 'r'),
        new LongOpt("version",    LongOpt.NO_ARGUMENT, null, 'V'),
        new LongOpt("blocksize",  LongOpt.REQUIRED_ARGUMENT, null, 'b'),
        new LongOpt("filename",   LongOpt.REQUIRED_ARGUMENT, null, 'f'),
        new LongOpt("outputfile", LongOpt.REQUIRED_ARGUMENT, null, 'o')
    };

    private static boolean make=false;
    
    public static void main(String[] args) {
        Getopt g = new Getopt("jazsync", args, OPTSTRING, LONGOPTS);
        int c;

        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'm':
                    make=true;
                    break;
                case 'h':
                    help(System.out);
                    System.exit(0);
                    break;
                case 'V':
                    version(System.out);
                    System.exit(0);
                    break;
                case '?':
                    System.out.println("Try 'jazsync --help' for more info.");
                    System.exit(1);
                    break;
                default:
            }
        }

        if(make){
            MetaFileMaker mfm=new MetaFileMaker(args);
        } else {
            FileMaker fm = new FileMaker(args);
        }
    }

    /**
     * Prints out a help message
     * @param out Output stream (e.g. System.out)
     */
    public static void help(PrintStream out) {
        out.println("");
        out.println("Usage: jazsync [OPTIONS] {local metafilename | url}");
        out.println("");
        out.println("Usage: jazsync --make [OPTIONS] filename");
        out.println("");
        out.println("JAZSYNC OPTIONS: ");
        out.println("  -h, --help                     Show this help message");
        out.println("  -A USERNAME:PASSWORD           Specifies a username and password if there is authentication needed");
        out.println("  -i, --inputfile FILENAME       Specifies (extra) input file");
        out.println("  -r, --ranges NUMBER            Maximum of simultaneously downloaded blocks (1-100)");
        out.println("  -k, --metafile FILENAME        Indicates that jazsync should download the metafile, with the given filename");
        out.println("  -u, --url URL                  Specifies original URL of local .zsync file in case that it contains a relative URL");
        out.println("  -V, --version                  Show program version");
        out.println("-----");
        out.println("JAZSYNCMAKE OPTIONS: ");
        out.println("  -h, --help                     Show this help message");
        out.println("  -b, --blocksize NUMBER         Specifies blocksize");
        out.println("  -f, --filename FILENAME        Set new filename of output file");
        out.println("  -o, --outputfile FILENAME      Override the default filename and path of metafile");
        out.println("  -u, --url URL                  Specifies the URL from which users can download the content");
        out.println("  -V, --version                  Show program version");
    }


    /**
     * Prints out a version message
     * @param out Output stream (e.g. System.out)
     */
    public static void version(PrintStream out){
        out.println("Version: Jazsync v0.8.1");
        out.println("by Tomáš Hlavnička <hlavntom@fel.cvut.cz>");
    }
}