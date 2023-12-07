package core.peer;

import core.Lib;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

class Receiver extends Runner
{
    public static class Packet
    {
        public HashMap<Peer.RoutingTableEntry, byte[]> m_payloads;

        Packet()
        {
            this.m_payloads = new HashMap<>();
        }

        public void AddPayload(Peer.RoutingTableEntry conn, byte[] payload)
        {
            m_payloads.put(conn, payload);
        }
    }

    Receiver(Peer receive_peer)
    {
        super();
        this.m_receiver_peer = receive_peer;
        this.m_receiver = receive_peer.GetSocket();
        this.m_received = new HashMap<>();
    }

    @Override
    public void run()
    {
        while(this.m_running)
        {
            try {
                Toggle();
                DatagramPacket packet = this.m_receiver.ReceivePacket();
                Peer.RoutingTableEntry conn = new Peer.RoutingTableEntry(packet.getAddress().getHostAddress(), packet.getPort());

                InsertValidPacket(conn, packet.getData());
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException | RuntimeException | InterruptedException e) {
                break;
            }
        }
    }

    public void AddReceivedItem(BigInteger command_count)
    {
        if(!(m_received.containsKey(command_count)))
        {
            m_received.put(command_count, new Lib.Pair<>(new Semaphore(0), new Packet()));
        }
    }

    public HashMap<BigInteger, Lib.Pair<Semaphore, Packet>> GetReceivedPackets()
    {
        return m_received;
    }

    private void InsertValidPacket(Peer.RoutingTableEntry conn, byte[] payload) throws InterruptedException
    {
        byte[] prefix = Arrays.copyOf(payload, 4);
        byte[] magic_byte = IntToByteArray(MAGIC_VALUE);

        if(Arrays.equals(prefix, magic_byte))
        {
            byte[] packet = Arrays.copyOfRange(payload, 4, payload.length);
            AddPayload(conn, packet);
            m_receiver_peer.AddReceiveItem(conn, packet);
        }
    }

    private void AddPayload(Peer.RoutingTableEntry conn, byte[] payload)
    {
        String[] tokens = new String(payload).replace("\0", "").split(" ");
        BigInteger command_count = new BigInteger(tokens[0].split(":")[1]);

        if(m_received.containsKey(command_count))
        {
            if (m_received.get(command_count).first.getQueueLength() == 1)
            {
                m_received.get(command_count).first.release();
            }
            m_received.get(command_count).second.AddPayload(conn, payload);
        }
    }

    private final Peer m_receiver_peer;
    private final core.peer.Node m_receiver;
    private final HashMap<BigInteger, Lib.Pair<Semaphore, Packet>> m_received;
}
