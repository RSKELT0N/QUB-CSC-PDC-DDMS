package core.peer;

import core.Lib;

import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Peer
{
    static public class RoutingTableEntry
    {
        public int peer_id;
        public int port;
        public String ip_address;

        public RoutingTableEntry(int peer_id, String ip_address, int port)
        {
            this.peer_id = peer_id;
            this.ip_address = ip_address;
            this.port = port;
        }
    }

    public Peer(int port) throws UnknownHostException, NoSuchAlgorithmException, SocketException
    {
        this.m_received = new LinkedBlockingQueue<>();
        this.m_m_bits = 8;

        DefineUDPSocket(port);
        String hash_value = InetAddress.getLocalHost().getHostAddress() + ":" + this.m_socket.m_port;
        this.m_id = Lib.SHA1(hash_value, (int) Math.pow(2, this.m_m_bits));

        DefineFingerTable();
        DefineSenderAndReceiver();
    }

    public void Close()
    {
        this.m_sender.SetState(false);
        this.m_receiver.SetState(false);
        this.m_socket.m_socket.close();
    }

    public Lib.Pair<Lib.Pair<String, Integer>, String> GetNextReceived() throws InterruptedException
    {
        return this.m_received.poll(1000, TimeUnit.MILLISECONDS);
    }

    public final core.peer.Node GetSocket()
    {
        return this.m_socket;
    }

    public void Send(Lib.Pair<String, Integer> p, String s) throws InterruptedException
    {
        this.m_sender.AddSendItem(p, s);
    }

    public void PrintRoutingTable()
    {
        System.out.println("Peer (" + m_id + ") routing table\n---------------");
        for(var entry : m_routing_table.entrySet())
        {
            System.out.println("(" + entry.getKey() + ") | " + "Peer: [" + entry.getValue().peer_id + "] IP: [" + entry.getValue().ip_address + "] Port: [" + entry.getValue().port + "]");
        }
        System.out.println("---------------");
    }

    void AddReceiveItem(Lib.Pair<String, Integer> r, String s) throws InterruptedException
    {
        this.m_received.put(new Lib.Pair<>(new Lib.Pair<>(r.first, r.second), s));
    }

    private void DefineSenderAndReceiver()
    {
        this.m_sender = new Sender(this.GetSocket());
        this.m_receiver = new Receiver(this);

        Thread sender = new Thread(this.m_sender);
        Thread receiver = new Thread(this.m_receiver);
        receiver.start();
        sender.start();
    }

    private void DefineUDPSocket(int port) throws SocketException, UnknownHostException
    {
        try
        {
            this.m_socket = new core.peer.Node(port);
        }
        catch(BindException e)
        {
            System.err.println("Ensure the IP address and port is not currently opened (" + InetAddress.getLocalHost().getHostAddress() + ")");
            System.exit(-1);
        }
    }

    private void DefineFingerTable()
    {
        this.m_routing_table = new TreeMap<>();

        for(int i = 0; i < this.m_m_bits; i++)
        {
            this.m_routing_table.put((int) Math.pow(2, i), new RoutingTableEntry(m_id, m_socket.m_ip_address, m_socket.m_port));
        }
    }

    public final int m_id;
    public final int m_m_bits;
    public core.peer.Node m_socket;

    public Sender m_sender;
    public Receiver m_receiver;
    public NavigableMap<Integer, RoutingTableEntry> m_routing_table;
    private final LinkedBlockingQueue<Lib.Pair<Lib.Pair<String, Integer>, String>> m_received;

}
