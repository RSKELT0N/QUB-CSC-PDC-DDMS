package core;

import core.peer.Peer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface RPC
{
    public void Parse(Peer.RoutingTableEntry peer_info, byte[] message) throws InterruptedException, IOException, ClassNotFoundException, NoSuchAlgorithmException;
}
