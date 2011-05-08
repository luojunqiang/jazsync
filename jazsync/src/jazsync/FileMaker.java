package jazsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import org.metastatic.rsync.ChecksumPair;
import org.metastatic.rsync.Configuration;
import org.metastatic.rsync.Generator;
import org.metastatic.rsync.JarsyncProvider;

public class FileMaker {

    /*
     * file URL - pokud je URL absolutni neni co resit
     * pokud je relativni, tak je to http://hostnameMetafile/file
     * pokud mame metafile na disku, a url je relativni je potreba dodat -u URL
     * pri spousteni jazsync (odkazuje pouze na hostname)
     */
    private MetaFileReader mfr;
    private HttpConnection http;
    private ChainingHash hashtable;
    private Configuration config;
    private int bufferOffset;

    public FileMaker(String[] args) throws MalformedURLException, NoSuchAlgorithmException, FileNotFoundException, IOException{
         mfr = new MetaFileReader(args);
         hashtable = mfr.getHashtable();
         checkSimilarity();
         if(mfr.FILE_FLAG==1) {
             System.out.println("Stahneme par bloku");

         } else if (mfr.FILE_FLAG==-1) {
             getWholeFile();
         } else {
             System.out.println("Problem?");
         }
         
    }

    private void getWholeFile() throws MalformedURLException{
        if(mfr.getUrl().startsWith("http://")){     //absolute URL path to file
            http = new HttpConnection(mfr.getUrl());
        } else {                                    //relative URL path to file
            http = new HttpConnection(urlParser(mfr.getMetaFileURL()));
        }
        http.openConnection();
        http.getFile(mfr.getLength(), mfr.getFilename());
        System.out.println("Target 100.0% complete.");
        SHA1 sha = new SHA1(mfr.getFilename());
        if(sha.SHA1sum().equals(mfr.getSha1())){
            System.out.println("verifying download...checksum matches OK");
            System.out.println("used 0 local, fetched "+mfr.getLength());
            System.exit(0);
        }
    }
    
    private String urlParser(URL url){
        String host = url.getHost().toString();
        String pathToFile = url.getPath().toString();
        pathToFile=pathToFile.substring(0, pathToFile.lastIndexOf("/"));
        String newUrl = ("http://"+host+pathToFile+"/"+mfr.getUrl());
        return newUrl;
    }

    private void fileMaker(){
        //setLastModified (mf_MTime)
    }

    private void checkSimilarity() throws NoSuchAlgorithmException, FileNotFoundException, IOException{
        Security.addProvider(new JarsyncProvider());
        config = new Configuration();
        config.strongSum = MessageDigest.getInstance("MD4");
        config.weakSum = new Rsum();
        config.blockLength = mfr.getBlocksize();
        config.strongSumLength = mfr.getChecksumBytes();
        Generator gen = new Generator(config);
        int[] fileMap = new int[mfr.getBlockCount()];
        ChecksumPair p;
        int weakSum;
        byte[] fileBuffer;
        fileBuffer = new byte[256];
        if(mfr.getLength() <= 1048576){
            fileBuffer = new byte[(int)mfr.getLength()+mfr.getBlocksize()];
        } else {
            fileBuffer = new byte[1048576];
        }

        InputStream is = new FileInputStream(mfr.getFilename());
        int n=0;
        int offset=0;
        byte newByte;
        boolean firstBlock=true;
        long fileOffset=1;
        int len=fileBuffer.length;
        boolean end = false;
        int y=mfr.getBlocksize()+1;
        long start = System.currentTimeMillis();
        while (true) {
            n=is.read(fileBuffer,offset,len-offset);
            //System.out.println("*********** Bytes read: "+n+" ***********");
            if(n==-1){
                break;
            }
            

            if(firstBlock){
                weakSum = gen.generateWeakSum(fileBuffer, 0);
                bufferOffset=mfr.getBlocksize();
                System.out.println("0. "+weakSum);
                if(hashLookUp(weakSum, null)){
                    
                }
                firstBlock=false;
            }

            for( ; bufferOffset<fileBuffer.length ; bufferOffset++){
                newByte = fileBuffer[bufferOffset];
                if(fileOffset+mfr.getBlocksize()>mfr.getLength()){
                    newByte=0;
                }
                /** Spocteme rolling checksum */
                weakSum = gen.generateRollSum(newByte);
                
                /** Nahledneme do hashtablu */
                //System.out.println(fileOffset+" "+bufferOffset+". "+weakSum);
                hashLookUp(weakSum, null);
                
//                p = new ChecksumPair(weakSum);
//                Link link = hashtable.find(p);
//                if(link!=null){
//
//                    System.out.println(fileOffset+" "+bufferOffset+". "+weakSum);
//                    link.displayLink();
//                    System.out.println("\n");
//                }
                



                fileOffset++;
                if(fileOffset==mfr.getLength()){
                    end=true;
                    break;
                }
            }

            if(end){
                break;
            }

            bufferOffset=0;
            
            for(int x=0;y<fileBuffer.length;x++){
                fileBuffer[x]=fileBuffer[y];
                y++;
            }

        }
        long endT = System.currentTimeMillis();
        System.out.println("Doba mapovani souboru: "+(double)(endT-start)/1000+"s");
        is.close();     
    }

    private boolean hashLookUp(int weakSum, byte[] strongSum){
        ChecksumPair p;
        if(strongSum==null){
            p = new ChecksumPair(weakSum);
            Link link = hashtable.find(p);
            if(link!=null){
                link.displayLink();
                System.out.println("\n");
                return true;
            }
        } else {
            p = new ChecksumPair(weakSum, strongSum);
            Link link = hashtable.findMatch(p);
            if(link!=null){
                link.displayLink();
                System.out.println("\n");
                return true;
            }
        }
        return false;
    }

}
