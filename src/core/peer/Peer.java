package core.peer;

import core.Lib;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Peer
{
    static public class RoutingTableEntry implements Serializable
    {
        public int id;
        public int port;
        public String ip_address;

        public RoutingTableEntry(int id, String ip_address, int port)
        {
            this.id = id;
            this.ip_address = ip_address;
            this.port = port;
        }

        public RoutingTableEntry(String ip_address, int port)
        {
            this.id = 0;
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
        this.m_m_bits = 8;

        DefineUDPSocket(port);
        String hash_value = InetAddress.getLocalHost().getHostAddress() + ":" + this.m_socket.m_port;
        this.m_id = Lib.SHA1(hash_value, 1<<m_m_bits);

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



    public void Ping(RoutingTableEntry peer_info, byte[] message) throws InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replace("\0", "").split(" ");
        assert message_string_tokens.length == 1;

        InsertPeerIntoRoutingTable(Integer.parseInt(message_string_tokens[0]), peer_info.ip_address, peer_info.port);
        Send(peer_info.ip_address, peer_info.port, ("PONG" + " " + m_id).getBytes());
    }

    public void Pong(RoutingTableEntry peer_info, byte[] message)
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        assert message_string_tokens.length == 1;

        int peer_id = Integer.parseInt(message_string_tokens[0]);
        InsertPeerIntoRoutingTable(peer_id, peer_info.ip_address, peer_info.port);
    }

    public void FindNodeRequest(RoutingTableEntry peer_info, byte[] message) throws IOException, InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        assert message_string_tokens.length == 1;
        int peer_id = Integer.parseInt(message_string_tokens[0]);

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

        if(m_data_table.size() == 0)
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

    private RoutingTableEntry[] GetClosePeers(int peer_id)
    {
        int total = GetTotalPeersInRoutingTable(peer_id);
        boolean take_all = total <= m_m_bits;

        ArrayList<RoutingTableEntry> all_peers = new ArrayList<>();
        ArrayList<RoutingTableEntry> remaining_peers = new ArrayList<>();

        for(int i = 0; i < m_m_bits; i++)
        {
            Lib.Pair<RoutingTableEntry[], RoutingTableEntry[]> bucket_peers = GetClosePeersFromBucket(peer_id, 1<<i, take_all);
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

    private Lib.Pair<RoutingTableEntry[], RoutingTableEntry[]> GetClosePeersFromBucket(int peer_id, int bucket_id, boolean take_all)
    {
        ArrayList<RoutingTableEntry> peers = new ArrayList<>();
        ArrayList<RoutingTableEntry> remaining_peers = new ArrayList<>();
        AtomicInteger closest_peer_dist = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger closest_peer = new AtomicInteger(-1);
        
        this.m_routing_table.get(bucket_id).forEach((id, peer) ->
        {
            if(!take_all)
            {
                if(Distance(peer_id, id) < closest_peer_dist.get() && id != peer_id)
                {
                    closest_peer_dist.set(Distance(peer_id, id));
                    closest_peer.set(id);
                }
            } else peers.add(peer);
            remaining_peers.add(peer);
        });

        if(!take_all && closest_peer.get() != -1)
            peers.add(this.m_routing_table.get(bucket_id).get(closest_peer.get()));

        for (RoutingTableEntry peer : peers)
            remaining_peers.remove(peer);

        return new Lib.Pair<>(peers.isEmpty() ? null : peers.toArray(new RoutingTableEntry[peers.size()]),
                              remaining_peers.isEmpty() ? null : remaining_peers.toArray(new RoutingTableEntry[remaining_peers.size()]));
    }

    private int GetRemainingClosestPeers(int peer_id, ArrayList<RoutingTableEntry> remaining_peers)
    {
        int curr_closest_peer_dist = Integer.MAX_VALUE;
        int curr_closest_idx = -1;
        for (int i = 0; i < remaining_peers.size(); i++)
        {
            if (Distance(peer_id, remaining_peers.get(i).id) < curr_closest_peer_dist && remaining_peers.get(i).id != peer_id) {
                curr_closest_peer_dist = Distance(peer_id, remaining_peers.get(i).id);
                curr_closest_idx = i;
            }
        }
        return curr_closest_idx;
    }

    private int GetTotalPeersInRoutingTable(int peer_id)
    {
        AtomicInteger sum = new AtomicInteger();

        this.m_routing_table.forEach((bucket_id, bucket) ->
        {
            int bias = 0;
            if(bucket.get(peer_id) != null)
                bias = -1;
            
            sum.addAndGet(bucket.size() + bias);
        });

        return sum.get();
    }

    private int Distance(int peer_id_1, int peer_id_2)
    {
        return peer_id_1 ^ peer_id_2;
    }

    private int DetermineBucket(int id)
    {
        int min = Distance(1, id);
        int min_pow = 1;

        for(int i = 0; i < m_m_bits; i++)
        {
            if(Distance(1<<i, id) < min)
            {
                min = Distance(1<<i, id);
                min_pow = 1 << i;
            }
        }
        return min_pow;
    }

    private void InsertPeerIntoRoutingTable(int peer_id, String remote_ip, int remote_port)
    {
        int bucket = DetermineBucket(peer_id);
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
            this.m_routing_table.put(1<<i, new TreeMap<>());
    }

    private void DefineDataTable()
    {
        this.m_data_table = new TreeMap<>();
    }

    public final int m_id;
    public final int m_m_bits;
    public core.peer.Node m_socket;

    public Sender m_sender;
    public Receiver m_receiver;
    public NavigableMap<Integer, NavigableMap<Integer, RoutingTableEntry>> m_routing_table;
    public NavigableMap<Integer, String> m_data_table;
    private final LinkedBlockingQueue<Lib.Pair<Lib.Pair<String, Integer>, byte[]>> m_received;

}
