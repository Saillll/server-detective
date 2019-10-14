package com.asiainfo.cuc.serverdetective.task;

import com.asiainfo.cuc.serverdetective.entity.Server;
import com.asiainfo.cuc.serverdetective.entity.ServersEntity;
import com.asiainfo.cuc.serverdetective.service.CollectService;
import com.asiainfo.cuc.serverdetective.service.EmailService;
import com.asiainfo.cuc.serverdetective.service.LocalService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class WatcherTask extends QuartzJobBean {

    @Autowired
    private CollectService collectService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LocalService localService;

    @Autowired
    private ServersEntity serversEntity;

    @Value("${email.special}")
    private boolean special;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        //放在服务器上的时候使用免密登录，不需要密码。
        try {

            StringBuffer stringBuffer = new StringBuffer();
            JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
            boolean special = false;
            if(jobDataMap!=null ){
                special = jobDataMap.getBoolean("special");
            }
            for(Server server:serversEntity.getList()){
                List<Map<String,String>> list = new ArrayList<>();
                //jar放在这个服务器上运行，需要单独列出来，特殊处理
                if(server.isMainserver()){
                    list = localService.collect(server);
                }else{
                    list = collectService.collect(server);
                }
                stringBuffer.append(assembleEachServer(list));
            }
            String content = stringBuffer.toString();
            if(content.contains("死亡")){
                content = ("<h1><strong><font color='yellow'>服务器存在错误</font></strong></h1><br>") + content;
            }else {
                content = ("<h1><strong><font color='green'>一切正常</font></strong></h1><br>") + content;
            }

            log.info("=============================================================");
            log.info(content);
            log.info("=============================================================");
            if(special){
                emailService.sendSecret(content);
            }else{
                emailService.sendBase(content);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public StringBuffer assembleEachServer(List<Map<String,String>>... infos){
        StringBuffer sb = new StringBuffer();
        boolean faultExists = false;
        for(List<Map<String,String>> info:infos){
            Map<String,String> baseinfo = info.get(0);
            Map<String,String> appinfo = info.get(1);
            for (Map.Entry<String, String> m : baseinfo.entrySet()) {
                sb.append( m.getValue());
                sb.append("<br>");
            }
            for (Map.Entry<String, String> m : appinfo.entrySet()) {
                sb.append( m.getValue());
                sb.append("<br>");
            }
            sb.append("<br>");
            sb.append("<br>");

        }
        return sb;
    }

}
