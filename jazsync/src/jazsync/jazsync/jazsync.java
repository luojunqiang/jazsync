package jazsync.jazsync;

public class jazsync {

    public static void main(String[] args) {
        args=new String[1];
        args[0]="http://fusal.wz.cz/jazsync.txt.zsync";
//        args[0]="-k";
//        args[1]="TEST.TEST";
//        args[2]="http://127.0.0.1/jazsync.txt.orig";
        args[0]="/home/Solitary/jazsync.txt.orig";
        //args[0]="/home/Solitary/Fedora-15-Beta-i686-Live-Desktop.iso.zsync";
//        args[0]="-A";
//        args[1]="127.0.0.2=admin:admin";
//        args[2]="http://127.0.0.1/munin/kolibri.iso.zsync";
        FileMaker fm = new FileMaker(args);
    }

}