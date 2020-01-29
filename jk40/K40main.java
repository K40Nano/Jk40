package jk40;

public class K40main {

    public static void main(String[] args) {
        K40Device p = new K40Device();
        p.open();
        p.home();
        p.setPower(250);
        p.move_absolute(500,500);
        p.setSpeed(40.0);
        p.start_compact_mode();
        p.cut_relative(0,50);
        p.cut_relative(50,0);
        p.cut_relative(50,50);
        p.exit_compact_mode();
        p.execute();
        p.close();
        System.out.println("Main thread has ended.");
    }

}
