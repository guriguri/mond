package guriguri.mond.tunneling.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by guriguri on 2015. 1. 5..
 */
public class SshServerInfo implements Parcelable {
    public static final String ACTION_TUNNELING_SERVICE =
            "guriguri.mond.action.TUNNELING_SERVICE";

    public static final String ACTION_TUNNELING_SERVICE_RESTART_SSHD =
            "guriguri.mond.action.TUNNELING_SERVICE_RESTART_SSHD";
    public static final String ACTION_TUNNELING_SERVICE_CONNECT =
            "guriguri.mond.action.TUNNELING_SERVICE_CONNECT";
    public static final String ACTION_TUNNELING_SERVICE_DISCONNECT =
            "guriguri.mond.action.TUNNELING_SERVICE_DISCONNECT";
//    public static final String ACTION_TUNNELING_SERVICE_PING =
//            "guriguri.mond.action.TUNNELING_SERVICE_PING";
    public static final String ACTION_TUNNELING_SERVICE_SAVE_PROPERTY =
            "guriguri.mond.action.TUNNELING_SERVICE_SAVE_PROPERTY";

    public static final String INTENT_KEY_SSH_SERVER_INFO = "sshServerInfo";
    public static final String INTENT_KEY_RETRY_CNT = "retryCnt";

    public static final Integer KEEP_ALIVE_ON = 0x01;
    public static final Integer KEEP_ALIVE_OFF = 0x02;

    public static final Parcelable.Creator<SshServerInfo> CREATOR = new Parcelable.Creator<SshServerInfo>() {

        public SshServerInfo createFromParcel(Parcel in) {
            return new SshServerInfo(in.readInt(), in.readString(), in.readString(), in.readString(),
                    in.readInt(), in.readInt(), in.readInt(), in.readString(), in.readInt());
        }

        public SshServerInfo[] newArray(int size) {
            return new SshServerInfo[size];
        }
    };

    // server
    private Integer serverPort;
    private String user;
    private String passwd;
    private String host;
    private Integer port;

    // tunneling
    private Integer keepAlive;
    private Integer lport;
    private String rhost;
    private Integer rport;

    public SshServerInfo(Integer serverPort, String user, String passwd, String host, Integer port,
                         Integer keepAlive, Integer lport, String rhost, Integer rport) {
        // server
        this.serverPort = serverPort;

        // tunneling
        this.user = user;
        this.passwd = passwd;
        this.host = host;
        this.port = port;
        this.keepAlive = keepAlive;
        this.lport = lport;
        this.rhost = rhost;
        this.rport = rport;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeInt(serverPort);
        out.writeString(user);
        out.writeString(passwd);
        out.writeString(host);
        out.writeInt(port);
        out.writeInt(keepAlive);
        out.writeInt(lport);
        out.writeString(rhost);
        out.writeInt(rport);
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public String getUser() {
        return user;
    }

    public String getPasswd() {
        return passwd;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getKeepAlive() {
        return keepAlive;
    }

    public Integer getLport() {
        return lport;
    }

    public String getRhost() {
        return rhost;
    }

    public Integer getRport() {
        return rport;
    }

    @Override
    public String toString() {
        return "SshServerInfo{" +
                "serverPort=" + serverPort +
                ", user='" + user + '\'' +
                ", passwd='" + passwd + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                "keepAlive=" + keepAlive +
                ", lport=" + lport +
                ", rhost='" + rhost + '\'' +
                ", rport=" + rport +
                '}';
    }

    public String toString4Tunneling() {
        return lport + ":" + rhost + ":" + rport + " " + user + "@" + host + " -p " + port;
    }
}
