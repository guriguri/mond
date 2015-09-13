package guriguri.mond.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.SshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import guriguri.mond.MainActivity;
import guriguri.mond.R;
import guriguri.mond.daemon.SshServerTask;
import guriguri.mond.daemon.SshTunnelingTask;
import guriguri.mond.tunneling.domain.ReturnValue;
import guriguri.mond.tunneling.domain.SshServerInfo;
import guriguri.mond.tunneling.service.IBindTunnelingService;
import guriguri.mond.util.NetworkUtil;
import guriguri.mond.util.PropUtil;

/**
 * Created by guriguri on 2015. 1. 5..
 */
public class SshService extends Service {
    private static final Logger log = LoggerFactory.getLogger(SshService.class);

    public static final String ACTION_RESTART_TUNNELING_SERVICE = "guriguri.mond.action.RESTART_TUNNELING_SERVICE";

    public static final long THREAD_SLEEP_MSEC_1ST = 5000L;
    public static final long THREAD_SLEEP_MSEC_2ND = (SshTunnelingTask.TIMEOUT_MSEC + SshTunnelingTask
            .THREAD_SLEEP_MSEC) * SshTunnelingTask.RETRY_CNT;

    public static final int TUNNELING_NOT_CONNECT = 0;
    public static final int TUNNELING_TRY_CONNECT = 1;
    public static final int TUNNELING_CONNECTING = 2;

    public static final String INTENT_KEY_TUNNELING_STATE = "tunnelingState";
    public static final int RETRY_COUNT_UNLIMITED = -1;
    public static final int RETRY_COUNT_DEFAULT = 10;

    private static final int RESTART_DELAY_MSEC = 10000;

    private static final String PROP_KEY_SERVER_PORT = "server.port";
    private static final String PROP_KEY_REMOTE_USER = "remote.user";
    private static final String PROP_KEY_REMOTE_PASSWD = "remote.passwd";
    private static final String PROP_KEY_REMOTE_HOST = "remote.host";
    private static final String PROP_KEY_REMOTE_PORT = "remote.port";
    private static final String PROP_KEY_TUNNEL_KEEPALIVE = "tunnel.keepAlive";
    private static final String PROP_KEY_TUNNEL_LPORT = "tunnel.lport";
    private static final String PROP_KEY_TUNNEL_RHOST = "tunnel.rhost";
    private static final String PROP_KEY_TUNNEL_RPORT = "tunnel.rport";

    public static AtomicInteger tunnelingState = new AtomicInteger(TUNNELING_NOT_CONNECT);

    private static Context context;
    private static BroadcastReceiver receiver;

    private static SshServer sshServer;
    private static SshTunnelingTask sshTunneling;

    private static SshServerInfo sshServerInfo;
    private static Boolean isForeground;

