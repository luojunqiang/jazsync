package jazsync;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.metastatic.rsync.Checksum32;
import org.metastatic.rsync.*;
import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.Generator;

public class jazsyncmake {
    private static String url=null;
    private static int blocksize=2048;
    private static String filename=null;
    private static File file;
    private static boolean noURL=false;
    static String[] argumenty={"-u","-U","-b","-o","-f","-z","-Z","-e","-C","-v"};
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
//        if(args.length==0){
//            System.out.println("Use jazsyncmake --help for more instructions.");
//            System.exit(0);
//        }
//        if(args[0].equals("--help")){
//            System.out.println("Here is help.");
//            System.exit(0);
//        } else {
//            if(args.length>1){
//                for (int i = 0; i < args.length; i++){
//                    if(args[i].startsWith("-")){
//                        System.out.println("JE TAM -");
//                    }
//                }
//            } else if (args.length==1) {
//                file = new File(args[0]);
//            }
//        }
//        System.out.println(file.getName());
        file=new File("/home/Solitary/pro_debily_v_textaku.txt");
        if(noURL){
            System.out.println("No URL given, so I am including a relative "
                    + "URL in the .zsync file - you must keep the file being"
                    + " served and the .zsync in the same public directory. "
                    + "Use -u test to get this same result without this warning.");
        }
        url="http://10.0.0.1/pro_debily_v_textaku.txt";
        HeaderMaker hm=new HeaderMaker(file,filename,url,blocksize);
        String header=hm.getHeader();
        try {

            BufferedWriter out = new BufferedWriter(new FileWriter(file.getPath()+".zsync"));
            header.replaceAll("\n", System.getProperty("line.separator"));
            out.write(header);
            out.close();
        }
        catch (IOException e){
            System.out.println("Header write exception");
        }
        FileOutputStream fos=new FileOutputStream(file+".zsync", true);
        Security.addProvider(new JarsyncProvider());
        Configuration c = new Configuration();
        c.strongSum = MessageDigest.getInstance("MD4");
        c.weakSum = new Rsum();
        c.blockLength = blocksize;
        c.strongSumLength = 16;
        Generator gen = new Generator(c);
        List list = new ArrayList((int)(file.length()/blocksize)+1);
        list = gen.generateSums(file);
        Iterator it = list.iterator();
        ChecksumPair p;
        byte[] b=new byte[4];
        while(it.hasNext()){
            p=(ChecksumPair)it.next();
            fos.write(intToBytes(p.getWeak()));
            fos.write(p.getStrong());
        }
    }

    /**
     * Converting integer weakSum into byte array that zsync can read
     * (hton byte order)
     * @param number weakSum in integer form
     * @return converted byte array readable by zsync (hton byte order)
     */
    public static byte[] intToBytes(int number){
        return new byte[] {
            (byte)((number << 16) >> 24),
            (byte)((number << 24) >> 24),
            (byte)( number >> 24),
            (byte)((number << 8) >> 24)};
    }
}
