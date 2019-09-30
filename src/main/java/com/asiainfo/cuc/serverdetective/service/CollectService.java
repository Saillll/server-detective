package com.asiainfo.cuc.serverdetective.service;

import com.asiainfo.cuc.serverdetective.entity.Server;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Service
@Slf4j
public class CollectService {

    @Value("${test.local}")
    private boolean isTest;

    public List<Map<String,String>> collect(Server server){
        Map<String,String> baseinfo = baseinfo(server);
        Map<String,String> appinfo = appinfo(server);
        List<Map<String,String>> serverinfo = new ArrayList<>();
        serverinfo.add(baseinfo);
        serverinfo.add(appinfo);
        return serverinfo;
    }

    /**
     * 查询服务器的CPU 内存 磁盘 基本信息
     * @param server
     * @return
     */
    public Map<String,String> baseinfo(Server server){
        Session session = connect(server);
        Map<String,String> baseMap = new HashMap<String,String>();
        String cpu = cpu(session);
        String memory = memory(session);
        String disk = disk(session);
        baseMap.put("server","[服务器]" + server.getHostname()+ "-------------------------");
        baseMap.put("cpu",cpu);
        baseMap.put("memory",memory);
        baseMap.put("disk",disk);
        session.disconnect();
        return baseMap;
    }

    public Map<String,String> appinfo(Server server){
        Session session = connect(server);
        Map<String,String> baseMap = new HashMap<String,String>();
        List<String>  appList = new ArrayList<>();
        for(String origincmd:server.getApps()){
            String cmd = "ps -ef|grep " + origincmd;
            appList = execute(session,cmd);
            if(null != appList && appList.size()>0){
                int size = appList.size();
                for(String line:appList){
                    if(line.contains("bash -c")){
                        size--;
                    }
                }
                //除去grep本身命令，如果还有一个存在，那么就是服务正常
                if(size >= 2){
                    baseMap.put(origincmd,"----服务["+origincmd+"]，存活状态：存活");
                }else {
                    baseMap.put(origincmd,"----服务["+origincmd+"]，存活状态：死亡");
                }
            }
        }
        session.disconnect();
        return baseMap;
    }
    /**
     * 链接服务器，获取SESSION
     * @param server
     * @return
     */
    private Session connect(Server server){
        JSch jsch = new JSch();
        Session session = null;
        try {
            String host = "";
            boolean keyLogin = false;
            //判断是本地运行还是服务器运行
            String sysType = System.getProperties().getProperty("os.name");
            //web02没有免密 直接使用IP 密码登录
            if(!server.isKeylogin()){
                session = jsch.getSession(server.getUsername(), server.getIp(), server.getPort());
                session.setPassword(server.getPassword());
                Properties config = new Properties();
                config.setProperty("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
            }else{
                //如果是放在服务器上时hostname 而不是ip
                session = jsch.getSession(server.getUsername(), server.getHostname(), server.getPort());
                String privateKey = "/home/admin66/.ssh/id_rsa";
                jsch.addIdentity(privateKey);
                Properties config = new Properties();
                config.setProperty("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
            }
        } catch (Exception e) {
            log.info(server.getHostname() + "Error");
            e.printStackTrace();
        }finally {
            return session;
        }
    }

    /**
     * 执行命令,将查询到的结果按[行]放入List中
     * @param session
     * @param cmd
     * @return
     */
    private List<String> execute(Session session,String cmd){
        List<String> list = new ArrayList<>();
        if (session == null) {
            log.info("Session is null!");
            return list;
        }
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            InputStream inputStream = channel.getInputStream();
            channel.setCommand(cmd);
            channel.connect();
            list = readLine(inputStream);
            channel.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            return list;
        }
    }
    /**
     * 获取返回的每一行数据
     * @param inputStream
     * @return
     */
    private List<String> readLine(InputStream inputStream){
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
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
        }finally {
            return queryList;
        }
    }
    /**
     * CPU
     * @param session
     * @return
     */
    private String cpu(Session session){
        String cpu = "";
        //获取使用率
        //String cmd = "sar 1 1 | grep Average | awk '{print (100-$8)\"%\"}'";
        //英文版的系统使用Average,中文版的使用Average
        String cmd_en = "sar| grep Average | awk '{print (100-$8)\"%\"}'";
        String cmd_zh = "sar| grep 平均时间 | awk '{print (100-$8)\"%\"}'";
        String language = language(session);
        List<String>  cpuList = new ArrayList<>();
        if("zh".equals(language)){
            cpuList = execute(session,cmd_zh);
        }else{
            cpuList = execute(session,cmd_en);
        }
        if(null != cpuList && cpuList.size() > 0){
            cpu = cpuList.get(0);
        }
        cpu = "[CPU] 使用率" + cpu;
        return cpu;
    }
    /**
     * 内存
     * @param session
     * @return
     */
    private String memory(Session session){
        String memory = "";
        //获取使用率
        String cmd = "free -m | sed -n '2p' | awk '{print $3\"MB \"($3+$4)\"MB \"$3/($3+$4)*100\"%\"}'";
        List<String>  memList = execute(session,cmd);
        if(null != memList && memList.size() > 0){
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
     * @param session
     * @return
     */
    private String disk(Session session){
        String disk = "";
        //获取使用率
        //可以根据系统自定义语句
        String cmd = "df -h | grep home  | sed -n '1p' | awk '{print $2\" \" $3\" \" $4\" \" $5}'";
        String cmd_test = "df -h | grep home | sed -n '2p' | awk '{print $1\" \" $2\" \" $3\" \" $4}'";
        List<String>  memList = new ArrayList<>();
        if(true == isTest){
            memList = execute(session,cmd_test);
        }else{
            memList = execute(session,cmd);
        }
        if(null != memList && memList.size() > 0){
            disk = memList.get(0);
        }
        String[] detail = disk.split(" ");
        if(detail.length > 0){
            if(!"".equals(detail[0])){
                disk = "[磁盘] 总量" + detail[0] + ",使用" + detail[1] + "剩余"+detail[2] + ",使用率" + detail[3];
            }else{
                disk = "[磁盘] 暂无挂载磁盘信息";
            }
        }
        return disk;
    }
    /**
     * 获取服务器系统的语言
     * @param session
     * @return
     */
    private String language(Session session){
        String language = "en";
        //获取使用率
        String cmd = "cat /etc/sysconfig/i18n";
        List<String>  languageList = execute(session,cmd);
        for(String line:languageList){
            if(line.contains("zh_CN")){
                language = "zh";
            }else {
                language = "en";
            }
        }
        return language;
    }




}