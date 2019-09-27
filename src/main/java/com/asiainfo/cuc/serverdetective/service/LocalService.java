package com.asiainfo.cuc.serverdetective.service;

import com.asiainfo.cuc.serverdetective.entity.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class LocalService {

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
        Map<String,String> baseMap = new HashMap<String,String>();
        String cpu = cpu();
        String memory = memory();
        String disk = disk();
        baseMap.put("server","[服务器]" + server.getHostname()+ "-------------------------");
        baseMap.put("cpu",cpu);
        baseMap.put("memory",memory);
        baseMap.put("disk",disk);
        return baseMap;
    }

    public Map<String,String> appinfo(Server server){
        Map<String,String> baseMap = new HashMap<String,String>();
        List<String>  appList = new ArrayList<>();
        for(String origincmd:server.getApps()){
            String cmd = "ps -ef|grep " + origincmd;
            appList = execute(cmd);
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
        return baseMap;
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
     * @return
     */
    private String cpu(){
        String cpu = "";
        //获取使用率
        //String cmd = "sar 1 1 | grep Average | awk '{print (100-$8)\"%\"}'";
        //英文版的系统使用Average,中文版的使用Average
        String cmd_en = "sar| grep Average | awk '{print (100-$8)\"%\"}'";
        String cmd_zh = "sar| grep 平均时间 | awk '{print (100-$8)\"%\"}'";
        String language = language();
        List<String>  cpuList = new ArrayList<>();
        if("zh".equals(language)){
            cpuList = execute(cmd_zh);
        }else{
            cpuList = execute(cmd_en);
        }
        if(null != cpuList && cpuList.size() > 0){
            cpu = cpuList.get(0);
        }
        cpu = "[CPU] 使用率" + cpu;
        return cpu;
    }
    /**
     * 内存
     * @return
     */
    private String memory(){
        String memory = "";
        //获取使用率
        String cmd = "free -m | sed -n '2p' | awk '{print $3\"MB \"($3+$4)\"MB \"$3/($3+$4)*100\"%\"}'";
        List<String>  memList = execute(cmd);
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
     * @return
     */
    private String disk(){
        String disk = "";
        //获取使用率
        String cmd = "df -h | grep home  | sed -n '1p' | awk '{print $2\" \" $3\" \" $4\" \" $5}'";
        String cmd_test = "df -h | grep home | sed -n '2p' | awk '{print $1\" \" $2\" \" $3\" \" $4}'";
        List<String>  memList = new ArrayList<>();
        if(true == isTest){
            memList = execute(cmd_test);
        }else{
            memList = execute(cmd);
        }
        if(null != memList && memList.size() > 0){
            disk = memList.get(0);
        }
        String[] detail = disk.split(" ");
        if(detail.length > 0){
            disk = "[磁盘] 总量" + detail[0] + ",使用" + detail[1] + "剩余"+detail[2] + ",使用率" + detail[3];
        }
        return disk;
    }
    /**
     * 获取服务器系统的语言
     * @return
     */
    private String language(){
        String language = "en";
        //获取使用率
        String cmd = "cat /etc/sysconfig/i18n";
        List<String>  languageList = execute(cmd);
        for(String line:languageList){
            if(line.contains("zh_CN")){
                language = "zh";
            }else {
                language = "en";
            }
        }
        return language;
    }

    /**
     * 执行命令,将查询到的结果按[行]放入List中
     * @param command
     * @return
     */
    public List<String>  execute(String command)  {
        List<String> list = new ArrayList<>();
        Process pro = null;
        Runtime runTime = Runtime.getRuntime();
        if (runTime == null) {
            System.err.println("Create runtime false!");
        }
        try {

            pro = runTime.exec(new String[] {"/bin/sh", "-c", command});
            InputStream inputStream = pro.getInputStream();
            list = readLine(inputStream);
            pro.destroy();
        } catch (Exception e) {
            System.out.println("LocalService execute Error");
            e.printStackTrace();
        }finally {
            return list;
        }
    }


}