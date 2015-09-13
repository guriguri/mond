package guriguri.mond;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guriguri.mond.service.SshService;
import guriguri.mond.tunneling.domain.SshServerInfo;
import guriguri.mond.tunneling.service.IBindTunnelingService;
import guriguri.mond.util.NetworkUtil;


public class MainActivity extends Activity {
    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

    public static final String ACTION_SHOW_SERVER_INFO = "guriguri.mond.action.SHOW_SERVER_INFO";
    public static final String ACTION_SHOW_TUNNELING_INFO = "guriguri.mond.action.SHOW_TUNNELING_INFO";

    private RemoteServiceConnection remoteServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log.info("CREATE");

        registerReceiver(connectivityReceiver, new IntentFilter(ACTION_SHOW_SERVER_INFO));
        registerReceiver(connectivityReceiver, new IntentFilter(ACTION_SHOW_TUNNELING_INFO));

        remoteServiceConnection = new RemoteServiceConnection();

        bindTunnelingService();
    }

    private void bindTunnelingService() {
        if (bindService(new Intent(SshServerInfo.ACTION_TUNNELING_SERVICE),
                remoteServiceConnection, Activity.BIND_AUTO_CREATE) == false) {
            log.error("bindService with {}", SshServerInfo.ACTION_TUNNELING_SERVICE);
        }
        else {
            log.info("bindService with {}", SshServerInfo.ACTION_TUNNELING_SERVICE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            unregisterReceiver(connectivityReceiver);

            unbindService(remoteServiceConnection);
            log.info("unbindService with {}", SshServerInfo.ACTION_TUNNELING_SERVICE);
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }

        log.info("STOP");
    }

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SshServerInfo sshServerInfo = intent.getParcelableExtra(SshServerInfo.INTENT_KEY_SSH_SERVER_INFO);

            String action = intent.getAction();

            if (ACTION_SHOW_SERVER_INFO.equals(action) == true) {
                printServerInfo(sshServerInfo);
            } else if (ACTION_SHOW_TUNNELING_INFO.equals(action) == true) {
                Bundle bundle = intent.getExtras();
                Integer tunnelingState = (Integer) bundle.get(SshService
                        .INTENT_KEY_TUNNELING_STATE);
                printTunnelingInfo(tunnelingState, sshServerInfo);
            }

            log.debug("action={}", action);
        }
    };

    private void printServerInfo(SshServerInfo sshServerInfo) {
        String ipv4 = NetworkUtil.getIPAddress(true);
        log.info("ip={}, port={}", ipv4, sshServerInfo.getPort());

        TextView serverIp = (TextView) findViewById(R.id.server_ip);
        serverIp.setText("Server IP: " + ipv4);

        TextView serverPort = (TextView) findViewById(R.id.server_port);
        serverPort.setText("Server Port: " + sshServerInfo.getServerPort());
    }

    private void printTunnelingInfo(int tunnelingState, SshServerInfo sshServerInfo) {
        log.info("tunnelingState[0:NOT,1:TRY,2:CON]={}, keepAlive(1:ON,2:OFF)={}, TunnelingInfo[-R {}]",
                tunnelingState, sshServerInfo.getKeepAlive(), sshServerInfo.toString4Tunneling());

        String msg = null;

        if (tunnelingState == SshService.TUNNELING_NOT_CONNECT) {
            msg = "not connect";
        } else if (tunnelingState == SshService.TUNNELING_TRY_CONNECT) {
            msg = "try connect";
        } else if (tunnelingState == SshService.TUNNELING_CONNECTING) {
            msg = "-R " + sshServerInfo.toString4Tunneling();
        } else {
            msg = "invalid tunnelingState=" + tunnelingState;
        }

        TextView tunnelingInfo = (TextView) findViewById(R.id.tunneling_info);
        tunnelingInfo.setText("Tunneling Info: " + msg);
    }

    public class RemoteServiceConnection implements ServiceConnection {
        IBindTunnelingService bindTunnelingServiceService;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bindTunnelingServiceService = IBindTunnelingService.Stub.asInterface(service);
            log.info("{}", name);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log.info("{}", name);
        }
    }
}
