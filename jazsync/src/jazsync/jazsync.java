package jazsync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

public class jazsync {

    public static void main(String[] args) throws FileNotFoundException, IOException, MalformedURLException, NoSuchAlgorithmException {
        args=new String[1];
        //args[0]="http://fusal.wz.cz/jazsync.txt.zsync";
        args[0]="/home/Solitary/jazsync.txt.zsync";
        //args[0]="/home/Solitary/Fedora-15-Beta-i686-Live-Desktop.iso.zsync";
        //args[0]="http://127.0.0.1/jazsync.txt.zsync";
        FileMaker fm = new FileMaker(args);
    }

}
