package jazsync;

import java.net.MalformedURLException;

public class FileMaker {

    /*
     * file URL - pokud je URL absolutni neni co resit
     * pokud je relativni, tak je to http://hostnameMetafile/file
     * pokud mame metafile na disku, a url je relativni je potreba dodat -u URL
     * pri spousteni jazsync (odkazuje pouze na hostname)
     */
    private MetaFileReader mfr;
    private HttpConnection http;
    public FileMaker(String[] args) throws MalformedURLException{
         mfr = new MetaFileReader(args);
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
        } else {                                    //relative URL path
            String host = mfr.getMetaFileSource().getHost().toString();
            String pathToFile = mfr.getMetaFileSource().getPath().toString();
            pathToFile=pathToFile.substring(0, pathToFile.lastIndexOf("/"));
            String url = ("http://"+host+pathToFile+"/"+mfr.getUrl());
            http = new HttpConnection(url);
        }
        http.openConnection();
        http.getFile(mfr.getLength(), mfr.getFilename());
        System.out.println("Target 100.0% complete.");
        SHA1 sha = new SHA1(mfr.getFilename());
        if(sha.SHA1sum().equals(mfr.getSha1())){
            System.out.println("verifying download...checksum matches OK");
            System.exit(0);
        }
    }
}