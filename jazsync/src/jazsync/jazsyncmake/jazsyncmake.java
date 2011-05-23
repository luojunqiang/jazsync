package jazsync.jazsyncmake;

public class jazsyncmake {

    public static void main(String[] args) {
        args=new String[5];
        args[0]="-b";
        args[1]="8192";
//        args[2]="-f";
//        args[3]="jazsync.txt";
//        args[4]="-u";
//        args[5]="http://127.0.0.1/jazsync.txt";
//        args[6]="/home/Solitary/jazsync.txt";
        args[0]="-o";
        args[1]="ubuntu.zsync-8192";
        args[2]="/var/www/html/ubuntu-11.04-desktop-i386.iso";
        args[3]="-u";
        args[4]="http://127.0.0.1/ubuntu-11.04-desktop-i386.iso";

//        args[0]="-b";
//        args[1]="128";
//        args[2]="-f";
//        args[3]="kolibri.iso";
//        args[4]="-u";
//        args[5]="http://127.0.0.1/kolibri.iso";
//        args[6]="/home/Solitary/kolibri.iso";
//        args[7]="-o";
//        args[8]="/home/Solitary/kolibri.iso.zsync";
//        args[0]="-b";
//        args[1]="2048";
//        args[2]="-f";
//        args[3]="Fedora-15-Beta-i686-Live-Desktop.iso";
//        args[4]="-u";
//        args[5]="http://ftp.linux.cz/pub/linux/fedora/linux/releases/test/15-Beta/Live/i686/Fedora-15-Beta-i686-Live-Desktop.iso";
//        args[6]="/home/Solitary/Fedora-15-Beta-i686-Live-Desktop.iso";
//        args[7]="-o";
//        args[8]="/home/Solitary/Fedora-15-Beta-i686-Live-Desktop.iso.zsync";
        long start=System.currentTimeMillis();
        MetaFileMaker mfm=new MetaFileMaker(args);
        long end=System.currentTimeMillis();
        System.out.println((double)(end-start)/1000+"s");
    }
}
