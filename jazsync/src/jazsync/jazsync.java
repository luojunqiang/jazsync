package jazsync;

import java.io.FileNotFoundException;
import java.io.IOException;

public class jazsync {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        args=new String[1];
        args[0]="/home/Solitary/jazsync.txt.zsync";
        MetaFileReader mfr=new MetaFileReader(args);

    }

}
