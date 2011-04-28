package jazsync;

public class jazsyncmake {

    
    public static void main(String[] args) {
        args=new String[9];
        args[0]="-b";
        args[1]="128";
        args[2]="-f";
        args[3]="jazsync.txt";
        args[4]="-u";
        args[5]="http://fusal.wz.cz/jazsync.txt";
        args[6]="/home/Solitary/jazsync.txt";
        args[7]="-o";
        args[8]="/home/Solitary/jazsync.txt.zsync";
//        args[0]="-b";
//        args[1]="2048";
//        args[2]="-f";
//        args[3]="Fedora-15-Beta-i686-Live-Desktop.iso";
//        args[4]="-u";
//        args[5]="http://ftp.linux.cz/pub/linux/fedora/linux/releases/test/15-Beta/Live/i686/Fedora-15-Beta-i686-Live-Desktop.iso";
//        args[6]="/home/Solitary/Fedora-15-Beta-i686-Live-Desktop.iso";
//        args[7]="-o";
//        args[8]="/home/Solitary/Fedora-15-Beta-i686-Live-Desktop.iso.zsync";

        MetaFileMaker mfm=new MetaFileMaker(args);
    }
}
