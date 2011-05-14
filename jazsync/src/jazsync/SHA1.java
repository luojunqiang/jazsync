package jazsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SHA1 {
    private String filename;
    private FileInputStream fis;
    private MessageDigest sha1;
    private StringBuilder sb;

    /**
     * Konstruktor SHA1
     * @param filename Nazev a cesta k souboru
     */
    public SHA1(String filename){
        this.filename=filename;
    }

    public SHA1(File file){
        this.filename=file.getName();
    }

    /**
     * Vypocet SHA1
     * @return String s kontrolnim souctem
     */
    public String SHA1sum(){
        try {
            sha1 = MessageDigest.getInstance("SHA1");
            fis = new FileInputStream(filename);
            byte[] dataBytes = new byte[1024];

            int read = 0;

            while ((read = fis.read(dataBytes)) != -1) {
              sha1.update(dataBytes, 0, read);
            }

            byte[] mdbytes = sha1.digest();

            //prevede byte do hex formatu
            sb = new StringBuilder("");
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
        } catch (IOException ex) {
            Logger.getLogger(SHA1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SHA1.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }
} 
