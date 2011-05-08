package jazsync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;

public class jazsync {

    public static void main(String[] args) throws FileNotFoundException, IOException, MalformedURLException, NoSuchAlgorithmException {
        args=new String[1];
        args[0]="http://fusal.wz.cz/jazsync.txt.zsync";
        args[0]="/home/Solitary/jazsync.txt.zsync";
        args[0]="/home/Solitary/Fedora-15-Beta-i686-Live-Desktop.iso.zsync";
        FileMaker fm = new FileMaker(args);
    }

}
