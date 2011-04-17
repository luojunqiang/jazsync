package jazsync;

public class jazsyncmake {

    
    public static void main(String[] args) {
        args=new String[9];

        args[0]="-b";
        args[1]="128";
        args[2]="-f";
        args[3]="pro_debily_v_textaku.txt";
        args[4]="-u";
        args[5]="http://fusal.wz.cz/pro_debily_v_textaku.txt";
        args[6]="/home/Solitary/pro_debily_v_textaku.txt";
        args[7]="-o";
        args[8]="/home/Solitary/pro_debily_v_textaku.txt.zsync";

        MetaFileMaker mfm=new MetaFileMaker(args);
    }
}
