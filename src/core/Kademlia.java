package core;

import core.peer.Peer;

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
        this.m_bootstrapped = false;
        this.m_state = 0;
        this.m_port = 0;
        new Thread(this).start();
    }

    public Kademlia(int port)
    {
        this.m_bootstrapped = false;
        this.m_state = 0;
        this.m_port = port;
        new Thread(this).start();
    }

    public Kademlia(int port, String bootstrapped_ip, int bootstrapped_port)
    {
        this.m_port = port;
        ConnectToBootStrapped(bootstrapped_ip, bootstrapped_port);
        new Thread(this).start();
    }

    public void ConnectToBootStrapped(String bootstrapped_ip, int bootstrapped_port)
    {
        this.m_state = 1;
        this.m_bootstrapped = true;
        this.m_bootstrapped_ip = bootstrapped_ip;
        this.m_bootstrapped_port = bootstrapped_port;
    }

    private void DefineCommands()
    {
        this.m_commands = new HashMap<>();
        this.m_commands.put("PING", this.m_peer::Ping);
        this.m_commands.put("PONG", this.m_peer::Pong);
        this.m_commands.put("FIND_NODE_REQUEST", this.m_peer::FindNodeRequest);
        this.m_commands.put("FIND_NODE_RESPONSE", this.m_peer::FindNodeResponse);
        this.m_commands.put("FIND_VALUE", this.m_peer::FindValue);
        this.m_commands.put("STORE", this.m_peer::Store);
        this.m_commands.put("EXIT", this::Exit);
    }

    public void PrintInfo()
    {
        System.out.println("Peer (" + m_peer.m_id + ")\n----------------");
        System.out.format("%-25s [%d]\n", "Peer ID:", m_peer.m_id);
        System.out.format("%-25s [%s]\n", "Socket IP Address:", m_peer.m_socket.m_ip_address);
        System.out.format("%-25s [%d]\n", "Socket Port:", m_peer.m_socket.m_port);
        System.out.format("%-25s [%s]\n", "Bootstrapped IP Address:", (this.m_bootstrapped_ip == null) ? "" : this.m_bootstrapped_ip);
        System.out.format("%-25s [%s]\n", "Bootstrapped Port:", (this.m_bootstrapped_port == 0) ? "" : String.valueOf(this.m_bootstrapped_port));
        System.out.println("----------------");
    }

    public void StoreData(String[] tokens) throws NoSuchAlgorithmException, InterruptedException
    {
        String key = tokens[0];
        String value = tokens[1];

        m_peer.AddDataItem(key, value);
    }

    public void GetData(String data_item)
    {
        boolean is_integer = data_item.matches("-?(0|[1-9]\\d*)");
        Optional<String> value;

        if(is_integer)
            value = m_peer.GetDataItem(new BigInteger(data_item));
        else value = m_peer.GetDataItem(data_item);

        if(value.isEmpty())
            System.out.println("~ Data item not present");
        else System.out.println(value.get());
    }

    public void run()
    {
        try
        {
            if(m_port != 0)
                InitPeer(m_port);
            else InitPeer();

            Lib.Pair<Lib.Pair<String, Integer>, byte[]> current_input = null;
            while (m_peer != null) {
                switch (this.m_state) {
                    case 0 -> {
                        current_input = m_peer.GetNextReceived();

                        if(!m_bootstrapped)
                            this.m_state = (current_input == null) ? 0 : 2;
                    }
                    case 1 -> {
                        m_peer.Send(this.m_bootstrapped_ip, this.m_bootstrapped_port, ("FIND_NODE_REQUEST" + " " + m_peer.m_id).getBytes());
                        this.m_state = 0;
                        this.m_bootstrapped = false;
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

    private void HandleCommand(Lib.Pair<Lib.Pair<String, Integer>, byte[]> request) throws IOException, InterruptedException, ClassNotFoundException, NoSuchAlgorithmException
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

        this.m_state = 0;
    }

    private void InitPeer(int port) throws IOException, NoSuchAlgorithmException
    {
        m_peer = new Peer(port);
        m_peer.DefineSenderAndReceiver();
        DefineCommands();
    }

    private void InitPeer() throws IOException, NoSuchAlgorithmException
    {
        m_peer = new Peer();
        m_peer.DefineSenderAndReceiver();
        DefineCommands();
        m_port = m_peer.m_socket.m_port;
    }

    private void Exit(Peer.RoutingTableEntry peer_info, byte[] message)
    {
        m_peer.Close();
        m_peer = null;
        System.out.println("Waiting on Runner Services to finish");
    }

    private void CloseRemotePeer(Lib.Pair<String, Integer> peer) throws InterruptedException
    {
        m_peer.Send(peer.first, peer.second, ("EXIT").getBytes());
    }

    public void Close()
    {
        Exit(null, null);
    }

    public Peer GetPeer()
    {
        return m_peer;
    }

    private boolean m_bootstrapped;
    private int m_port;
    private int m_state;
    private Peer m_peer;
    private String m_bootstrapped_ip;
    private int m_bootstrapped_port;
    private HashMap<String, RPC> m_commands;
}
