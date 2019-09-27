package com.asiainfo.cuc.serverdetective.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Server {

    private String ip;
    private int port;
    private String hostname;
    private String username;
    private String password;
    private List<String> apps;

    public Server() {
    }

    public Server(String ip, int port,String username, String password) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public Server(String hostname,int port) {
        this.port = port;
        this.hostname = hostname;
    }

    public Server(String ip, int port, String hostname, String username, String password) {
        this.ip = ip;
        this.port = port;
        this.hostname = hostname;
        this.username = username;
        this.password = password;
    }

}
