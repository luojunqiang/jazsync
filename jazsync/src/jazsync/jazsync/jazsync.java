package jazsync.jazsync;

public class jazsync {

    public static void main(String[] args) {
        args=new String[1];
        args[0]="/home/Solitary/jazsync.txt.zsync";
//        args[0]="http://fusal.wz.cz/jazsync.txt.zsync";
//        args[0]="-i";
//        args[1]="jazsync";
//        //args[2]="http://127.0.0.1/jazsync.txt.orig";
//        args[2]="-u";
//        args[3]="http://127.0.0.1/jazsync.txt.orig";
//        args[4]="/home/Solitary/jazsync.txt.orig";
        args[0]="/home/Solitary/ubuntu-11.04-desktop-i386.iso.zsync";
//        args[0]="-A";
//        args[1]="127.0.0.2=admin:admin";
//        args[2]="http://127.0.0.1/munin/kolibri.iso.zsync";
        long start=System.currentTimeMillis();
        FileMaker fm = new FileMaker(args);
        long end=System.currentTimeMillis();
        System.out.println((double)(end-start)/1000+"s");
    }

}