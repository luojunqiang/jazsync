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
    private long fileOffset;
    private long[] fileMap;

    public FileMaker(String[] args) throws MalformedURLException, NoSuchAlgorithmException, FileNotFoundException, IOException{
         mfr = new MetaFileReader(args);
         hashtable = mfr.getHashtable();
         fileMap = new long[mfr.getBlockCount()];
         Arrays.fill(fileMap, -1);
         fileOffset=0;
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

        int weakSum;
        byte[] strongSum;
        byte[] backBuffer=new byte[mfr.getBlocksize()];
        byte[] blockBuffer=new byte[mfr.getBlocksize()];
        byte[] fileBuffer;
        
        fileBuffer = new byte[150];
        if(mfr.getLength() < 1048576 && mfr.getBlocksize() < mfr.getLength()){
            fileBuffer = new byte[(int)mfr.getLength()];
        } else if (mfr.getBlocksize()>mfr.getLength()) {
            fileBuffer = new byte[mfr.getBlocksize()];
        } else {
            fileBuffer = new byte[1048576];
        }

        InputStream is = new FileInputStream(mfr.getFilename());
        File test = new File(mfr.getFilename());
        int n=0;
        byte newByte;
        boolean firstBlock=true;
        int len=fileBuffer.length;
        boolean end = false;
        long start = System.currentTimeMillis();
        
        while (true) {
            n=is.read(fileBuffer,0,len);
            //System.out.println("*********** Bytes read: "+n+" ***********");
            /** Inicializujeme rolling checksum tim, ze spocteme kontrolni soucet
             *  v prvni offsetu
             */
            if(firstBlock){
                weakSum = gen.generateWeakSum(fileBuffer, 0);
                bufferOffset=mfr.getBlocksize();
                //System.out.println("0. "+weakSum);
                if(hashLookUp(weakSum, null)){
                    strongSum = gen.generateStrongSum(fileBuffer, 0, mfr.getBlocksize());
                    hashLookUp(weakSum, strongSum);
                }
                fileOffset++;
                firstBlock=false;
            }
            
            //zde muzeme zapocit rolling checksum
            for( ; bufferOffset<fileBuffer.length ; bufferOffset++){
                
                newByte = fileBuffer[bufferOffset];
                if(fileOffset+mfr.getBlocksize()>test.length()){
                    newByte=0;
                }

                /** Spocteme rolling checksum */
                weakSum = gen.generateRollSum(newByte);
                
                /**
                 * V pripade, ze nalezeneme v hash tabulce shodu k slabemu
                 * rolling checksumu, zacneme pocitat i silny (MD4) checksum,
                 * abychom se ujistili, ze neslo pouze o kolizi.
                 */
                //System.out.println(fileOffset+" "+bufferOffset+". "+weakSum);
                if(hashLookUp(weakSum, null)){
                    

                    if(fileOffset+mfr.getBlocksize()>test.length()){
                        if(n>0){
                            Arrays.fill(fileBuffer, n, fileBuffer.length, (byte)0);
                        } else {
                            int offset=fileBuffer.length-mfr.getBlocksize()+bufferOffset+1;
                            System.arraycopy(fileBuffer, offset, blockBuffer, 0, fileBuffer.length-offset);
                            Arrays.fill(blockBuffer, fileBuffer.length-offset, blockBuffer.length, (byte)0);
                        }
                    }

                    if((bufferOffset-mfr.getBlocksize()+1)<0){
                        if(n>0){
                            System.arraycopy(backBuffer,
                                    backBuffer.length+bufferOffset-mfr.getBlocksize()+1,
                                    blockBuffer, 0, mfr.getBlocksize()-bufferOffset-1);

                            System.arraycopy(fileBuffer, 0, blockBuffer,
                                    mfr.getBlocksize()-bufferOffset-1, bufferOffset+1);
                        }
                        strongSum = gen.generateStrongSum(blockBuffer,
                                0, mfr.getBlocksize() );
                        hashLookUp(weakSum, strongSum);
                    } else {
                        strongSum = gen.generateStrongSum(fileBuffer,
                                bufferOffset-mfr.getBlocksize()+1,
                                mfr.getBlocksize() );
                        hashLookUp(weakSum, strongSum);
                    }
                    

                }

                fileOffset++;
                if(fileOffset==test.length()){
                    end=true;
                    break;
                }
            }
            System.arraycopy(fileBuffer, fileBuffer.length-mfr.getBlocksize(),
                    backBuffer, 0, mfr.getBlocksize());
            bufferOffset=0;
            if(end){
                break;
            }
        }

        long endT = System.currentTimeMillis();
        System.out.println("Doba mapovani souboru: "+(double)(endT-start)/1000+"s");
        System.out.println(Arrays.toString(fileMap));
        is.close();     
    }

    private boolean hashLookUp(int weakSum, byte[] strongSum){
        ChecksumPair p;
        if(strongSum==null){
            p = new ChecksumPair(weakSum);
            Link link = hashtable.find(p);
            if(link!=null){
//                System.out.println("Shoda slabeho souctu na ");
//                link.displayLink();
//                System.out.println("\n");
                return true;
            }
        } else {
            p = new ChecksumPair(weakSum, strongSum);
            System.out.println(p.getStrongHex()); 
            Link link = hashtable.findMatch(p);
            if(link!=null){
                fileMap[link.getKey().getSequence()]=fileOffset;
                return true;
            }
        }
        return false;
    }

}
