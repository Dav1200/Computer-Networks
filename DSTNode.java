// IN2011 Computer Networks
// Coursework 2022/2023
//
// Submission by
// DAVINDER SINGH
// 210022278
// davinder.singh@city.ac.uk

import javax.sound.midi.Soundbank;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DSTNode {

    private String fullnode;
    ServerSocket serverSocket;
    Set<String> networkDirectory;
    HashMap<String, String> storeValue;

    // Do not change the interface!
    public DSTNode(InetAddress host, int port, String id) {
        // Using the IP address, port number and identifier compute the node name and node ID.
        fullnode = host.getHostAddress() + "/" + port + "/" + id;
        networkDirectory = new HashSet<>();
        storeValue = new HashMap<>();
        createFileIfNotExists("StoreData.txt");
        storeValue = loadHashMapFromFile("StoreData.txt");

        //create node on network
        try {
            serverSocket = new ServerSocket(port);
            networkDirectory.add(fullnode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Do not change the interface!
    public void handleIncomingConnections(String startingNodeName) {
        AtomicReference<ArrayList<String>> visitedNodes = new AtomicReference<>(new ArrayList<>());
        visitedNodes.get().add(startingNodeName);
        //visitedNodes.get().remove(fullnode);
        Random rand = new Random();
        //pick random user to for active mapping
        int randNum = rand.nextInt(visitedNodes.get().size());

        //find all active nodes the scan timer can be adjusted
        Thread searchThread = new Thread(() -> {
            while (true) {
                //visitedNodes.get().remove(fullnode);

                visitedNodes.set(activeMap(visitedNodes.get().get(randNum), visitedNodes.get()));
                //visitedNodes.get().remove(fullnode);
                deleteDuplicates(visitedNodes.get());

                visitedNodes.set(FixNodes(visitedNodes.get()));
                //visitedNodes.get().remove(fullnode);
                //randomNumber[0] = random.nextInt(visitedNodes.get().size());
                try {
                    Thread.sleep(3000); // sleep for 1 seconds
                } catch (InterruptedException e) {
                    // thread was interrupted
                    return;
                }
            }
        });
        searchThread.start();


        deleteDuplicates(visitedNodes.get());
        //System.out.println(storeValue);
        System.out.println(startingNodeName);
        //visitedNodes.get().remove(fullnode);
        //Multithreading used to handle multiple clients
        while (true) {
            Socket socket = null;
            try {
                //the scokets accepts clients
                socket = serverSocket.accept();

                System.out.println("New client connected");
                visitedNodes.get().add(fullnode);
                deleteDuplicates(visitedNodes.get());
                //client connected handle the response

                try{
                    visitedNodes.set(FixNodes(visitedNodes.get()));}
                catch (IndexOutOfBoundsException e) {
                    //throw new RuntimeException(e);
                }

                //make a individual thread for the client
                System.out.println(visitedNodes);
                Socket finalSocket = socket;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            //set up input and output
                            OutputStream output = finalSocket.getOutputStream();
                            PrintWriter writer = new PrintWriter(output, true);
                            InputStream input = finalSocket.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                            writer.println("HELLO " + fullnode);
                            //Handle client response
                            String line;
                            boolean closeConnection = false;
                            int count = 0;
                            while (!closeConnection && (line = reader.readLine()) != null) {
                                System.out.println(line);
                                String[] messageParts = line.split(" ");

                                switch (messageParts[0]) {
                                    //chekc for what the client inputs and handle the response accordingly
                                    case "ALL":
                                        deleteDuplicates(visitedNodes.get());
                                        if (count == 0) {
                                            closeConnection = true;
                                            writer.println("BYE UNKNOWN COMMAND");
                                            break;
                                        }
                                        //visitedNodes.set(FixNodes(visitedNodes.get()));
                                        for(String s: visitedNodes.get()){
                                            writer.println(s);
                                        }
                                        break;

                                    case "HELLO":
                                        deleteDuplicates(visitedNodes.get());
                                        //check if hello message has been sent
                                        if (count == 1) {
                                            closeConnection = true;
                                            writer.println("BYE UNKNOWN COMMAND");
                                            break;
                                        }
                                        count = count + 1;

                                        if (messageParts.length == 2) {
                                            String regex = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/\\d+/.+(?!\\n)$";

                                            Pattern pattern = Pattern.compile(regex);
                                            Matcher matcher = pattern.matcher(messageParts[1]);
                                            //System.out.println(matcher.matches());
                                            if (messageParts[1].equals("ephemeral") || matcher.matches()) {
                                                String name = messageParts[1];
                                                //System.out.println("HELLO " + name);
                                                // Handle HELLO message with a name
                                                if (!messageParts[1].equals("ephemeral")) {

                                                    visitedNodes.updateAndGet(list -> {
                                                        list.add(messageParts[1]);
                                                        return list;
                                                    });
                                                    visitedNodes.set(FixNodes(visitedNodes.get()));
                                                    networkDirectory.add(messageParts[1]);
                                                }
                                                //System.out.println(networkDirectory);
                                            } else {
                                                closeConnection = true;
                                                writer.println("HELLO SHOULD BE FOLLOWED BY A VALID ID ");
                                            }
                                        } else {
                                            closeConnection = true;
                                            writer.println("HELLO SHOULD BE FOLLOWED BY A STATEMENT ");

                                        }
                                        break;

                                    //ping with pong
                                    case "PING":
                                        deleteDuplicates(visitedNodes.get());
                                        //check if hello is said
                                        if (count == 0) {
                                            closeConnection = true;
                                            writer.println("HELLO SHOULD START WITH HELLO");
                                            break;
                                        }
                                        // Handle PING request
                                        writer.println("PONG");
                                        break;

                                    case "STORE":
                                        deleteDuplicates(visitedNodes.get());
                                        if (count == 0) {//check if hello is said
                                            closeConnection = true;
                                            writer.println("HELLO SHOULD START WITH HELLO");
                                            break;
                                        }
                                        if (messageParts.length == 2) {
                                            if (isInteger(messageParts[1])) {
                                                StringBuilder sb = new StringBuilder();
                                                int storeCount = 0;

                                                while (storeCount != Integer.parseInt(messageParts[1])) {
                                                    String line2 = reader.readLine();
                                                    if (storeCount > 0) {
                                                        sb.append("\n");
                                                    }
                                                    sb.append(line2);
                                                    storeCount++;
                                                }
                                                String read = sb.toString();
                                                System.out.println(read);
                                                String key = sha(read);
                                                System.out.println(key);
                                                //might need to see if three closest nodes
                                                //visitedNodes.get().add(fullnode);
                                                ArrayList<String> binaryValue = visitedNodes.get();
                                                //binaryValue = FixNodes(binaryValue);
                                                binaryValue = containKey(binaryValue);
                                                HashMap<Integer, Integer> topThree = compareNodes(binaryValue, key);
                                                ArrayList<String> display3Nodes = new ArrayList<>();
                                                int countt = 0;
                                                //check the top 3 values returned
                                                for (Map.Entry<Integer, Integer> entry : topThree.entrySet()) {
                                                    //code to store the value  now to all the corresponding nodes
                                                    if (countt < 3) {
                                                        display3Nodes.add(visitedNodes.get().get(entry.getKey()));
                                                        countt++;
                                                    }
                                                }
                                                System.out.println(display3Nodes);
                                                if(display3Nodes.contains(fullnode)){
                                                    //store
                                                    display3Nodes.remove(fullnode);
                                                    store(display3Nodes,storeCount,read);
                                                    storeValue.put(key, sb.toString());
                                                    saveHashMapToFile(storeValue, "StoreData.txt");
                                                    writer.println("STORED " + key);
                                                }
                                                else {
                                                    writer.println("NOTSTORED");
                                                    //other nodes are closer
                                                }
                                                //check for closest nodes first, if current node is in the list store

                                                //System.out.println(key);

                                            } else {
                                                closeConnection = true;
                                                writer.println("STORE MUST FOLLOWED BY  NUMBER");
                                            }
                                        } else {
                                            closeConnection = true;
                                            writer.println("STORE MUST BE FOLLOWED BY NUMBER");
                                        }

                                        break;

                                    case "LOOKUP":
                                        deleteDuplicates(visitedNodes.get());
                                        if (count == 0) {//check if hello is said
                                            closeConnection = true;
                                            writer.println("HELLO SHOULD START WITH HELLO");
                                            break;
                                        }
                                        if (messageParts.length == 2) {
                                            if (storeValue.get(messageParts[1])!= null && messageParts[1].length() == 64) {
                                                int nn = storeValue.get(messageParts[1]).split("\n", -1).length ;
                                                writer.println("FOUND " + nn);
                                                writer.println(storeValue.get(messageParts[1]));
                                            } else {
                                                writer.println("NOTFOUND");
                                            }
                                        }
                                        else {
                                            writer.println("NOTFOUND");
                                        }
                                        break;

                                    case "FINDNEAREST":
                                        deleteDuplicates(visitedNodes.get());
                                        if (count == 0) {
                                            closeConnection = true;
                                            writer.println("HELLO SHOULD START WITH HELLO");
                                            break;
                                        }
                                        //visitedNodes.get().add(fullnode);
                                        if (messageParts.length == 2) {
                                            //check if length of hash is 64
                                            if (messageParts[1].length() == 64) {
                                                ArrayList<String> binaryValue = visitedNodes.get();
                                                //binaryValue = FixNodes(binaryValue);
                                                binaryValue = containKey(binaryValue);
                                                HashMap<Integer, Integer> topThree = compareNodes(binaryValue, messageParts[1]);
                                                //System.out.println(visitedNodes.get());
                                                //S//ystem.out.println(binaryValue);
                                                //System.out.println(topThree);
                                                ArrayList<String> display3Nodes = new ArrayList<>();
                                                int countt = 0;
                                                //check the top 3 values returned
                                                for (Map.Entry<Integer, Integer> entry : topThree.entrySet()) {
                                                    //code to store the value  now to all the corresponding nodes
                                                    if (countt < 3 ) {
                                                        display3Nodes.add(visitedNodes.get().get(entry.getKey()));
                                                        countt++;
                                                    }

                                                }
                                                //display the top 3 values
                                                writer.println("NODES " + countt);
                                                for (String s : display3Nodes) {
                                                    writer.println(s);
                                                }


                                            } else {
                                                closeConnection = true;
                                                writer.println("BYE KEY MUST BE 64 digits");
                                            }

                                        } else {
                                            closeConnection = true;
                                            writer.println("BYE Must be followed");
                                        }

                                        break;

                                    case "BYE":

                                        if (count == 0) {//check if hello is said
                                            closeConnection = true;
                                            writer.println("HELLO SHOULD START WITH HELLO");
                                            break;
                                        }
                                        // Handle BYE message

                                        writer.println("BYE CONNECTION CLOSED");
                                        closeConnection = true;
                                        break;
                                    default:
                                        writer.println("BYE UNKNOWN COMMAND");
                                        closeConnection = true;
                                        break;
                                }
                            }
                            //System.out.println("Client disconnected");
                            reader.close();
                            writer.close();
                            finalSocket.close();

                        }

                        catch ( ConcurrentModificationException e){
                            System.out.println("Testign Again");
                        }
                        catch (IndexOutOfBoundsException  e){
                            //System.out.println("Testing");
                        }

                        catch (IOException e) {
                            //throw new RuntimeException(e);
                        }

                    }

                }).start();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //store on the closest nodes
    public void store(ArrayList<String> list,int count, String text){
        //System.out.println(list);
        while (!list.isEmpty()){
            String[] partf = list.get(0).split("/");
            String ipsf = partf[partf.length - 3];
            String portsf = partf[partf.length - 2];

            try {
                //System.out.println(list.get(0));
//test if we need to store in the next avaiable node, if some are offline, could count --
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ipsf, Integer.parseInt(portsf)), 400);
                socket.setSoTimeout(100);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //System.out.println(in.readLine());
                out.println("HELLO ephemeral");
                out.println("STORE " + count);
                out.println(text);
                //System.out.println(numLines);
                //result = in.readLine();

                list.remove(0);
                //System.out.println(countt);
            } catch (IOException e) {
            }
        }}
    //get the closeset nodes looking at the key, binary list
    public HashMap<Integer, Integer> compareNodes(ArrayList<String> flistC, String s) {

        StringBuilder binaryStrings = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            String hex = s.substring(i, i + 1);
            int num = Integer.parseInt(hex, 16);
            String binary = Integer.toBinaryString(num);
            while (binary.length() < 4) {
                binary = "0" + binary;
            }
            binaryStrings.append(binary);
        }
        //System.out.println(s);
        s = binaryStrings.toString();

        //System.out.println(s);

        HashMap<Integer, Integer> topNodes = new HashMap<>();
        int count = 0;
        int fcount = 0;
        int Index = 0;
        // System.out.println(flistC);
        for (int i = 0; i < flistC.size(); i++) {
            //System.out.println(flistC.get(i));
            for (int j = 0; j < flistC.get(i).length(); j++) {
                if (flistC.get(i).charAt(j) == s.charAt(j)) {
                    count++;
                } else {
                    break;
                }
            }

            topNodes.put(i, 256 - count);
            if (count > fcount) {
                fcount = count;
                Index = i;
            }
            count = 0;
        }
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(topNodes.entrySet());
        Comparator<Map.Entry<Integer, Integer>> valueComparator = (e1, e2) -> e1.getValue().compareTo(e2.getValue());
        list.sort(valueComparator);

        // Create a new hashmap and add the sorted entries to it
        LinkedHashMap<Integer, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        //System.out.println(sortedMap);
//a
        return sortedMap;
    }

    //convert to sha then binary
    public ArrayList<String> containKey(ArrayList<String> fList) {
        ArrayList<String> binaryV = new ArrayList<>();
        for (int j = 0; j < fList.size(); j++) {
            String[] temp = fList.get(j).split("/");
            String id = fList.get(j);
            //System.out.println(id);
            id = sha(id);

            // System.out.println(id);
            StringBuilder binaryStrings = new StringBuilder();
            for (int i = 0; i < id.length(); i++) {
                String hex = id.substring(i, i + 1);
                int num = Integer.parseInt(hex, 16);
                String binary = Integer.toBinaryString(num);
                while (binary.length() < 4) {
                    binary = "0" + binary;
                }
                binaryStrings.append(binary);
            }
            binaryV.add(binaryStrings.toString());
        }
        //System.out.println(binaryV);
        return binaryV;
    }


    //get rid of corrupted nodes
    public ArrayList<String> FixNodes(ArrayList<String> visitedNodes) {
        ArrayList<String> finalarr = new ArrayList<>();
        for (int i = 0; i < visitedNodes.size(); i++) {
            String[] part = visitedNodes.get(i).split("/");
            String ips = part[part.length - 3];
            String ports = part[part.length - 2];
            try {
                if(visitedNodes.get(i).equals(fullnode)){
                    finalarr.add(visitedNodes.get(i));
                    continue;
                }
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ips, Integer.parseInt(ports)), 200);
                socket.setSoTimeout(100);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String result = in.readLine();
                out.println("HELLO " + fullnode);
                out.println("BYE checkComplete" );
                int count1 = result.length() - result.replace("/", "").length();
                if (count1 == 2) {
                    finalarr.add(result.substring(6));
                    deleteDuplicates(finalarr);
                }
            } catch (SocketTimeoutException e) {
                //visitedNodes.remove(i);
                // Handle timeout
                //System.out.println("Error: Connection attempt timed out");
                continue; // Skip to next iteration of loop
            } catch (IOException e) {
                //visitedNodes.remove(i);
                // Handle other I/O errors
                //System.out.println("Error: " + e.getMessage());
                continue; // Skip to next iteration of loop
            }
        }
        //finalarr.add(fullnode);
        return finalarr;
    }

    //check for active mapping
    public ArrayList<String> activeMap(String startingNodeName, ArrayList<String> visitedNodes) {
        //visitedNodes.remove(fullnode);
        String[] part = startingNodeName.split("/");
        String ips = part[part.length - 3];
        String ports = part[part.length - 2];
        Stack<String> Nodes = new Stack<>();
        ArrayList<String> dummyNodes = new ArrayList<>();
        //ArrayList<String> visitedNodes = new ArrayList<>();
        try {
            String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
            Random random = new Random();
            StringBuilder sb = new StringBuilder(26);
            for (int i = 0; i < 26; i++) {
                int randomInt = random.nextInt(ALPHABET.length());
                char randomChar = ALPHABET.charAt(randomInt);
                sb.append(randomChar);
            }

            String shaValue = sha(sb.toString());
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ips, Integer.parseInt(ports)), 200);
            socket.setSoTimeout(100);
            // Set a timeout for the socket (e.g., 5000 milliseconds)
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("HELLO " + fullnode);//Connect with Node
            out.println("FINDNEAREST " + shaValue);
            out.println("BYE testComplete" );
            String result = in.readLine();
            Nodes = nearNodes(in, Nodes, visitedNodes);
            //System.out.println(Nodes);

            while (!Nodes.isEmpty()) {
                //System.out.println(Nodes.peek());
                String a = Nodes.pop();
                if(a.equals(fullnode)){
                    continue;
                }
                if (a.equals("127.0.0.1/80/localhost")) {
                    continue;
                }
                String[] parts = a.split("/");
                String ip = parts[parts.length - 3];
                String port = parts[parts.length - 2];

                if (!visitedNodes.contains(a)) {
                    visitedNodes.add(a);
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 400);//change the time if needed
                        socket.setSoTimeout(100);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String hello = in.readLine();
                        int count12 = hello.length() - hello.replace("/", "").length();

                        out.println("HELLO ephemeral");

                        out.println("FINDNEAREST " + shaValue);

                        result = in.readLine();
                        //System.out.println(result);
                        int count1 = result.length() - result.replace("/", "").length();
                        if (count1 == 2) {
                            if (count1 != 2 || result.isEmpty()) {
                                dummyNodes.add(a);
                                continue;
                            }
                            String line;
                            while ((line = in.readLine()) != null) {
                                int count = line.length() - line.replace("/", "").length();
                                if (count == 2) {
                                    visitedNodes.add(line);
                                }
                            }

                            out.println("BYE testComplete" );
                            out.close();
                            socket.close();
                            in.close();
                        }
                    } catch (SocketTimeoutException e) {
                        dummyNodes.add(a);

                        continue; // Skip to next iteration of loop
                    } catch (IOException e) {

                        continue; // Skip to next iteration of loop
                    }
                }
            }

        } catch (IndexOutOfBoundsException | IOException e) {
            //return false;
        }
        //visitedNodes.remove(fullnode);
        deleteDuplicates(visitedNodes);
        //visitedNodes = FixNodes(visitedNodes);
        return visitedNodes;
    }

    //output response from terminal
    public Stack<String> nearNodes(BufferedReader in, Stack<String> s, ArrayList<String> list) {
        String line = "";
        while (true) {
            try {
                line = in.readLine();
                if (line == null) {
                    break;
                }
                String id = line;

                int count = id.length() - id.replace("/", "").length();
                if (count == 2) {
                    //System.out.println(id);
                    if (!list.contains(id)) {
                        s.push(id);
                    }
                }
            } catch (SocketTimeoutException e) {
                // Break the loop if a timeout occurs
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return s;
    }
    //convert to sha 256
    public String sha(String s) {
        String input = s + "" + "\n";
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
// Convert the hash to a hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        String hashString = hexString.toString();

        return hashString;

    }

    //check is the string is intenger
    public boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    //Create text file to store hash values
    public void createFileIfNotExists(String fileName) {
        Path filePath = Paths.get(fileName);
        if (!Files.exists(filePath)) {
            try {
                Files.createFile(filePath);
                System.out.println("File created: " + fileName);
            } catch (IOException e) {
                System.err.println("Failed to create file: " + e.getMessage());
            }
        } else {
            System.out.println("File already exists: " + fileName);
        }
    }

    //save to txt file
    public void saveHashMapToFile(HashMap<String, String> hashMap, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Map.Entry<String, String> entry : hashMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().replace("\n", "¬");
                writer.write(key + "=" + value);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //load from txt file
    public HashMap<String, String> loadHashMapFromFile(String fileName) {
        HashMap<String, String> hashMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] keyValue = line.split("=", 2);
                if (keyValue.length == 2) {
                    String value = keyValue[1].replace("¬", "\n");
                    hashMap.put(keyValue[0], value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hashMap;
    }

    //delete duplicate values
    public static <T> void deleteDuplicates(ArrayList<T> list) {
        Set<T> set = new HashSet<>(list); // Convert the list to a set to remove duplicates
        list.clear(); // Clear the original list
        list.addAll(set); // Add the unique elements back to the list
    }

}