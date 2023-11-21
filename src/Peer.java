import java.net.*;
import java.security.NoSuchAlgorithmException;

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
        this.m_m_bits = 20;
        String hash_value = Inet4Address.getLocalHost().toString() + ":" + port;

        this.m_id = Lib.SHA1(hash_value, this.m_m_bits);
        DefineUDPSocket(port);
        DefineFingerTable();
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

    public void DefineUDPSocket(int port) throws SocketException, UnknownHostException
    {
        try
        {
            this.m_socket = new Node(port);
        }
        catch(BindException e)
        {
            System.err.println("Ensure the IP address and port is not currently opened (" +
                               Inet4Address.getLocalHost().getHostAddress() + ", " +
                               port + ")");
            System.exit(-1);
        }
    }

    public Node GetSocket()
    {
        return this.m_socket;
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

    private Node m_socket;
    private final int m_id;
    private final int m_m_bits;
    private FingerTableEntry[] m_finger_table;
}
