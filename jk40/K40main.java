package jk40;

public class K40main {

    public static void main(String[] args) {
        K40Device p = new K40Device();
        p.open();
        p.home();
        p.execute();
        p.close();
        System.out.println("Main thread has ended.");
    }

}
