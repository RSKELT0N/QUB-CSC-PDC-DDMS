package core.peer;

import core.Lib;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Peer
{
    static public class RoutingTableEntry implements Serializable
    {
        public BigInteger id;
        public int port;
        public String ip_address;

        public RoutingTableEntry(BigInteger id, String ip_address, int port)
        {
            this.id = id;
            this.ip_address = ip_address;
            this.port = port;
        }

        public RoutingTableEntry(String ip_address, int port)
        {
            this.id = BigInteger.valueOf(0);
            this.ip_address = ip_address;
            this.port = port;
        }

        @Override
        public boolean equals(Object in)
        {
            if (!(in instanceof RoutingTableEntry)) {
                return false;
            }

            RoutingTableEntry e = (RoutingTableEntry)in;
            return this.id == e.id &&
                   this.port == e.port &&
                   this.ip_address.equals(e.ip_address);
        }
    }

    public Peer(int port) throws IOException, NoSuchAlgorithmException
    {
        this.m_received = new LinkedBlockingQueue<>();
        this.m_m_bits = 160;

        DefineUDPSocket(port);
        String hash_value = InetAddress.getLocalHost().getHostAddress() + ":" + this.m_socket.m_port;
        this.m_id = Lib.SHA1(hash_value, BigInteger.valueOf(1).shiftLeft(m_m_bits));

        DefineRoutingTable();
        DefineDataTable();
    }

    public void DefineSenderAndReceiver()
    {
        this.m_sender = new Sender(this.GetSocket());
        this.m_receiver = new Receiver(this);

        Thread sender = new Thread(this.m_sender);
        Thread receiver = new Thread(this.m_receiver);
        receiver.start();
        sender.start();
    }

    public void Store(RoutingTableEntry peer_info, byte[] message) throws NoSuchAlgorithmException, InterruptedException
    {
        String key_value = new String(message, StandardCharsets.UTF_8).replace("\0", "");
        String[] key_value_tokens = key_value.split(",");
        String key = key_value_tokens[0];
        String value = key_value_tokens[1];

        BigInteger key_hash = Lib.SHA1(key, BigInteger.valueOf(1).shiftLeft(m_m_bits));

        if(!(m_data_table.containsKey(key_hash)))
        {
            m_data_table.put(key_hash, value);

            RoutingTableEntry[] close_peers = GetClosePeers(m_id);
            byte[] to_send = ("STORE" + " " + key + "," + value).getBytes();

            for(var peer : close_peers)
                Send(peer.ip_address, peer.port, to_send);
        }
    }

    public void Ping(RoutingTableEntry peer_info, byte[] message) throws InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replace("\0", "").split(" ");
        assert message_string_tokens.length == 1;

        InsertPeerIntoRoutingTable(new BigInteger(message_string_tokens[0]), peer_info.ip_address, peer_info.port);
        Send(peer_info.ip_address, peer_info.port, ("PONG" + " " + m_id).getBytes());
    }

    public void Pong(RoutingTableEntry peer_info, byte[] message)
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        assert message_string_tokens.length == 1;

        BigInteger peer_id = new BigInteger(message_string_tokens[0]);
        InsertPeerIntoRoutingTable(peer_id, peer_info.ip_address, peer_info.port);
    }

    public void FindNodeRequest(RoutingTableEntry peer_info, byte[] message) throws IOException, InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        assert message_string_tokens.length == 1;
        BigInteger peer_id = new BigInteger(message_string_tokens[0]);

        RoutingTableEntry[] close_peers = GetClosePeers(peer_id);
        InsertPeerIntoRoutingTable(peer_id, peer_info.ip_address, peer_info.port);

        byte[] initial_command = ("FIND_NODE_RESPONSE ").getBytes();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);

        oos.writeObject(close_peers);
        oos.flush();

        byte[] to_send = new byte[initial_command.length + out.size()];
        ByteBuffer buff = ByteBuffer.wrap(to_send);
        buff.put(initial_command);
        buff.put(out.toByteArray());
        byte[] combined = buff.array();

        Send(peer_info.ip_address, peer_info.port, combined);
    }

    public void FindNodeResponse(RoutingTableEntry peer_info, byte[] message) throws IOException, ClassNotFoundException, InterruptedException
    {
        ByteArrayInputStream in = new ByteArrayInputStream(message);
        ObjectInputStream iis = new ObjectInputStream(in);

        Send(peer_info.ip_address, peer_info.port, ("PING" + " " + this.m_id).getBytes());
        RoutingTableEntry[] peers = (RoutingTableEntry[]) iis.readObject();

        for(var peer : peers)
        {
            Send(peer.ip_address, peer.port, ("PING" + " " + this.m_id).getBytes());
        }
    }

    public void Close()
    {
        this.m_sender.SetState(false);
        this.m_receiver.SetState(false);
        this.m_socket.m_socket.close();
    }

    public void AddDataItem(String key, String value) throws NoSuchAlgorithmException, InterruptedException
    {
        this.m_data_table.put(Lib.SHA1(key, BigInteger.valueOf(1).shiftLeft(m_m_bits)), value);

        RoutingTableEntry[] close_peers = GetClosePeers(m_id);
        byte[] to_send = ("STORE" + " " + key + "," + value).getBytes();

        for(var peer : close_peers)
            Send(peer.ip_address, peer.port, to_send);
    }

    public Lib.Pair<Lib.Pair<String, Integer>, byte[]> GetNextReceived() throws InterruptedException
    {
        return this.m_received.poll(1000, TimeUnit.MILLISECONDS);
    }

    public final core.peer.Node GetSocket()
    {
        return this.m_socket;
    }

    public void Send(String remote_ip, int remote_port, byte[] message) throws InterruptedException
    {
        this.m_sender.AddSendItem(new Lib.Pair<>(remote_ip, remote_port), message);
    }

    public void PrintRoutingTable()
    {
        System.out.println("Peer (" + m_id + ") routing table\n---------------");
        for(var bucket : m_routing_table.entrySet())
        {
            var bucket_values = bucket.getValue();
            System.out.print("(" + bucket.getKey() + ") [" + bucket_values.size() + " " + "Peers] |");
            for(var peer : bucket_values.entrySet())
            {
                System.out.print(" " + peer.getKey());
            }
            System.out.println();
        }
        System.out.println("---------------");
    }

    public void PrintDataTable()
    {
        System.out.println("Peer (" + m_id + ") data table\n---------------");

        if(m_data_table.isEmpty())
        {
            System.out.println("No data entries");
            return;
        }

        for(var data : m_data_table.entrySet())
        {
            System.out.println(data.getKey() + " | " + data.getValue());
        }
        System.out.println("---------------");
    }

    public void AddReceiveItem(Lib.Pair<String, Integer> r, byte[] s) throws InterruptedException
    {
        this.m_received.put(new Lib.Pair<>(new Lib.Pair<>(r.first, r.second), s));
    }

    public String FormatJoin(int id, String ip, int port)
    {
        return id + "," + ip + "," + port;
    }

    private RoutingTableEntry[] GetClosePeers(BigInteger peer_id)
    {
        BigInteger total = GetTotalPeersInRoutingTable(peer_id);
        boolean take_all = total.compareTo(BigInteger.valueOf(m_m_bits)) <= 0;

        ArrayList<RoutingTableEntry> all_peers = new ArrayList<>();
        ArrayList<RoutingTableEntry> remaining_peers = new ArrayList<>();

        for(int i = 0; i < m_m_bits; i++)
        {
            Lib.Pair<RoutingTableEntry[], RoutingTableEntry[]> bucket_peers = GetClosePeersFromBucket(peer_id, BigInteger.valueOf(1).shiftLeft(i), take_all);
            if(bucket_peers.first != null) {
                all_peers.addAll(Arrays.asList(bucket_peers.first));
            }

            if(bucket_peers.second != null) {
                remaining_peers.addAll(Arrays.asList(bucket_peers.second));
            }
        }

        // Get remaining peers based on closest to equal total.
        if(!take_all && all_peers.size() < m_m_bits)
        {
            int diff = Math.abs(all_peers.size() - m_m_bits);

            while(diff > 0)
            {
                int curr_closest_peer_idx = GetRemainingClosestPeers(peer_id, remaining_peers);

                all_peers.add(remaining_peers.get(curr_closest_peer_idx));
                remaining_peers.remove(curr_closest_peer_idx);
                diff--;
            }
        }

        return all_peers.toArray(new RoutingTableEntry[all_peers.size()]);
    }

    private Lib.Pair<RoutingTableEntry[], RoutingTableEntry[]> GetClosePeersFromBucket(BigInteger peer_id, BigInteger bucket_id, boolean take_all)
    {
        ArrayList<RoutingTableEntry> peers = new ArrayList<>();
        ArrayList<RoutingTableEntry> remaining_peers = new ArrayList<>();
        BigInteger closest_peer_dist = BigInteger.valueOf(Integer.MAX_VALUE);
        BigInteger closest_peer = BigInteger.valueOf(-1);

        for(var peer : this.m_routing_table.get(bucket_id).entrySet())
        {
            if(!take_all)
            {
                if(Distance(peer_id, peer.getKey()).compareTo(closest_peer_dist) < 0 && !(peer.getKey().equals(peer_id)))
                {
                    closest_peer_dist = Distance(peer_id, peer.getKey());
                    closest_peer = peer.getKey();
                }
            } else peers.add(peer.getValue());
            remaining_peers.add(peer.getValue());
        }

        if(!take_all && !(closest_peer.equals(BigInteger.valueOf(-1))))
            peers.add(this.m_routing_table.get(bucket_id).get(closest_peer));

        for (RoutingTableEntry peer : peers)
            remaining_peers.remove(peer);

        return new Lib.Pair<>(peers.isEmpty() ? null : peers.toArray(new RoutingTableEntry[peers.size()]),
                              remaining_peers.isEmpty() ? null : remaining_peers.toArray(new RoutingTableEntry[remaining_peers.size()]));
    }

    private int GetRemainingClosestPeers(BigInteger peer_id, ArrayList<RoutingTableEntry> remaining_peers)
    {
        BigInteger curr_closest_peer_dist = BigInteger.valueOf(Integer.MAX_VALUE);
        int curr_closest_idx = -1;
        for (int i = 0; i < remaining_peers.size(); i++)
        {
            if (Distance(peer_id, remaining_peers.get(i).id).compareTo(curr_closest_peer_dist) < 0 && remaining_peers.get(i).id != peer_id) {
                curr_closest_peer_dist = Distance(peer_id, remaining_peers.get(i).id);
                curr_closest_idx = i;
            }
        }
        return curr_closest_idx;
    }

    private BigInteger GetTotalPeersInRoutingTable(BigInteger peer_id)
    {
        BigInteger sum = BigInteger.valueOf(0);

        for(var bucket : this.m_routing_table.entrySet())
        {
            int bias = 0;
            if(bucket.getValue().get(peer_id) != null)
                bias = -1;

            sum = sum.add(BigInteger.valueOf(bucket.getValue().size()).add(BigInteger.valueOf(bias)));
        }

        return sum;
    }

    private BigInteger Distance(BigInteger peer_id_1, BigInteger peer_id_2)
    {
        return peer_id_1.xor(peer_id_2);
    }

    private BigInteger DetermineBucket(BigInteger id)
    {
        BigInteger min = Distance(BigInteger.valueOf(1), id);
        BigInteger min_pow = BigInteger.valueOf(1);

        for(int i = 0; i < m_m_bits; i++)
        {
            if(Distance(BigInteger.valueOf(1).shiftLeft(i), id).compareTo(min) < 0)
            {
                min = Distance(BigInteger.valueOf(1).shiftLeft(i), id);
                min_pow = BigInteger.valueOf(1).shiftLeft(i);
            }
        }
        return min_pow;
    }

    private void InsertPeerIntoRoutingTable(BigInteger peer_id, String remote_ip, int remote_port)
    {
        BigInteger bucket = DetermineBucket(peer_id);
        this.m_routing_table.get(bucket).put(peer_id, new RoutingTableEntry(peer_id, remote_ip, remote_port));
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

    private void DefineRoutingTable()
    {
        this.m_routing_table = new TreeMap<>();

        for(int i = 0; i < this.m_m_bits; i++)
            this.m_routing_table.put(BigInteger.valueOf(1).shiftLeft(i), new TreeMap<>());
    }

    private void DefineDataTable()
    {
        this.m_data_table = new TreeMap<>();
    }

    public final BigInteger m_id;
    public final int m_m_bits;
    public core.peer.Node m_socket;

    public Sender m_sender;
    public Receiver m_receiver;
    public NavigableMap<BigInteger, NavigableMap<BigInteger, RoutingTableEntry>> m_routing_table;
    public NavigableMap<BigInteger, String> m_data_table;
    private final LinkedBlockingQueue<Lib.Pair<Lib.Pair<String, Integer>, byte[]>> m_received;

}
