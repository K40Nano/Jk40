/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jk40;

/**
 *
 * @author Tat
 */
public class MockUsb implements BaseUsb {

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
        }
    }

    @Override
    public void open() {
        sleep(1000);
        System.out.println("Mock Usb Connected.");
    }

    @Override
    public void close() {
        sleep(1000);
        System.out.println("Mock Usb Disconnected.");
    }

    @Override
    public void wait_for_ok() {
        sleep(20);
        System.out.println("Mock Usb: OKAY!");
    }

    @Override
    public void send_packet(CharSequence subSequence) {
        sleep(100);
        System.out.println("Mock Packst Sent:" + subSequence);
    }

    @Override
    public void wait_for_finish() {
        sleep(4000);
        System.out.println("Mock Usb: Finished");
    }

}
