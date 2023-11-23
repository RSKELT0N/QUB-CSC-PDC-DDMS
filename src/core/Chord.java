package core;

import core.peer.Peer;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Chord implements Remote, Runnable
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

    public void Close()
    {
        m_peer.Close();
        m_peer = null;
    }

    public Peer GetPeer()
    {
        return m_peer;
    }

    public void CreatePeer()
    {
        new Thread(this).start();
    }

    private void DefineCommands()
    {
        this.m_commands = new HashMap<>();
        this.m_commands.put("JOIN", this::Join);
        this.m_commands.put("PING", this::Ping);
        this.m_commands.put("EXIT", this::Exit);
    }

//    private void ReachOutToPeers() throws InterruptedException
//    {
//        var peer = m_peer.m_peer_id_table.firstEntry();
//
//        while(peer != null)
//        {
//            if(peer.getKey() != m_peer.m_id)
//            {
//                m_peer.Send(peer.getValue(), "JOIN" + " " + FormatJoin(m_peer.m_id, m_peer.m_socket.m_ip_address, m_peer.m_socket.m_port) + " " + "END");
//            }
//            peer = m_peer.m_peer_id_table.higherEntry(peer.getKey());
//
//        }
//    }

//    private void UpdatePeerTable(String[] tokens)
//    {
//        for(int i = 1; i < tokens.length - 1; i++)
//        {
//            String[] peer_info = tokens[i].split(",");
//            int peer_id = Integer.parseInt(peer_info[0]);
//            String peer_ip = peer_info[1];
//            int peer_port = Integer.parseInt(peer_info[2]);
//
//            m_peer.m_peer_id_table.put(peer_id, new Lib.Pair<>(peer_ip, peer_port));
//        }
//    }

