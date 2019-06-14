package jk40;

/**
 *
 * @author Tat
 */

public class K40Encoder {
    
    K40Device device;
    
    public void open() {
        device = new K40Device();
        device.open();
    }
    
    public void close() {
        device.close();
        device = null;
        
    }
    
    public void setPower(double power) {
        device.setPower(power);
    }
    
    public void setSpeed(double mm_per_second) {
        device.setSpeed(mm_per_second);
    }
    
    public void moveToPoint(int x, int y) {    
        device.move_absolute(x,y);
    }
    
    public void lineToPoint(int x, int y) {    
        device.cut_absolute(x,y);
    }
    
    public void executeJob() {
        device.execute();
    }
    
}
