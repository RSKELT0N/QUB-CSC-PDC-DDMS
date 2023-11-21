package core;

import core.peer.Peer;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class Chord implements Remote
{
    public Chord(int port)
    {
        this.m_state = 0;
        this.m_port = port;
        DefineCommands();
    }

    public Chord(int port, String bootstrapped_ip, int bootstrapped_port)
    {
        this.m_state = 1;
        this.m_port = port;
        this.m_bootstrapped_ip = bootstrapped_ip;
        this.m_bootstrapped_port = bootstrapped_port;
        DefineCommands();
    }

    public Peer GetPeer()
    {
        return this.m_peer;
    }

    public void CreatePeer() throws SocketException, UnknownHostException, NoSuchAlgorithmException, InterruptedException
    {
        this.m_peer = new Peer(this.m_port);
        m_peer.PrintFingerTable();
        this.m_predecessor = this.m_peer;
        this.m_successor = this.m_peer;
        Run();
    }

    private void DefineCommands()
    {
        this.m_commands = new HashMap<>();
        this.m_commands.put("JOIN", this::Join);
        this.m_commands.put("PING", this::Ping);
        this.m_commands.put("EXIT", this::Exit);
    }

    private void Run() throws InterruptedException, UnknownHostException
    {
        Lib.Pair<Lib.Pair<String, Integer>, String> current_input = null;
        while(m_peer != null)
        {
            switch (this.m_state)
            {
                case 0 -> {
                    current_input = this.m_peer.GetNextReceived();
                    this.m_state = 2;
                }
                case 1 -> {
                    this.m_peer.Send(new Lib.Pair<>(this.m_bootstrapped_ip, this.m_bootstrapped_port), "EXIT");
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

    private void HandleCommand(Lib.Pair<Lib.Pair<String, Integer>, String> request) throws UnknownHostException, InterruptedException
    {
        int port = request.first.second;
        InetAddress ip_address = InetAddress.getByName(request.first.first);
        String[] tokens = request.second.split(" ");

        if(!this.m_commands.containsKey(tokens[0]))
        {
            this.m_peer.Send(new Lib.Pair<>(ip_address.getHostAddress(), port), "EXIT");
        }
        else
        {
            var command = this.m_commands.get(tokens[0]);
            command.Parse(request.second);
        }

        this.m_state = 0;
    }

    private void Ping(String s)
    {

    }

    private void Join(String s)
    {

    }

    private void Exit(String s)
    {
        System.out.println("Someone closed me");
        this.m_peer.Close();
        this.m_peer = null;
    }

    private int m_port;
    private Peer m_peer;
    private int m_state;
    private Peer m_successor;
    private Peer m_predecessor;
    private String m_bootstrapped_ip;
    private int m_bootstrapped_port;
    private HashMap<String, Command> m_commands;
}