//    private void UpdateFingerTable(String[] tokens)
//    {
//        for(var entry : m_peer.m_finger_table.entrySet())
//        {
//            Peer.RoutingTableEntry curr_table = entry.getValue();
//
//            int updated_peer_id = m_peer.GetSuccessor(entry.getValue().data_id);
//
//            if(updated_peer_id != entry.getValue().peer_id)
//            {
//                curr_table.peer_id = updated_peer_id;
//                curr_table.ip_address = entry.getValue().ip_address;
//                curr_table.port = entry.getValue().port;
//                m_peer.m_finger_table.put(entry.getKey(), curr_table);
//            }
//        }
//    }

    private int Distance(int peer_id_1, int peer_id_2)
    {
        return peer_id_1 ^ peer_id_2;
    }

    private void SendKnownPeersToRemote(String ip, int port, HashSet<Peer.RoutingTableEntry> peers) throws InterruptedException
    {
        StringBuilder to_send = new StringBuilder();
        to_send.append("JOIN");
        peers.forEach((peer) -> {
            to_send.append(" ").append(FormatJoin(peer.peer_id, peer.ip_address, peer.port));
        });
        to_send.append(" RECEIVE");

        m_peer.Send(new Lib.Pair<>(ip, port), to_send.toString());
    }

    private HashSet<Peer.RoutingTableEntry> GatherAllPeersFromRoutingTable()
    {
        HashSet<Integer> peer_ids = new HashSet<>();
        HashSet<Peer.RoutingTableEntry> peers = new HashSet<>();

        m_peer.m_routing_table.forEach((exp, peer) -> {
            peer_ids.add(peer.peer_id);
        });

        m_peer.m_routing_table.forEach((exp, peer) -> {
            if(peer_ids.contains(peer.peer_id))
            {
                peers.add(peer);
                peer_ids.remove(peer.peer_id);
            }

        });
        return peers;
    }

    private void InsertPeersIntoRoutingTable(String[] peers)
    {
        for(var peer : peers)
        {
            String[] peer_tokens = peer.split(",");
            int peer_id = Integer.parseInt(peer_tokens[0]);
            String peer_ip = peer_tokens[1];
            int peer_port = Integer.parseInt(peer_tokens[2]);
            InsertPeerIntoRoutingTable(peer_id, peer_ip, peer_port);
        }
    }

    private void InsertPeerIntoRoutingTable(int peer_id, String remote_ip, int remote_port)
    {
        for(var row : m_peer.m_routing_table.entrySet())
        {
            if((Distance(row.getKey(), peer_id) < Distance(row.getKey(), row.getValue().peer_id)) || (row.getValue().peer_id == -1))
            {
                Peer.RoutingTableEntry swap = new Peer.RoutingTableEntry(peer_id, remote_ip, remote_port);
                m_peer.m_routing_table.put(row.getKey(), swap);
            }
        }
    }

    private String FormatJoin(int id, String ip, int port)
    {
        return id + "," + ip + "," + port;
    }

    public void run()
    {

        Lib.Pair<Lib.Pair<String, Integer>, String> current_input = null;

        try
        {
            m_peer = new Peer(this.m_port);
            this.m_peer.PrintRoutingTable();
            while (m_peer != null) {
                switch (this.m_state) {
                    case 0 -> {
                        current_input = m_peer.GetNextReceived();
                        this.m_state = (current_input == null) ? 0 : 2;
                    }
                    case 1 -> {
                        m_peer.Send(new Lib.Pair<>(this.m_bootstrapped_ip, this.m_bootstrapped_port), "JOIN " + FormatJoin(m_peer.m_id, m_peer.m_socket.m_ip_address, m_peer.m_socket.m_port) + " REQUEST");
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

    private void HandleCommand(Lib.Pair<Lib.Pair<String, Integer>, String> request) throws UnknownHostException, InterruptedException
    {
        int port = request.first.second;
        InetAddress ip_address = InetAddress.getByName(request.first.first);
        String[] tokens = request.second.split(" ");

        if(!this.m_commands.containsKey(tokens[0]))
        {
            this.CloseRemotePeer(new Lib.Pair<>(ip_address.getHostAddress(), port));
        }
        else
        {
            var command = this.m_commands.get(tokens[0]);
            command.Parse(request, tokens);
        }

        this.m_state = 0;
    }

    private void Ping(Lib.Pair<Lib.Pair<String, Integer>, String> request, String[] s)
    {

    }

    private void Join(Lib.Pair<Lib.Pair<String, Integer>, String> request, String[] s) throws InterruptedException
    {
        String last_token = s[s.length - 1];

        switch(last_token)
        {
            case "REQUEST" ->
            {
                if(s.length == 3)
                {
                    String[] remote_peer_tokens = s[1].split(",");
                    int remote_peer_id = Integer.parseInt(remote_peer_tokens[0]);
                    String remote_peer_ip = remote_peer_tokens[1];
                    int remote_peer_port = Integer.parseInt(remote_peer_tokens[2]);

                    HashSet<Peer.RoutingTableEntry> all_known_peers = GatherAllPeersFromRoutingTable();
                    SendKnownPeersToRemote(remote_peer_ip, remote_peer_port, all_known_peers);

                    InsertPeerIntoRoutingTable(remote_peer_id, request.first.first, request.first.second);
                    m_peer.PrintRoutingTable();

                } else this.CloseRemotePeer(request.first);
            }
            case "RECEIVE" ->
            {
                InsertPeersIntoRoutingTable(Arrays.copyOfRange(s, 1, s.length - 1));
                m_peer.PrintRoutingTable();
            }
            case "END" ->
            {
            }
        }
    }

    private void Exit(Lib.Pair<Lib.Pair<String, Integer>, String> request, String[] s)
    {
        m_peer.Close();
        m_peer = null;
    }

    private void CloseRemotePeer(Lib.Pair<String, Integer> peer) throws InterruptedException
    {
        m_peer.Send(peer, "EXIT");
    }

    private int m_port;
    private int m_state;
    private Peer m_peer;
    private String m_bootstrapped_ip;
    private int m_bootstrapped_port;
    private HashMap<String, DDMSCommand> m_commands;
}
