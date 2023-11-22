package core;

import java.net.InetAddress;

public interface Command
{
    public void Parse(Lib.Pair<Lib.Pair<String, Integer>, String> request, String[] command) throws InterruptedException;
}
