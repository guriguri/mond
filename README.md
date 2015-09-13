mond
====

mond is the android monitoring tool.
* received a control message by mqtt in remote server.
* starts sshd and make a back-door in remote server by ssh tunneling.
* The operator accesses the android device as mond's back-door.

<h4>Reference</h4>
* The PseudoTerminalFactory(PseudoTerminalFactory.java and jni/*) has been referred to android-sshd(https://github.com/stepinto/android-sshd).



