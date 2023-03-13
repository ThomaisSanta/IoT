package com.example.iot.controllers;


public class IoT {
    private String type ;
    private boolean send_data ;
    private float  slider_value;

    public IoT(String type) {
        this.type = type ;
    }

    public String getType() {return this.type ;}

    public void setType(String type) {this.type=type ;}

    public boolean getSendData() {return this.send_data; }

    public void setSendData(boolean send_data) {this.send_data = send_data ;}

    public float getSliderValue() {return this.slider_value ;}

    public void setSliderValue(float slider_value) {this.slider_value = slider_value ; }
}
