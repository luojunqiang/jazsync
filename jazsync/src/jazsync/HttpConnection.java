package jazsync;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpConnection {
    private String rangeRequest;
    private String username;
    private String password;
    private HttpURLConnection connection;
    private URL address;
    private String boundary;
    private byte[] boundaryBytes;
    private long contLen;

    public HttpConnection(String url) {
        try {
            address = new URL(url);
        } catch (MalformedURLException e) {
            failed(url);
        }
    }

    private int getHttpStatusCode(){
        int code=0;
        try {
            code = connection.getResponseCode();
        } catch (IOException e) {
            failed(address.toString());
        }
        return code;
    }

    /**
     *
     */
    public void openConnection(){
        try {
            connection = (HttpURLConnection)address.openConnection();
        } catch (MalformedURLException e) {
            failed(address.toString());
        } catch (IOException e) {
            failed(address.toString());
        }
    }

    public void sendRequest(){
        try{
            connection.setRequestMethod("GET");
            if(username!=null && password!=null){
                String encoding = Base64Coder.encodeLines((username+":"+password).getBytes());
                connection.setRequestProperty("Authorization", 
                        "Basic "+encoding.substring(0, encoding.length()-1));
            }
            if (rangeRequest!=null){
                connection.setRequestProperty("Range", "bytes="+rangeRequest);
            }
        } catch (IOException e) {
           failed(address.toString());
        }
    }

    /**
     * Method used to initialize range for http request
     * @param ranges ArrayList of DataRange objects containing block ranges
     */
    public void setRangesRequest(ArrayList<DataRange> ranges){
        StringBuilder sb = new StringBuilder();
        for(DataRange d : ranges){
            sb.append(d.getRange()).append(",");
        }
        sb.delete(sb.length()-1,sb.length());
        System.out.println(sb.toString());
        rangeRequest=sb.toString();
    }

    /**
     *
     * @param username
     * @param password
     */
    public void setAuthentication(String username, String password){
        this.username=username;
        this.password=password;
    }

    private boolean compareBytes(byte[] src, int srcOff, byte[] bound){
        int j = srcOff;
        for(int i=0; i<bound.length; i++){
            if(src[j]!=bound[i]){
                return false;
            }
            j++;
        }
        return true;
    }

    public byte[] getResponseBody(){
        byte[] bytes = new byte[(int)contLen];
        try {
            InputStream in = connection.getInputStream();
            for (int i = 0; i < bytes.length; i++) {
                bytes[i]=(byte)in.read();
                System.out.print((char)(bytes[i]));//+" ");

            }
        } catch (IOException e) {
            failed(address.toString());
        }
        byte[] rangeBytes = new byte[(int)contLen];
        for(int i = 0; i <bytes.length;i++){
            if(bytes[i]==45 && bytes[i+1]==45){
                if(compareBytes(bytes, i+2, boundaryBytes)){

                }
            }
        }
        return bytes;
    }

    public void getFile(Long length, String filename){
        try {
            FileOutputStream fos=new FileOutputStream(filename, true);
            InputStream in = connection.getInputStream();
            for(int i=0;i<length;i++){
                fos.write((byte)in.read());
            }
        } catch (IOException e) {
            failed(address.toString());
        }
    }
    
    public String getResponseHeader(){
        String header="";
        Map responseHeader = connection.getHeaderFields();
            for (Iterator iterator = responseHeader.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                if(key!=null) {
                    header+=key + " = ";
                }
                List values = (List) responseHeader.get(key);
                for (int i = 0; i < values.size(); i++) {
                    Object o = values.get(i);
                    header+=o.toString();
                    parseBoundary(key,o.toString());
                    parseLength(key,o.toString());
                }
                header+="\n";
            }
        return header;
    }

    /**
     * Method parse
     * @param key
     * @param values
     */
    private void parseLength(String key, String values){
        if(key!=null && key.equals("Content-Length")==true){
            contLen=Integer.valueOf(values);
        }
    }

    /**
     * Gets boundary sequence from response header for identificating the range
     * boundaries
     * @param key Key name of header line
     * @param values Values of key header line
     */
    private void parseBoundary(String key, String values){
        if(getHttpStatusCode()==206 && key!=null && key.equals("Content-Type")==true){
            int index=values.indexOf("boundary");
            if(index!=-1){
                boundary=values.substring(index+"boundary=".length());
                boundaryBytes=boundary.getBytes();
            }
        }
    }

    public void closeConnection(){
        connection.disconnect();
    }

    private void failed(String url){
        System.out.println("Failed on url "+url);
        System.out.println("Could not read file from URL "+url);
    }

}
