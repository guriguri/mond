package guriguri.mond.daemon;

import android.os.AsyncTask;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import guriguri.mond.factory.PseudoTerminalFactory;

/**
 * Created by max on 2014. 12. 14..
 */
public class SshServerTask extends AsyncTask<String, Void, Void> {
    private static final Logger log = LoggerFactory.getLogger(SshServerTask.class);

    private static Map<String, String> userMap = new HashMap<String, String>();

    private SshServer sshd;

    static {
        userMap.put("guriguri", "guriguri");
    }

    public SshServerTask(int port) throws Exception {
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setReuseAddress(true);
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                Set<String> userSet = userMap.keySet();

                if ((StringUtils.isEmpty(username) == false)
                        && (StringUtils.isEmpty(password) == false)
                        && (userSet.contains(username) == true)
                        && (userMap.get(username)
                        .equals
                                (password) == true)) {
                    log.debug("access, username={}", username);

                    return true;
                }

                log.error("deny, username={}, password={}", username, password);

                return false;
            }
        });
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("/sdcard/mond.ser"));
        sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setShellFactory(new PseudoTerminalFactory("/system/bin/sh", "-i"));

        sshd.start();

        log.info("START, sshd, port={}", port);
    }

    public SshServer getSshd() {
        return sshd;
    }

    @Override
    protected Void doInBackground(String... strings) {
        return null;
    }
}
