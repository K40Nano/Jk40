package jk40;

public class K40main {

    public static void main(String[] args) {
        K40Device p = new K40Device();
        p.open();
        //p.move_absolute(1, 1);
        p.cut_absolute(50, 25);
        p.cut_absolute(100, 100);
        p.cut_absolute(50, 50);
        p.move_absolute(0, 0);
        p.move_absolute(50, 50);
        p.move_absolute(0, 0);
        p.cut_absolute(100, 100);
        p.execute();
        p.close();
        System.out.println("Main thread has ended.");
    }

}
