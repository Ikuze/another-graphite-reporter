package utils;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Pattern;
import java.lang.Integer;

import org.apache.commons.lang.StringUtils;

public class GraphiteValidator {

    static final String portPatern = "^[0-9]+$";

    public boolean validateIpFormat(String ip) {
        InetSocketAddress add = new InetSocketAddress(ip, 0);
        return !add.isUnresolved();
    }

    public boolean validatePortFormat(String port) {
        Pattern pattern = Pattern.compile(this.portPatern);
        return (pattern.matcher(port).matches() && (Integer.parseInt(port) < 65536));
    }

    public boolean isListening(String ip, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 5000);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public boolean isIpPresent(String ip) {
        if (ip.length() == 0) {
            return false;
        }
        return true;
    }

    public boolean isPortPresent(String port) {
        if (port.length() == 0) {
            return false;
        }
        return true;
    }

    public boolean isIDPresent(String id) {
        if (id.length() == 0) {
            return false;
        }
        return true;
    }

    public boolean isIDTooLong(String description, int length) {
        if (description.length() > length) {
            return true;
        }
        return false;
    }
    
    public boolean isBaseQueueNamePresent(String baseQueueName) {
        return StringUtils.isNotBlank(baseQueueName);
    }

    public boolean validateBaseQueueName(String value) {
        if(value.endsWith(".")){
            return false;
        }
        return true;
    }

}
