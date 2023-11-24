package core;

import core.peer.Peer;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Remote;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Chord implements Remote, Runnable
{
    public Chord(int port) throws IOException, NoSuchAlgorithmException
    {
        this.m_state = 0;
        this.m_port = port;
        m_peer = new Peer(this.m_port);
        m_peer.DefineSenderAndReceiver();
        DefineCommands();
        this.run();
    }

    public Chord(int port, String bootstrapped_ip, int bootstrapped_port) throws IOException, NoSuchAlgorithmException
    {
        this.m_state = 1;
        this.m_port = port;
        this.m_bootstrapped_ip = bootstrapped_ip;
        this.m_bootstrapped_port = bootstrapped_port;
        m_peer = new Peer(this.m_port);
        m_peer.DefineSenderAndReceiver();
        DefineCommands();
        this.run();
    }

    public void Close()
    {
        m_peer.Close();
        m_peer = null;
    }

    public Peer GetPeer()
    {
        return m_peer;
    }

    private void DefineCommands()
    {
        this.m_commands = new HashMap<>();
        this.m_commands.put("PING", this.m_peer::Ping);
        this.m_commands.put("PONG", this.m_peer::Pong);
        this.m_commands.put("FIND_NODE_REQUEST", this.m_peer::FindNodeRequest);
        this.m_commands.put("FIND_NODE_RESPONSE", this.m_peer::FindNodeResponse);
        this.m_commands.put("EXIT", this::Exit);
    }

    public void run()
    {
        this.m_peer.PrintRoutingTable();
        Lib.Pair<Lib.Pair<String, Integer>, byte[]> current_input = null;

        try
        {
            while (m_peer != null) {
                switch (this.m_state) {
                    case 0 -> {
                        current_input = m_peer.GetNextReceived();
                        this.m_state = (current_input == null) ? 0 : 2;
                    }
                    case 1 -> {
                        m_peer.Send(this.m_bootstrapped_ip, this.m_bootstrapped_port, ("FIND_NODE_REQUEST" + " " + m_peer.m_id).getBytes());
                        this.m_state = 0;
                    }
                    case 2 -> {
                        assert current_input != null;
                        HandleCommand(current_input);
                        this.m_state = 0;
                    }
                }
            }
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }

    private void HandleCommand(Lib.Pair<Lib.Pair<String, Integer>, byte[]> request) throws IOException, InterruptedException, ClassNotFoundException
    {
        int port = request.first.second;
        InetAddress ip_address = InetAddress.getByName(request.first.first);
        String[] tokens = new String(request.second).replace("\0", "").split(" ");

        if(!this.m_commands.containsKey(tokens[0]))
        {
            this.CloseRemotePeer(new Lib.Pair<>(ip_address.getHostAddress(), port));
        }
        else
        {
            var command = this.m_commands.get(tokens[0]);
            command.Parse(new Peer.RoutingTableEntry(request.first.first, request.first.second), Arrays.copyOfRange(request.second, tokens[0].length() + 1, request.second.length));
        }
        m_peer.PrintRoutingTable();

        this.m_state = 0;
    }

    private void Exit(Peer.RoutingTableEntry peer_info, byte[] message)
    {
        m_peer.Close();
        m_peer = null;
    }

    private void CloseRemotePeer(Lib.Pair<String, Integer> peer) throws InterruptedException
    {
        m_peer.Send(peer.first, peer.second, ("EXIT").getBytes());
    }

    private int m_port;
    private int m_state;
    private Peer m_peer;
    private String m_bootstrapped_ip;
    private int m_bootstrapped_port;
    private HashMap<String, DDMSCommand> m_commands;
}
