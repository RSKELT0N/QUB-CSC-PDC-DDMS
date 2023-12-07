package core;

import core.peer.Peer;
import core.peer.Runner;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.rmi.Remote;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Kademlia implements Remote, Runnable
{

    public Kademlia()
    {
        this.m_peer = null;
        this.m_state = 0;
    }

    public void ConnectToBootStrapped(String bootstrapped_ip, int bootstrapped_port) throws InterruptedException
    {
        m_peer.JoinThroughPeer(bootstrapped_ip, bootstrapped_port);
    }

    public void ConnectThroughBroadcast() throws IOException, InterruptedException
    {
        m_peer.JoinThroughBroadcast();
    }

    private void DefineCommands()
    {
        this.m_commands = new HashMap<>();
        this.m_commands.put("PING",                this.m_peer::Ping);
        this.m_commands.put("PONG",                this.m_peer::Pong);
        this.m_commands.put("FIND_NODE_REQUEST",   this.m_peer::FindNodeRequest);
        this.m_commands.put("FIND_NODE_RESPONSE",  this.m_peer::FindNodeResponse);
        this.m_commands.put("FIND_VALUE_REQUEST",  this.m_peer::FindValueRequest);
        this.m_commands.put("FIND_VALUE_RESPONSE", this.m_peer::FindValueResponse);
        this.m_commands.put("FIND_KEYS_REQUEST",   this.m_peer::FindKeysRequest);
        this.m_commands.put("FIND_KEYS_RESPONSE",  this.m_peer::FindKeysResponse);
        this.m_commands.put("STORE",               this.m_peer::Store);
        this.m_commands.put("EXIT",                this::Exit);
    }

    public void PrintInfo()
    {
        System.out.println("Peer (" + m_peer.m_id + ")\n----------------");
        System.out.format("%-25s [%s]\n", "Peer Nickname:", m_peer.m_nickname);
        System.out.format("%-25s [%d]\n", "Peer ID:", m_peer.m_id);
        System.out.format("%-25s [%s]\n", "Socket IP Address:", m_peer.m_socket.m_ip_address);
        System.out.format("%-25s [%d]\n", "Socket Port:", m_peer.m_socket.m_port);
        System.out.format("%-25s [%s]\n", "Bootstrapped IP Address:", (m_peer.m_bootstrapped_ip == null) ? "" : m_peer.m_bootstrapped_ip);
        System.out.format("%-25s [%s]\n", "Bootstrapped Port:", (m_peer.m_bootstrapped_port == 0) ? "" : String.valueOf(m_peer.m_bootstrapped_port));
        System.out.println("----------------");
    }

    public void StoreData(String key, String value) throws NoSuchAlgorithmException, InterruptedException, IOException
    {
        m_peer.AddDataItem(key, value);
    }

    public void run()
    {
        try
        {
            Lib.Pair<Peer.RoutingTableEntry, byte[]> current_input = null;
            while (m_peer != null)
            {
                switch (this.m_state)
                {
                    case 0:
                        current_input = m_peer.GetNextReceived();
                        this.m_state = (current_input == null) ? 0 : 2;
                        break;
                    case 2:
                        HandleCommand(current_input);
                        this.m_state = 0; break;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error: " + e + "\n\n"  + "Stacktrace: " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void HandleCommand(Lib.Pair<Peer.RoutingTableEntry, byte[]> request) throws IOException, InterruptedException, NoSuchAlgorithmException
    {
        String[] tokens = new String(request.second).replace("\0", "").split(" ");
        String command_str = tokens[0].split(":")[0];

        int port = request.first.port;
        InetAddress ip_address = InetAddress.getByName(request.first.ip_address);

        if(!this.m_commands.containsKey(command_str))
        {
            this.CloseRemotePeer(new Lib.Pair<>(ip_address.getHostAddress(), port));
            this.m_peer.RemovePeerFromRoutingTable(Lib.SHA1(ip_address + ":" + port, BigInteger.valueOf(1).shiftLeft(m_peer.m_m_bits)));
        }
        else
        {
            var command = this.m_commands.get(command_str);

            new Thread(() -> {
                try {
                    command.Parse(request.first, request.second);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        this.m_state = 0;
    }

    public void GetallDataKeys() throws InterruptedException
    {
        m_peer.ContactAllBuckets();

        for(var bucket : m_peer.m_routing_table.entrySet())
        {
            var peer = bucket.getValue().firstEntry();

            if(peer != null)
                m_peer.Send(peer.getValue().ip_address, peer.getValue().port, (m_peer.FormatCommand("FIND_KEYS_REQUEST")).getBytes(), true);
        }
    }

    public void ToggleLink()
    {
        ((Runner) m_peer.m_sender).ToggleLink();
        ((Runner) m_peer.m_receiver).ToggleLink();
        ((Runner) m_peer.m_heartbeat).ToggleLink();
    }

    public void InitPeer(String nickname, int port) throws IOException, NoSuchAlgorithmException
    {
        m_peer = new Peer(nickname, port);
        m_peer.DefineSenderAndReceiver();
        DefineCommands();
        new Thread(this).start();
    }

    private void Exit(Peer.RoutingTableEntry peer_info, byte[] message)
    {
        m_peer.Close();
        m_peer = null;
        System.out.println("\rWaiting on Runner Services to finish");
        System.exit(-1);
    }

    private void CloseRemotePeer(Lib.Pair<String, Integer> peer) throws InterruptedException
    {
        m_peer.Send(peer.first, peer.second, (m_peer.FormatCommand("EXIT")).getBytes(), false);
    }

    public void Close()
    {
        Exit(null, null);
    }

    public Peer GetPeer()
    {
        return m_peer;
    }

    private int m_state;
    private Peer m_peer;
    private HashMap<String, RPC> m_commands;
}
