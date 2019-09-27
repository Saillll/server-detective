package com.asiainfo.cuc.serverdetective.service;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import com.asiainfo.cuc.serverdetective.entity.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 无用
 */
@Service
@Slf4j
public class ShellService {



    public boolean valid(Server server){
        Connection connection = new Connection(server.getIp());
        boolean connected = false;
        try {
            connection.connect();
            connected = connection.authenticateWithPassword(server.getUsername(), server.getPassword());
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            connection.close();
            return connected;
        }
    }

    public Map<String,String> baseinfo(Server server){
        Map<String,String> baseMap = new HashMap<String,String>();
        String cpu = cpu(server);
        String memory = memory(server);
        String disk = disk(server);
        baseMap.put("cpu",cpu);
        baseMap.put("memory",memory);
        baseMap.put("disk",disk);
        return baseMap;
    }
    public List<String> exec(Server server,String cmds){
        InputStream in = null;
        Connection conn = null;
        boolean connected = false;
        List<String> queryList = new ArrayList<>();
        try {
            conn = new Connection(server.getIp());
            conn.connect();
            connected = conn.authenticateWithPassword(server.getUsername(), server.getPassword());
            if(connected){
                Session session = conn.openSession();
                session.execCommand(cmds);
                in = session.getStdout();
                queryList = this.readLine(in, StandardCharsets.UTF_8);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if( null != conn){
                conn.close();
            }
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return queryList;
    }


    /**
     * 获取返回的每一行数据
     * @param inputStream
     * @param charset
     * @return
     */
    public List<String> readLine(InputStream inputStream, Charset charset){
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer sb = new StringBuffer();
        List<String> queryList = new ArrayList<>();
        String line = "";
        int count = 0;
        try {
            while ((line = reader.readLine())!=null){
                count++;
                queryList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queryList;
    }
    public boolean alive(List<String> queryList){
        boolean alive = false;
        if(queryList !=null && queryList.size() >=2 ){
            alive = true;
        }
        return alive;
    }

    /**
     * cpu
     * @param server
     * @return
     */
    public String cpu(Server server){
        String cpu = "";
        //获取使用率
        String cmd = "sar 1 1 | grep Average | awk '{print (100-$8)\"%\"}'";
        List<String>  cpuList = exec(server,cmd);
        if(null != cmd && cpuList.size() > 0){
            cpu = cpuList.get(0);
        }
        cpu = "[CPU] " + cpu;
        return cpu;
    }

    /**
     * 内存
     * @param server
     * @return
     */
    public String memory(Server server){
        String memory = "";
        //获取使用率
        String cmd = "free -m | sed -n '2p' | awk '{print $3\"MB \"($3+$4)\"MB \"$3/($3+$4)*100\"%\"}'";
        List<String>  memList = exec(server,cmd);
        if(null != cmd && memList.size() > 0){
            memory = memList.get(0);
        }
        String[] detail = memory.split(" ");
        if(detail.length > 0){
            memory = "[内存] 总" + detail[1] + ",使用" + detail[0] + ",使用率" + detail[2];
        }
        return memory;
    }

    /**
     * 磁盘
     * @param server
     * @return
     */
    public String disk(Server server){
        String disk = "";
        //获取使用率
        String cmd = "df -h | grep home  | sed -n '1p' | awk '{print $2\" \" $3\" \" $4\" \" $5}'";
        List<String>  memList = exec(server,cmd);
        if(null != cmd && memList.size() > 0){
            disk = memList.get(0);
        }
        String[] detail = disk.split(" ");
        if(detail.length > 0){
            disk = "[磁盘] 总量" + detail[0] + ",使用" + detail[1] + "剩余"+detail[2] + ",使用率" + detail[3];
        }
        return disk;
    }

    public StringBuffer readAll(InputStream in, Charset charset) throws FileNotFoundException {
        byte[] buf = new byte[1024];
        StringBuffer sb = new StringBuffer();
        try {
            int length;
            while ((length = in.read(buf)) != -1) {
                sb.append(new String(buf, 0, length));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb;
    }

}
