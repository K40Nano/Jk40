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
public class MockUsb {

    void open() {
        System.out.println("Mock Usb Connected.");
    }

    void close() {
        System.out.println("Mock Usb Disconnected.");
    }

    void wait_for_ok() {
        System.out.println("Mock Usb: OKAY!");
    }

    void send_packet(CharSequence subSequence) {
        System.out.println("Mock Packst Sent:" + subSequence);
    }

    void wait_for_finish() {
        System.out.println("Mock Usb: Finished");
    }

}
