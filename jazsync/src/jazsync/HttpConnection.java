package jazsync;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.BASE64Encoder;

public class HttpConnection {
    private String rangeRequest;
    private String encoding;
    private String username;
    private String password;
    private HttpURLConnection connection;
    private URL address;
    private String boundary;
    private long contLen;

    public HttpConnection(String url) {
        try {
            //address = new URL("http://fusal.wz.cz/jazsync.txt.zsync");
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
            connection.setRequestMethod("GET");
            if(username!=null && password!=null){
                encoding = new BASE64Encoder().encode((username+":"+password).getBytes());
                connection.setRequestProperty("Authorization", "Basic "+encoding);
            } else if (rangeRequest!=null){
                connection.setRequestProperty("Range", "bytes="+rangeRequest);
            } 
        } catch (MalformedURLException e) {
            failed(address.toString());
        } catch (IOException e) {
           failed(address.toString());
            //e.printStackTrace();
        }
    }

    /**
     *
     * @param ranges Array of integers one by one symbolizing start/end
     * of every byte range
     */
    public void setRangesRequest(int[] ranges){
        String var="";
        for(int i=0;i<ranges.length-1;i++){
            var+=String.valueOf(ranges[i])+"-"+String.valueOf(ranges[i+1]);
            var+=",";
            i++;
        }
        rangeRequest=var.substring(0, var.length()-1);
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

    public void getResponseBody(){
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
        } catch (IOException e){
            failed(address.toString());
        }
    }

    public byte[] getMetafile(){
        byte[] bytes = new byte[(int)contLen];
        InputStreamReader in;
        try {
            in = new InputStreamReader(connection.getInputStream());
            for (int i = 0; i < bytes.length; i++) {
              int b = in.read( );
              if (b  == -1) break;
              bytes[i] = (byte) b;
                System.out.print((char)bytes[i]);
            }
        } catch (IOException ex) {
            failed(address.toString());
        }
        return bytes;
    }

    public void getFile(){

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
                    getBoundary(key,o.toString());
                    getLength(key,o.toString());
                }
                header+="\n";
            }
        return header;
    }

    private void getLength(String key, String values){
        if(key!=null && key.equals("Content-Length")==true){
            contLen=Integer.valueOf(values);
        }
    }

    /**
     * Gets boundary sequence from response header for indetification of range
     * boundaries
     * @param key Key name of header line
     * @param values Values of key header line
     */
    private void getBoundary(String key, String values){
        if(getHttpStatusCode()==206 && key!=null && key.equals("Content-Type")==true){
            int index=values.indexOf("boundary");
            if(index!=-1){
                boundary=values.substring(index+"boundary=".length());
            }
        }
    }

    public void rangesParser(){

    }

    public void closeConnection(){
        connection.disconnect();
    }

    private void failed(String url){
        System.out.println("Failed on url "+url);
        System.out.println("Could not read file from URL "+url);
    }

}
