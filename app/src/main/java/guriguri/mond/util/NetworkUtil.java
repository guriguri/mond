package guriguri.mond.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.util.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by guriguri on 2014. 12. 19..
 */
public class NetworkUtil {
    private static final Logger log = LoggerFactory.getLogger(NetworkUtil.class);

    public static final int NO_CONNECTIVITY = -1;

    public static int getConnectivityStatusDetail(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        int retValue = NO_CONNECTIVITY;
        String extraMsg = null;

        if (activeNetwork != null) {
            switch (activeNetwork.getType()) {
                case ConnectivityManager.TYPE_MOBILE:
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_WIMAX:
                case ConnectivityManager.TYPE_ETHERNET:
                    retValue = activeNetwork.getType();
                    extraMsg = "";
                    break;
                default:
                    extraMsg = ", invalid activeNetwork.getType=" + activeNetwork.getType();
                    break;
            }
        }
        else {
            extraMsg = ", activeNetwork is null";
        }

        log.debug("retValue[-1:NOT,0:MOBI,1:WIFI,6:WIMAX,9:ETH]={}{}", retValue, extraMsg);

        return retValue;
    }

    public static boolean getConnectivityStatus(Context context) {
        int activeNetworkType = getConnectivityStatusDetail(context);

        if (activeNetworkType != NO_CONNECTIVITY) {
            return true;
        }

        return false;
    }

    public static Map<String, String> getMACAddressAll() {
        Map<String, String> map = new HashMap<String, String>();

        try {
            List<NetworkInterface> list = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface ni : list) {
                StringBuffer buff = new StringBuffer();

                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    for (int i = 0, size = mac.length; i < size; i++) {
                        buff.append(String.format("%02X:", mac[i]));
                    }

                    if (buff.length() > 0) {
                        buff.deleteCharAt(buff.length() - 1);
                    }
                }

                if (buff.length() > 0) {
                    map.put(ni.getName(), buff.toString());
                    log.debug("mac, {}={}", ni.getName(), buff);
                }
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }

        return map;
    }

    public static String getMACAddress(String interfaceName) {
        String mac = null;
        if (StringUtils.isEmpty(interfaceName) == true) {
            log.error("interfaceName={} is null or empty", interfaceName);
            return mac;
        }

        interfaceName = interfaceName.toLowerCase();

        Map<String, String> map = getMACAddressAll();
        for (String key : map.keySet()) {
            if (interfaceName.equals(key) == true) {
                mac = map.get(key);
                break;
            }
        }

        log.debug("{} is mac={}", interfaceName, mac);

        return mac;
    }

    public static String getIPAddress(boolean useIPv4) {
        String ip = null;

        try {
            List<NetworkInterface> interfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface ni : interfaceList) {

                List<InetAddress> addrList = Collections.list(ni.getInetAddresses());
                for (InetAddress addr : addrList) {

                    if (addr.isLoopbackAddress() == false) {

                        ip = addr.getHostAddress();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(ip);

                        if ((useIPv4 == true) && (isIPv4 == true)) {
                            // nothing todo
                        } else if (((useIPv4 == true) && (isIPv4 == false)) ||
                                ((useIPv4 == false) && (isIPv4 == true))) {
                            ip = null;
                        } else if ((useIPv4 == false) && (isIPv4 == false)) {
                            int delim = ip.indexOf('%'); // drop ip6 port suffix
                            ip = delim < 0 ? ip : ip.substring(0, delim);
                        }
                    }
                }

                if (ip != null) {
                    break;
                }
            }

            log.debug("ip={}", ip);
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }

        return ip;
    }

}
