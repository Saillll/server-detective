package com.asiainfo.cuc.serverdetective.connector;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;

import java.util.Properties;

/**
 * 无用
 */
public class JSCHUtil {
    private static JSCHUtil instance = new JSCHUtil();

    private JSCHUtil() {
    }


    private Session getSession(String host, int port, String ueseName)
            throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(ueseName, host, port);

        return session;
    }

    public Session connect(String host, int port, String ueseName,
                           String password) throws Exception {
        Session session = getSession(host, port, ueseName);
        session.setPassword(password);

        Properties config = new Properties();
        config.setProperty("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();

        return session;
    }

    public String execCmd(Session session, String command)
            throws Exception {
        if (session == null) {
            throw new RuntimeException("Session is null!");
        }

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        InputStream in = channel.getInputStream();
        byte[] b = new byte[1024];

        channel.setCommand(command);
        channel.connect();

        StringBuffer buffer = new StringBuffer();

        while (in.read(b) > 0) {
            buffer.append(new String(b));
        }

        channel.disconnect();

        return buffer.toString();
    }

    public void clear(Session session) {
        if ((session != null) && session.isConnected()) {
            session.disconnect();
            session = null;
        }
    }

}
