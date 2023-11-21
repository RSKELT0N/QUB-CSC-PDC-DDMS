import java.net.Inet4Address;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Peer
{
    static class FingerTableEntry
    {
        int id;
        Node conn;
    }

    public Peer(int port) throws UnknownHostException, NoSuchAlgorithmException, SocketException
    {
        String hash_value = Inet4Address.getLocalHost().toString() + ":" + port;
        this.m_id = Lib.SHA1(hash_value, Integer.MAX_VALUE);
        this.m_socket = new Node(port);
    }

    private int m_id;
    private Node m_socket;
    private FingerTableEntry[] m_finger_table;
}
