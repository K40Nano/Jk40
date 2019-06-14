package jk40;

public class K40main {

    public static void main(String[] args) {
        K40Encoder p = new K40Encoder();
        p.open();
        p.lineToPoint(0,512);
        p.lineToPoint(512,512);
        p.lineToPoint(512, 0);
        p.lineToPoint(0, 0);
        p.executeJob();
        p.lineToPoint(50, 50);
        p.moveToPoint(100, 100);
        p.executeJob();
        p.close();
        System.out.println("Main thread has ended.");
    }

}