    private static final IBindTunnelingService binder = new IBindTunnelingService.Stub() {
        @Override
        public ReturnValue restartSshd(SshServerInfo sshServerInfo) throws
                RemoteException {
            try {
                SshServer newSshServer = new SshServerTask(sshServerInfo.getServerPort()).getSshd();

                if (sshServer != null) {
                    sshServer.stop(true);
                }

                sshServer = newSshServer;

                return new ReturnValue(ReturnValue.SUCC, "success");
            } catch (Exception e) {
                log.error("{}", e.getMessage(), e);
                return new ReturnValue(ReturnValue.FAIL, e.getMessage());
            }
        }

        @Override
        public ReturnValue connect(SshServerInfo sshServerInfo, int retryCnt) throws RemoteException {
            if (isValidTunnelingInfo(sshServerInfo) == false) {
                return new ReturnValue(ReturnValue.FAIL, "invalid sshServerInfo");
            } else if (NetworkUtil.getConnectivityStatus(context) == false) {
                log.debug("network is not connect");
                return new ReturnValue(ReturnValue.FAIL, "network is not connect");
            } else if (tunnelingState.get() == TUNNELING_TRY_CONNECT) {
                log.debug("already try tunneling");
                return new ReturnValue(ReturnValue.FAIL, "already try tunneling");
            } else if (sshServerInfo == null) {
                log.error("sshServiceInfo is null");
                return new ReturnValue(ReturnValue.FAIL, "sshServiceInfo is null");
            }

            tunnelingState.set(TUNNELING_TRY_CONNECT);
            sendBroadcast(MainActivity.ACTION_SHOW_TUNNELING_INFO);

            if ((sshTunneling != null) && (sshTunneling.getSession() != null)) {
                sshTunneling.getSession().disconnect();
                log.info("disconnect tunneling");
            }

            for (int i = 1; (retryCnt == RETRY_COUNT_UNLIMITED) || (i <= retryCnt); i++) {
                sshTunneling = new SshTunnelingTask();
                sshTunneling.execute(sshServerInfo);

                if ((isConnect(THREAD_SLEEP_MSEC_1ST) == true)
                        || (isConnect(THREAD_SLEEP_MSEC_2ND) == true)) {
                    tunnelingState.set(TUNNELING_CONNECTING);
                    sendBroadcast(MainActivity.ACTION_SHOW_TUNNELING_INFO);
                    log.info("success tunneling");
                    break;
                }

                log.debug("failure tunneling, retryCnt={}, sleepMsec={}", i,
                        (THREAD_SLEEP_MSEC_1ST + THREAD_SLEEP_MSEC_2ND));
            }

            if (tunnelingState.get() != TUNNELING_CONNECTING) {
                tunnelingState.set(TUNNELING_NOT_CONNECT);

                log.error("failure tunneling. check remote port forwarding listen port={} or misc" +
                        ".", sshServerInfo.getRport());
                return new ReturnValue(ReturnValue.FAIL, "failure tunneling. check remote port " +
                        "forwarding listen port=" + sshServerInfo.getRport() + " or misc.");
            }

            return new ReturnValue(ReturnValue.SUCC, "success");
        }

        @Override
        public ReturnValue disconnect() throws RemoteException {
            if ((sshTunneling != null) && (sshTunneling.getSession() != null)) {
                sshTunneling.getSession().disconnect();
                log.info("disconnect tunneling");
            } else {
                log.info("not connected");
            }

            tunnelingState.set(TUNNELING_NOT_CONNECT);
            sendBroadcast(MainActivity.ACTION_SHOW_TUNNELING_INFO);

            return new ReturnValue(ReturnValue.SUCC, "success");
        }

        @Override
        public ReturnValue ping() throws RemoteException {
            log.debug("receive, ping");
            return new ReturnValue(ReturnValue.SUCC, "success");
        }

        @Override
        public ReturnValue saveSshServerInfo(SshServerInfo sshServerInfo) throws
                RemoteException {
            if (sshServerInfo == null) {
                log.error("sshServiceInfo is null");
                return new ReturnValue(ReturnValue.FAIL, "sshServiceInfo is null");
            }

            Properties prop = new Properties();

            // server
            prop.put(PROP_KEY_SERVER_PORT, String.valueOf(sshServerInfo.getServerPort()));

            // tunneling
            prop.put(PROP_KEY_REMOTE_USER, sshServerInfo.getUser());
            prop.put(PROP_KEY_REMOTE_PASSWD, sshServerInfo.getPasswd());
            prop.put(PROP_KEY_REMOTE_HOST, sshServerInfo.getHost());
            prop.put(PROP_KEY_REMOTE_PORT, String.valueOf(sshServerInfo.getPort()));
            prop.put(PROP_KEY_TUNNEL_KEEPALIVE, String.valueOf(sshServerInfo.getKeepAlive()));
            prop.put(PROP_KEY_TUNNEL_LPORT, String.valueOf(sshServerInfo.getLport()));
            prop.put(PROP_KEY_TUNNEL_RHOST, sshServerInfo.getRhost());
            prop.put(PROP_KEY_TUNNEL_RPORT, String.valueOf(sshServerInfo.getRport()));

            try {
                PropUtil.setProperties(PropUtil.FILE_PRIORITY_PROPERTY, prop, "mond's properties");
                log.debug("file={}", PropUtil.FILE_PRIORITY_PROPERTY);

                return new ReturnValue(ReturnValue.SUCC, "success");
            } catch (Exception e) {
                log.error("{}", e.getMessage(), e);
                return new ReturnValue(ReturnValue.FAIL, e.getMessage());
            }
        }
    };

    public IBinder onBind(Intent intent) {
        log.info("BIND");

        String action = intent.getAction();

        if (SshServerInfo.ACTION_TUNNELING_SERVICE.equals(action) == true) {
            setForeground();

            sendBroadcast(MainActivity.ACTION_SHOW_SERVER_INFO);
            sendBroadcast(MainActivity.ACTION_SHOW_TUNNELING_INFO);

            return binder.asBinder();
        }

        return null;
    }

