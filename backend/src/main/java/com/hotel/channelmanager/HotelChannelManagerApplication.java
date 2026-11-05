package com.hotel.channelmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotelChannelManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelChannelManagerApplication.class, args);
    }
}

