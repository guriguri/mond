// IBindSshService.aidl
package guriguri.mond.tunneling.service;

import guriguri.mond.tunneling.domain.ReturnValue;
import guriguri.mond.tunneling.domain.SshServerInfo;

interface IBindTunnelingService {
    ReturnValue restartSshd(in SshServerInfo sshServerInfo);
    ReturnValue connect(in SshServerInfo sshServerInfo, in int retryCnt);
    ReturnValue disconnect();
    ReturnValue ping();
    ReturnValue saveSshServerInfo(in SshServerInfo sshServerInfo);
}
