package guriguri.mond.daemon;

import android.os.AsyncTask;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guriguri.mond.tunneling.domain.SshServerInfo;


public class SshTunnelingTask extends AsyncTask<Object, Integer, Void> {
    private static final Logger log = LoggerFactory.getLogger(SshTunnelingTask.class);

    public static final int RETRY_CNT = 3;
    public static final long THREAD_SLEEP_MSEC = 1000L;
    public static final int TIMEOUT_MSEC = 15000;

    private Session session;

    public Session getSession() {
        return session;
    }

    private Session connect(SshServerInfo sshServerInfo) {
        try {
            JSch jsch = new JSch();

            Session session = jsch.getSession(sshServerInfo.getUser(), sshServerInfo.getHost(),
                    sshServerInfo.getPort());
            session.setPassword(sshServerInfo.getPasswd());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(TIMEOUT_MSEC);
            session.setPortForwardingR(sshServerInfo.getLport(), sshServerInfo.getRhost(),
                    sshServerInfo.getRport());

            log.info("ssh -R {}", sshServerInfo.toString4Tunneling());

            return session;
        } catch (Exception e) {
            String errMsg = e.getMessage();

            if ((errMsg != null) && (errMsg.indexOf("UnknownHostException") != -1)) {
                log.error("ssh -R {}, errMsg={}",
                        sshServerInfo.toString4Tunneling(),
                        e.getMessage());
            } else {
                log.error("ssh -R {}, passwd={}, errMsg={}, errCause={}",
                        sshServerInfo.toString4Tunneling(), sshServerInfo.getPasswd(),
                        e.getMessage(), e.getCause(), e);
            }
        }

        return null;
    }

    @Override
    protected Void doInBackground(Object... objects) {
        SshServerInfo sshServerInfo = null;

        try {
            sshServerInfo = (SshServerInfo) objects[0];
        } catch (Exception e) {
            log.error("invalid params, params={}, errMsg={}",
                    new Object[]{objects, e.getMessage(), e});
        }

        for (int i = 0; i < RETRY_CNT; i++) {
            Session session = connect(sshServerInfo);

            if (session != null) {
                this.session = session;
                break;
            }

            try {
                Thread.sleep(THREAD_SLEEP_MSEC);
            } catch (InterruptedException e) {
                log.error("{}", e.getMessage(), e);
            }
        }

        return null;
    }
}
