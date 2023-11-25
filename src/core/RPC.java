package core;

import core.peer.Peer;

import java.io.IOException;

public interface RPC
{
    public void Parse(Peer.RoutingTableEntry peer_info, byte[] message) throws InterruptedException, IOException, ClassNotFoundException;
}
