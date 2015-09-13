package test.tunnelingaidltest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guriguri.mond.tunneling.domain.ReturnValue;
import guriguri.mond.tunneling.domain.SshServerInfo;
import guriguri.mond.tunneling.service.IBindTunnelingService;


public class MainActivity extends Activity {
    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

    private SshServerInfo sshServerInfo;
    private RemoteServiceConnection remoteServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // for server
        Integer serverPort = 2111;

        // for tunneling
        String user = "signage";
        String passwd = "signage1234!@#$";
        String host = "smartshelf.iptime.org";
        Integer port = 2222;
        Integer keepAlive = SshServerInfo.KEEP_ALIVE_ON;
        Integer lport = 2222;
        String rhost = "localhost";
        Integer rport = 2111;

        sshServerInfo = new SshServerInfo(serverPort, user, passwd, host, port, keepAlive, lport,
                rhost, rport);

        registerReceiver(receiver, new IntentFilter(ReturnValue.ACTION_TUNNELING_SERVICE_RETURN_VALUE));

        Button restartSshdButtonByBind = (Button) findViewById(R.id.btn_restart_sshd_by_bind);
        restartSshdButtonByBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = null;

                for (int i = 0; i < 2; i++) {
                    try {
                        ReturnValue returnValue = remoteServiceConnection.bindTunnelingServiceService
                                .restartSshd(sshServerInfo);

                        if (returnValue.getErrCode() == ReturnValue.SUCC) {
                            msg = "ok, restart sshd, port=" + sshServerInfo.getServerPort();
                        } else {
                            msg = "errMsg=" + returnValue.getErrMsg();
                        }

                        break;
                    } catch (Exception e) {
                        log.error("{}", e.getMessage(), e);

                        msg = "errMsg=" + e.getMessage();

                        if (e instanceof android.os.DeadObjectException) {
                            bindTunnelingService();
                        }
                    }
                }

                log.debug("Restart Sshd Click By Bind, {}", msg);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        Button startButtonByBind = (Button) findViewById(R.id.btn_start_tunneling_by_bind);
        startButtonByBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = null;

                for (int i = 0; i < 2; i++) {
                    try {
                        ReturnValue returnValue = remoteServiceConnection.bindTunnelingServiceService.connect
                                (sshServerInfo,
                                        10 /* retryCnt */);

                        if (returnValue.getErrCode() == ReturnValue.SUCC) {
                            msg = "ok, " + sshServerInfo.toString4Tunneling();
                        } else {
                            msg = "errMsg=" + returnValue.getErrMsg();
                        }

                        break;
                    } catch (Exception e) {
                        log.error("{}", e.getMessage(), e);

                        msg = "errMsg=" + e.getMessage();

                        if (e instanceof android.os.DeadObjectException) {
                            bindTunnelingService();
                        }
                    }
                }

                log.debug("Start Click By Bind, {}", msg);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        Button stopButtonByBind = (Button) findViewById(R.id.btn_stop_tunneling_by_bind);
        stopButtonByBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = null;

                for (int i = 0; i < 2; i++) {
                    try {
                        ReturnValue returnValue = remoteServiceConnection
                                .bindTunnelingServiceService
                                .disconnect();

                        if (returnValue.getErrCode() == ReturnValue.SUCC) {
                            msg = "ok, stop";
                        } else {
                            msg = "errMsg=" + returnValue.getErrMsg();
                        }

                        break;
                    } catch (Exception e) {
                        log.error("{}", e.getMessage(), e);

                        msg = "errMsg=" + e.getMessage();

                        if (e instanceof android.os.DeadObjectException) {
                            bindTunnelingService();
                        }
                    }
                }

                log.debug("Stop Click By Bind, {}", msg);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        Button saveButtonByBind = (Button) findViewById(R.id.btn_save_properties_by_bind);
        saveButtonByBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = null;

                for (int i = 0; i < 2; i++) {
                    try {
                        ReturnValue returnValue = remoteServiceConnection
                                .bindTunnelingServiceService
                                .saveSshServerInfo
                                        (sshServerInfo);

                        if (returnValue.getErrCode() == ReturnValue.SUCC) {
                            msg = "ok, save";
                        } else {
                            msg = "errMsg=" + returnValue.getErrMsg();
                        }

                        break;
                    } catch (Exception e) {
                        log.error("{}", e.getMessage(), e);

                        msg = "errMsg=" + e.getMessage();

                        if (e instanceof android.os.DeadObjectException) {
                            bindTunnelingService();
                        }
                    }
                }

                log.debug("Save Click By Bind, {}", msg);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        Button restartSshdButtonByBroadcast = (Button) findViewById(R.id.btn_restart_sshd_by_broadcast);
        restartSshdButtonByBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SshServerInfo.ACTION_TUNNELING_SERVICE_RESTART_SSHD);
                intent.putExtra(SshServerInfo.INTENT_KEY_SSH_SERVER_INFO, sshServerInfo);

                sendBroadcast(intent);
                log.debug("Send Restart Sshd Click By Broadcast");
            }
        });

        Button startButtonByBroadcast = (Button) findViewById(R.id.btn_start_tunneling_by_broadcast);
        startButtonByBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SshServerInfo.ACTION_TUNNELING_SERVICE_CONNECT);
                intent.putExtra(SshServerInfo.INTENT_KEY_SSH_SERVER_INFO, sshServerInfo);
                intent.putExtra(SshServerInfo.INTENT_KEY_RETRY_CNT, 10);

                sendBroadcast(intent);
                log.debug("Send Start Click By Broadcast");
            }
        });

        Button stopButtonByBroadcast = (Button) findViewById(R.id.btn_stop_tunneling_by_broadcast);
        stopButtonByBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SshServerInfo.ACTION_TUNNELING_SERVICE_DISCONNECT);

                sendBroadcast(intent);
                log.debug("Send Stop Click By Broadcast");
            }
        });

        Button saveButtonByBroadcast = (Button) findViewById(R.id.btn_save_properties_by_broadcast);
        saveButtonByBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SshServerInfo.ACTION_TUNNELING_SERVICE_SAVE_PROPERTY);
                intent.putExtra(SshServerInfo.INTENT_KEY_SSH_SERVER_INFO, sshServerInfo);

                sendBroadcast(intent);

                log.debug("Send Save Click By Broadcast");
            }
        });

        remoteServiceConnection = new RemoteServiceConnection();

        bindTunnelingService();
    }

    private void bindTunnelingService() {
        if (bindService(new Intent(SshServerInfo.ACTION_TUNNELING_SERVICE),
                remoteServiceConnection, Activity.BIND_AUTO_CREATE) == false) {
            log.error("bindService with {}", SshServerInfo.ACTION_TUNNELING_SERVICE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            unregisterReceiver(receiver);

            unbindService(remoteServiceConnection);
            log.info("unbindService with {}", SshServerInfo.ACTION_TUNNELING_SERVICE);
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ReturnValue.ACTION_TUNNELING_SERVICE_RETURN_VALUE.equals(action) == true) {
                Bundle bundle = intent.getExtras();
                String sendAction = (String) bundle.get(ReturnValue.INTENT_KEY_ACTION);
                ReturnValue returnValue = (ReturnValue) bundle.get(ReturnValue.INTENT_KEY_RETURN_VALUE);

                String msg = sendAction;
                if (returnValue.getErrCode() == ReturnValue.SUCC) {
                    msg += ", ok";
                } else {
                    msg += ", errMsg=" + returnValue.getErrMsg();
                }

                log.debug("{}", sendAction, msg);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
            else {
                log.warn("action={}", action);
            }
        }
    };
}
