package jazsync;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.RoundingMode;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

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
    private int numBlocks;
    private SHA1 sha;
    private int missing;

    public FileMaker(String[] args) throws MalformedURLException, NoSuchAlgorithmException, FileNotFoundException, IOException {
         mfr = new MetaFileReader(args);
         hashtable = mfr.getHashtable();
         //hashtable.displayTable();
         fileMap = new long[mfr.getBlockCount()];
         Arrays.fill(fileMap, -1);
         fileOffset=0;
         if(mfr.FILE_FLAG==1) {
             System.out.println("Stahneme par bloku");
             checkSimilarity();
             fileMaker();
         } else if (mfr.FILE_FLAG==-1) {
             getWholeFile();
         } else {
             System.out.println("Problem?");
         }
         
    }

    private void openConnection() throws MalformedURLException{
        if(mfr.getUrl().startsWith("http://")){     //absolute URL path to file
            http = new HttpConnection(mfr.getUrl());
        } else {                                    //relative URL path to file
            http = new HttpConnection(urlParser(mfr.getMetaFileURL()));
        }
        http.openConnection();
        if(mfr.getAuthentication()){
            http.setAuthentication(mfr.getUsername(), mfr.getPassword());
        }
    }

    private void getWholeFile() throws MalformedURLException{
        openConnection();
        http.getFile(mfr.getLength(), mfr.getFilename());
        System.out.println("Target 100.0% complete.");
        sha = new SHA1(mfr.getFilename());
        if(sha.SHA1sum().equals(mfr.getSha1())){
            System.out.println("verifying download...checksum matches OK");
            System.out.println("used 0 local, fetched "+mfr.getLength());
            System.exit(0);
        }
        http.closeConnection();
    }
    
    private String urlParser(URL url){
        String host = url.getHost().toString();
        String pathToFile = url.getPath().toString();
        pathToFile=pathToFile.substring(0, pathToFile.lastIndexOf("/"));
        String newUrl = ("http://"+host+pathToFile+"/"+mfr.getUrl());
        return newUrl;
    }

    /**
     *
     * @throws IOException
     */
    private void fileMaker() throws IOException {
        int range;
        File newFile = new File(mfr.getFilename()+".part");
        if(newFile.exists()){
            newFile.delete();
        }
        newFile.createNewFile();
        ByteBuffer buffer = ByteBuffer.allocate(mfr.getBlocksize());
        FileChannel rChannel = new FileInputStream(mfr.getFilename()).getChannel();
        FileChannel wChannel = new FileOutputStream(newFile, true).getChannel();
        openConnection();
        http.getResponseHeader();

        for(int i=0;i<fileMap.length;i++){
            fileOffset = fileMap[i];
            if( fileOffset != -1 ){
                rChannel.read(buffer, fileOffset);
                buffer.flip();
                wChannel.write(buffer);
                buffer.clear();
            } else {
                openConnection();
                range=i*mfr.getBlocksize();
                http.setRangesRequest(new int[]{range,range+mfr.getBlocksize()-1});
                http.sendRequest();
                http.getResponseHeader();
                buffer.put(http.getResponseBody());
                buffer.flip();
                wChannel.write(buffer);
                buffer.clear();
            }
        }
        newFile.setLastModified(getMTime());
        sha = new SHA1(newFile);
        if(sha.SHA1sum().equals(mfr.getSha1())){
            System.out.println("verifying download...checksum matches OK");
            System.out.println("used "+(mfr.getLength()-(mfr.getBlocksize()*missing))+" "
                    + "local, fetched "+(mfr.getBlocksize()*missing));
            new File(mfr.getFilename()).renameTo(new File(mfr.getFilename()+".zs-old"));
            newFile.renameTo(new File(mfr.getFilename()));
            System.exit(0);
        }
    }

    /** Misto serioveho stahovani si projedem cely fileMap, dokud nenasbirame
     *  maximalne 20 chybejicich bloku, o ty si pozadame a budeme je drzet v pameti.
     *  Postupne je pak budeme z pameti pri spravnych prilezitostech psat do souboru,
     *  dokud vsechno z pameti nevypiseme. Nasledne muzeme s pruzkumem fileMap pokracovat.
     */
    private int[] rangeLookUp(int i){
        int[] ranges = null;

        return ranges;
    }

    /**
     * Parsing out date from metafile into long value
     * @return Time as long value in milliseconds passed since 1.1.1970
     */
    private long getMTime() {
        long mtime=0;
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z",Locale.US);
            Date date = sdf.parse(mfr.getMtime());
            mtime = date.getTime();
        } catch (ParseException e){
            System.out.println("Metafile is containing a wrong time format. "
                    + "Using today's date.");
            Date today = new Date();
            mtime=today.getTime();
        }
        return mtime;
    }

    /**
     *
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException
     */
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
        int mebiByte=1048576;

        if(mfr.getLength() < mebiByte && mfr.getBlocksize() < mfr.getLength()){
            fileBuffer = new byte[(int)mfr.getLength()];
        } else if (mfr.getBlocksize()>mfr.getLength() || mfr.getBlocksize() > mebiByte) {
            fileBuffer = new byte[mfr.getBlocksize()];
        } else {
            fileBuffer = new byte[mebiByte]; // 1 MiB
        }

        InputStream is = new FileInputStream(mfr.getFilename());
        File test = new File(mfr.getFilename());
        long fileLength = test.length();
        int n;
        byte newByte;
        boolean firstBlock=true;
        int len=fileBuffer.length;
        boolean end = false;
        long start = System.currentTimeMillis();
        System.out.print("Reading "+mfr.getFilename()+": ");
        System.out.print("|----------|");
        double a = 10;
        while (true) {
            n=is.read(fileBuffer,0,len);
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
                if(fileOffset+mfr.getBlocksize()>fileLength){
                    newByte=0;
                }

                /** Spocteme rolling checksum */
                weakSum = gen.generateRollSum(newByte);
                
                /**
                 * V pripade, ze nalezeneme v hash tabulce shodu k slabemu
                 * rolling checksumu, zacneme pocitat i silny (MD4) checksum,
                 * abychom se ujistili, ze neslo pouze o kolizi.
                 */
                if(hashLookUp(weakSum, null)){
                    if(fileOffset+mfr.getBlocksize()>fileLength){
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
                if((((double)fileOffset/(double)fileLength)*100)>=a){
                    progressBar(((double)fileOffset/(double)fileLength)*100);
                    a+=10;
                }
                if(fileOffset==fileLength){
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
        DecimalFormat df = new DecimalFormat("#.##");
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        df.setRoundingMode(RoundingMode.DOWN);
        System.out.println();
        System.out.println("Target "+df.format(completion())+"% complete.");
        long endT = System.currentTimeMillis();
//        System.out.println("Doba mapovani souboru: "+(double)(endT-start)/1000+"s");
//        System.out.println(Arrays.toString(fileMap));
        is.close();     
    }

    /**
     * Method is used to draw a progress bar of
     * how much we already read from file.
     * @param i How much data we already read (value in percents)
     */
    private void progressBar(double i){
        if(i>=10){
            for(int b=0;b<11;b++){
                System.out.print("\b");
            }
        }
        if(i>=10 && i<20){
            System.out.print("#---------|");
        } else if(i>=20 && i<30){
            System.out.print("##--------|");
        } else if(i>=30 && i<40){
            System.out.print("###-------|");
        } else if(i>=40 && i<50){
            System.out.print("####------|");
        } else if(i>=50 && i<60){
            System.out.print("#####-----|");
        } else if(i>=60 && i<70){
            System.out.print("######----|");
        } else if(i>=70 && i<80){
            System.out.print("#######---|");
        } else if(i>=80 && i<90){
            System.out.print("########--|");
        } else if(i>=90 & i<100){
            System.out.print("#########-|");
        } else if(i>=100){
            System.out.print("##########|");
        }
    }

    /**
     * Returns percentage value of how complete is our file
     * @return How many percent of file we have already
     */
    private double completion(){
        missing=0;
        for(int i=0;i<fileMap.length;i++){
            if(fileMap[i]==-1){
                missing++;
            }
        }
        return ((((double)fileMap.length-missing)/(double)fileMap.length)*100);
    }

    /**
     *
     * @param weakSum
     * @param strongSum
     * @return
     */
    private boolean hashLookUp(int weakSum, byte[] strongSum){
        ChecksumPair p;
        if(strongSum==null){
            p = new ChecksumPair(weakSum);
            Link link = hashtable.find(p);
            if(link!=null){
                return true;
            }
        } else {
            p = new ChecksumPair(weakSum, strongSum);
            Link link = hashtable.findMatch(p);
            int seq;
            if(link!=null){
               /** V pripade, ze nalezneme shodu si zapiseme do file mapy offset
                * bloku, kde muzeme dana data ziskat.
                * Nasledne po sobe muzeme tento zaznam z hash tabulky vymazat.
                */
                seq=link.getKey().getSequence();
                fileMap[seq]=fileOffset;
                hashtable.delete(new ChecksumPair(weakSum, strongSum,
                        mfr.getBlocksize()*seq,mfr.getBlocksize(),seq));
                return true;
            }
        }
        return false;
    }
}
