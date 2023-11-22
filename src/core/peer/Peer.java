package core.peer;

import core.Lib;

import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Peer
{
    static public class FingerTableEntry
    {
        public int port;
        public String ip_address;
        public int successor;
        public int predecessor;

        public FingerTableEntry(String ip_address, int port, int succ, int predec)
        {
            this.ip_address = ip_address;
            this.port = port;
            this.successor = succ;
            this.predecessor = predec;
        }
    }

    public Peer(int port) throws UnknownHostException, NoSuchAlgorithmException, SocketException
    {
        this.m_received = new LinkedBlockingQueue<>();
        this.m_m_bits = 100;

        DefineUDPSocket(port);

        String hash_value = InetAddress.getLocalHost().getHostAddress() + ":" + this.m_socket.m_port;
        this.m_id = Lib.SHA1(hash_value, this.m_m_bits);
        DefineFingerTable();
        DefineSenderAndReceiver();
    }

    public int GetSuccessor(int peer_id)
    {
        Integer succ = this.m_finger_table.higherKey(peer_id);

        if(succ == null)
        {
            succ = this.m_finger_table.firstKey();
        }
        return succ;
    }

    public int GetPredecessor(int peer_id)
    {
        Integer predec = this.m_finger_table.lowerKey(peer_id);

        if(predec == null)
        {
            predec = this.m_finger_table.lastKey();
        }
        return predec;
    }

    public void Close()
    {
        this.m_sender.SetState(false);
        this.m_receiver.SetState(false);
        this.m_socket.m_socket.close();
    }

    public Lib.Pair<Lib.Pair<String, Integer>, String> GetNextReceived() throws InterruptedException
    {
        return this.m_received.take();
    }

    public final core.peer.Node GetSocket()
    {
        return this.m_socket;
    }

    public void Send(Lib.Pair<String, Integer> p, String s) throws InterruptedException
    {
        this.m_sender.AddSendItem(p, s);
    }

    public void PrintFingerTable()
    {
        for(var entry : m_finger_table.entrySet())
        {
            System.out.println("(" + entry.getKey() + ") IP: [" + entry.getValue().ip_address + "] Port: [" + entry.getValue().port + "] Successor: [" + entry.getValue().successor + "] Predecessor: [" + entry.getValue().predecessor + "]");
        }
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
        this.m_finger_table = new TreeMap<>();

        this.m_finger_table.put(this.m_id, new FingerTableEntry(this.m_socket.m_ip_address, this.m_socket.m_port, this.m_id, this.m_id));
    }

    public final int m_id;
    public final int m_m_bits;
    public core.peer.Node m_socket;

    public Sender m_sender;
    public Receiver m_receiver;
    public NavigableMap<Integer, FingerTableEntry> m_finger_table;
    private final LinkedBlockingQueue<Lib.Pair<Lib.Pair<String, Integer>, String>> m_received;

}
