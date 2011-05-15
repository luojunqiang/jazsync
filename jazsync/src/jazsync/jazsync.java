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
//        args[0]="/home/Solitary/Fedora-15-Beta-i686-Live-Desktop.iso.zsync";
//        args[0]="-A";
//        args[1]="127.0.0.2=admin:admin";
//        args[2]="http://127.0.0.1/munin/kolibri.iso.zsync";
        FileMaker fm = new FileMaker(args);
    }

}

/*
 90 195 161 115 116 117 112 99 101 32 98 108 111 107 117 32 51 10 85 73 68 9
 75 97 110 100 105 100 195 161 116 9 80 114 111 9 80 114 111 116 105 9 82 111
 122 100 195 173 108 9 86 195 189 115 108 101 100 101 107 10 49 52 55 48 48 9
 77 105 99 104 97 101 108 97 32 197 160 111 117 114 107 111 118 195 161 32 9 52
 51 32 9 49 32 9 52 50 10 10 10 90 195 161 115 116 117 112 99 101 32 98 108 111
 107 117 32 53 10 85 73 68 9 75 97 110 100 105 100 195 161
 */