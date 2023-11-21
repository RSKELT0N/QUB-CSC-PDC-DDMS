package core.peer;

import core.Lib;

import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;

public class Peer
{
    static class FingerTableEntry
    {
        int id;
        String ip_address;
        int port;

        FingerTableEntry(int id, String ip_address, int port)
        {
            this.id = id;
            this.ip_address = ip_address;
            this.port = port;
        }
    }


    public Peer(int port) throws UnknownHostException, NoSuchAlgorithmException, SocketException
    {
        this.m_received = new LinkedBlockingQueue<>();
        this.m_m_bits = 20;
        String hash_value = InetAddress.getLocalHost().toString() + ":" + port;

        this.m_id = Lib.SHA1(hash_value, this.m_m_bits);
        DefineUDPSocket(port);
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
        for(int i = 0; i < m_m_bits; i++)
        {
            System.out.println("(" + i + "): " +
                               this.m_finger_table[i].id + ", " +
                               this.m_finger_table[i].ip_address + ", " +
                               this.m_finger_table[i].port);
        }
    }

    public void AddReceiveItem(Lib.Pair<String, Integer> r, String s) throws InterruptedException
    {
        this.m_received.put(new Lib.Pair<>(new Lib.Pair<>(r.first, r.second), s));
    }

    private void DefineSenderAndReceiver()
    {
        this.m_sender = new Sender(this.GetSocket());
        this.m_receiver = new Receiver(this);

        Thread sender = new Thread(this.m_sender);
        Thread receiver = new Thread(this.m_receiver);
        sender.start();
        receiver.start();
    }

    private void DefineUDPSocket(int port) throws SocketException, UnknownHostException
    {
        try
        {
            this.m_socket = new core.peer.Node(port);
        }
        catch(BindException e)
        {
            System.err.println("Ensure the IP address and port is not currently opened (" +
                               InetAddress.getLocalHost().getHostAddress() + ", " +
                               port + ")");
            System.exit(-1);
        }
    }

    private void DefineFingerTable()
    {
        this.m_finger_table = new FingerTableEntry[this.m_m_bits];

        for(int i = 0; i < this.m_m_bits; i++)
        {
            this.m_finger_table[i] = new FingerTableEntry(this.m_id,
                                                          this.m_socket.m_ip_address,
                                                          this.m_socket.m_port);
        }
    }

    private core.peer.Node m_socket;
    private final int m_id;
    private final int m_m_bits;

    private Sender m_sender;
    private Receiver m_receiver;
    private FingerTableEntry[] m_finger_table;
    private final LinkedBlockingQueue<Lib.Pair<Lib.Pair<String, Integer>, String>> m_received;

}