    @Override
    public void onCreate() {
        log.info("CREATE");

        super.onCreate();

        context = getApplicationContext();
        receiver = new SshServiceReceiver();

        init();

        try {
            if (sshServer == null) {
                sshServer = new SshServerTask(sshServerInfo.getServerPort()).getSshd();
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }

        registerReceiver(receiver, new IntentFilter(ConnectivityManager
                .CONNECTIVITY_ACTION));
        registerReceiver(receiver, new IntentFilter(SshServerInfo.ACTION_TUNNELING_SERVICE_RESTART_SSHD));
        registerReceiver(receiver, new IntentFilter(SshServerInfo.ACTION_TUNNELING_SERVICE_CONNECT));
        registerReceiver(receiver, new IntentFilter(SshServerInfo.ACTION_TUNNELING_SERVICE_DISCONNECT));
        registerReceiver(receiver, new IntentFilter(SshServerInfo.ACTION_TUNNELING_SERVICE_SAVE_PROPERTY));

        unregisterRestartAlarm();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.info("START");

        super.onStartCommand(intent, flags, startId);

        setForeground();

        sendBroadcast(MainActivity.ACTION_SHOW_SERVER_INFO);
        sendBroadcast(MainActivity.ACTION_SHOW_TUNNELING_INFO);

        return START_STICKY;
    }

    private void setForeground() {
        if (isForeground == null) {
            Service service = this;
            Class clazz = SshService.class;

            Notification notification = new Notification.Builder(service)
                    .setContentTitle(clazz.getSimpleName())
                    .setContentText("Foreground")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setWhen(System.currentTimeMillis()).build();

            startForeground(1, notification);

            isForeground = Boolean.TRUE;
        }
    }

    @Override
    public void onDestroy() {
        log.info("DESTROY");

        unregisterReceiver(receiver);

        registerRestartAlarm();

        super.onDestroy();
    }

    private void init() {
        Properties prop = PropUtil.getMergeProperty(context, PropUtil.FILE_DEFAULT_PROPERTY, PropUtil.FILE_PRIORITY_PROPERTY);

        // for server
        Integer serverPort = Integer.parseInt(prop.getProperty(PROP_KEY_SERVER_PORT));

        // for tunneling
        String user = prop.getProperty(PROP_KEY_REMOTE_USER);
        String passwd = prop.getProperty(PROP_KEY_REMOTE_PASSWD);
        String host = prop.getProperty(PROP_KEY_REMOTE_HOST);
        Integer port = PropUtil.getInt(prop.getProperty(PROP_KEY_REMOTE_PORT));
        Integer keepAlive = PropUtil.getInt(prop.getProperty(PROP_KEY_TUNNEL_KEEPALIVE));
        Integer lport = PropUtil.getInt(prop.getProperty(PROP_KEY_TUNNEL_LPORT));
        String rhost = prop.getProperty(PROP_KEY_TUNNEL_RHOST);
        Integer rport = PropUtil.getInt(prop.getProperty(PROP_KEY_TUNNEL_RPORT));

        sshServerInfo = new SshServerInfo(serverPort, user, passwd, host, port, keepAlive, lport,
                rhost, rport);
    }

    private void registerRestartAlarm() {
        log.debug("action={}", ACTION_RESTART_TUNNELING_SERVICE);

        Intent intent = new Intent(context, SshServiceReceiver.class);
        intent.setAction(ACTION_RESTART_TUNNELING_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(SshService.this, 0, intent, 0);

        long startTime = SystemClock.elapsedRealtime() + RESTART_DELAY_MSEC;

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, startTime, RESTART_DELAY_MSEC, sender);
    }

    private void unregisterRestartAlarm() {
        log.debug("action={}", ACTION_RESTART_TUNNELING_SERVICE);

        Intent intent = new Intent(context, SshServiceReceiver.class);
        intent.setAction(ACTION_RESTART_TUNNELING_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }

    private static void sendBroadcast(String action) {
        Intent intent = new Intent(action);
        intent.putExtra(SshServerInfo.INTENT_KEY_SSH_SERVER_INFO, sshServerInfo);

        if (MainActivity.ACTION_SHOW_SERVER_INFO.equals(action) == true) {
            log.info("action={}", action);
        } else if (MainActivity.ACTION_SHOW_TUNNELING_INFO.equals(action) == true) {
            intent.putExtra(INTENT_KEY_TUNNELING_STATE, tunnelingState.get());
            log.info("action={}, tunnelingState[0:NOT,1:TRY,2:CON]={}",
                    action, tunnelingState.get());
        } else {
            log.warn("invalid action={}", action);
            return;
        }

        context.sendBroadcast(intent);
    }

    private static boolean isValidTunnelingInfo(SshServerInfo sshServerInfo) {
        String user = sshServerInfo.getUser();
        String passwd = sshServerInfo.getPasswd();
        String host = sshServerInfo.getHost();
        String rhost = sshServerInfo.getRhost();
        Integer port = sshServerInfo.getPort();
        Integer keepAlive = sshServerInfo.getKeepAlive();
        Integer lport = sshServerInfo.getLport();
        Integer rport = sshServerInfo.getRport();

        if ((StringUtils.isEmpty(user) == true)
                || (StringUtils.isEmpty(passwd) == true)
                || (StringUtils.isEmpty(host) == true)
                || (StringUtils.isEmpty(rhost) == true)
                || (port == null)
                || (keepAlive == null)
                || (lport == null)
                || (rport == null)) {
            log.warn("ssh -R {}", sshServerInfo.toString4Tunneling());

            return false;
        }

        return true;
    }

    private static boolean isConnect(long sleepMsec) {
        try {
            Thread.sleep(sleepMsec);
        } catch (InterruptedException e) {
            log.error("{}", e.getMessage(), e);
        }

        if ((sshTunneling != null) && (sshTunneling.getSession() != null)) {
            return true;
        }

        return false;
    }

    public static class SshServiceReceiver extends BroadcastReceiver {
        public void setNotification(Context context, String ip) {
            Class clazz = SshService.class;

            Notification notification = new Notification.Builder(context)
                    .setContentTitle(clazz.getSimpleName())
                    .setContentText("MyIP=" + ip)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setWhen(System.currentTimeMillis()).build();

            NotificationManager nm = (NotificationManager) context.getSystemService(Context
                    .NOTIFICATION_SERVICE);
            nm.notify(0, notification);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();
            ReturnValue returnValue = null;

            log.debug("action={}", action);

            // for restart
            if ((ACTION_RESTART_TUNNELING_SERVICE.equals(action) == true)
                    || (Intent.ACTION_BOOT_COMPLETED.equals(action) == true)) {
                Intent i = new Intent(context, SshService.class);
                context.startService(i);
            }
            // for network
            else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action) == true) {
                try {
                    if (sshServerInfo.getKeepAlive() == SshServerInfo.KEEP_ALIVE_ON) {
                        binder.connect(sshServerInfo, RETRY_COUNT_UNLIMITED);
                    } else {
                        log.debug("keepAlive is OFF");
                    }

                    sendBroadcast(MainActivity.ACTION_SHOW_SERVER_INFO);

                    setNotification(context, NetworkUtil.getIPAddress(true));
                } catch (RemoteException e) {
                    log.error("action={}, {}", action, e.getMessage(), e);
                }
            }
            // for sshService
            else if (SshServerInfo.ACTION_TUNNELING_SERVICE_RESTART_SSHD.equals(action) == true) {
                try {
                    SshServerInfo sshServerInfo = (SshServerInfo) bundle.get(SshServerInfo.INTENT_KEY_SSH_SERVER_INFO);

                    returnValue = binder.restartSshd(sshServerInfo);
                } catch (Exception e) {
                    log.error("action={}, {}", action, e.getMessage(), e);
                    returnValue = new ReturnValue(ReturnValue.FAIL, e.getMessage());
                }
            } else if (SshServerInfo.ACTION_TUNNELING_SERVICE_CONNECT.equals(action) == true) {
                try {
                    SshServerInfo sshServerInfo = (SshServerInfo) bundle.get(SshServerInfo.INTENT_KEY_SSH_SERVER_INFO);
                    Integer retryCnt = (Integer) bundle.get(SshServerInfo.INTENT_KEY_RETRY_CNT);

                    if (retryCnt == null) {
                        retryCnt = RETRY_COUNT_DEFAULT;
                    }

                    returnValue = binder.connect(sshServerInfo, retryCnt);
                } catch (Exception e) {
                    log.error("action={}, {}", action, e.getMessage(), e);
                    returnValue = new ReturnValue(ReturnValue.FAIL, e.getMessage());
                }
            } else if (SshServerInfo.ACTION_TUNNELING_SERVICE_DISCONNECT.equals(action) == true) {
                try {
                    returnValue = binder.disconnect();
                } catch (Exception e) {
                    log.error("action={}, {}", action, e.getMessage(), e);
                    returnValue = new ReturnValue(ReturnValue.FAIL, e.getMessage());
                }
            } else if (SshServerInfo.ACTION_TUNNELING_SERVICE_SAVE_PROPERTY.equals(action) == true) {
                try {
                    SshServerInfo sshServerInfo = (SshServerInfo) bundle.get(SshServerInfo.INTENT_KEY_SSH_SERVER_INFO);
                    returnValue = binder.saveSshServerInfo(sshServerInfo);
                } catch (Exception e) {
                    log.error("action={}, {}", action, e.getMessage(), e);
                    returnValue = new ReturnValue(ReturnValue.FAIL, e.getMessage());
                }
            }

            if (returnValue != null) {
                Intent i = new Intent(ReturnValue.ACTION_TUNNELING_SERVICE_RETURN_VALUE);
                i.putExtra(ReturnValue.INTENT_KEY_ACTION, action);
                i.putExtra(ReturnValue.INTENT_KEY_RETURN_VALUE, returnValue);
                context.sendBroadcast(i);

                log.info("sendBroadcast, action={}", ReturnValue.ACTION_TUNNELING_SERVICE_RETURN_VALUE);
            }
        }
    }
}
