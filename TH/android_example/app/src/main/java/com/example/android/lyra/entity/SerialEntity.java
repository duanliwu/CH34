package com.example.android.lyra.entity;

import android.hardware.usb.UsbDevice;

public class SerialEntity {
    private UsbDevice usbDevice;
    private int serialNumber;



    public SerialEntity(UsbDevice usbDevice, int serialNumber) {
        this.usbDevice = usbDevice;
        this.serialNumber = serialNumber;
    }

    public UsbDevice getUsbDevice() {
        return usbDevice;
    }

    public int getSerialNumber() {
        return serialNumber;
    }
}
