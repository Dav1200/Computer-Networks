// IN2011 Computer Networks
// Coursework 2022/2023
//
// Submission by
// YOUR_NAME_GOES_HERE
// YOUR_STUDENT_ID_NUMBER_GOES_HERE
// YOUR_EMAIL_GOES_HERE

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DSTStore {

    // Do not change the interface!
    public DSTStore() {
    }

    // Do not change the interface!
    public boolean storeValue(String startingNodeName, String value) {
        String msg = "";
        boolean stored = false;
        String shaValue;
        int numLines;
        if (!value.isEmpty()) {
            shaValue = sha(value);
            numLines = value.split("\n").length;
        } else {
            numLines = 0;
            return false;
            //return false;
        }
        String trimmedStr = value.trim().replaceAll("\n+$", "\n");

        shaValue = sha(trimmedStr);
        // System.out.println(shaValue);
        StringBuilder binaryStrings = new StringBuilder();
        for (int i = 0; i < shaValue.length(); i++) {
            String hex = shaValue.substring(i, i + 1);
            int num = Integer.parseInt(hex, 16);
            String binary = Integer.toBinaryString(num);
            while (binary.length() < 4) {
                binary = "0" + binary;
            }
            binaryStrings.append(binary);
        }
        // System.out.println(value);
        System.out.println("SHA 256: " + shaValue);
        String convertedKey = binaryStrings.toString();


        String[] part = startingNodeName.split("/");
        String ips = part[part.length - 3];
        String ports = part[part.length - 2];

        try {
            ArrayList<String> visitedNodes = new ArrayList<>();
            ArrayList<String> finalNodes = new ArrayList<>();
            ArrayList<String> dummyNodes = new ArrayList<>();
            Stack<String> Nodes = new Stack<>();
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ips, Integer.parseInt(ports)), 100);
            socket.setSoTimeout(100);
            // Set a timeout for the socket (e.g., 5000 milliseconds)
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("HELLO ephemeral");//Connect with Node
            out.println("FINDNEAREST " + shaValue);
            String result = in.readLine();
            Nodes = nearNodes(in, Nodes, visitedNodes);

            //add all near nodes of each other
            while (!Nodes.isEmpty()) {
                String a = Nodes.pop();
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
                        socket.setSoTimeout(400);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        //System.out.println(in.readLine());
                        out.println("HELLO ephemeral");
                        out.println("FINDNEAREST " + shaValue);
                        result = in.readLine();
                        Nodes = nearNodes(in, Nodes, visitedNodes);
                        //System.out.println(Nodes);

                    } catch (SocketTimeoutException e) {
                        dummyNodes.add(a);
                        // Handle timeout
                        //System.out.println("Error: Connection attempt timed out");
                        continue; // Skip to next iteration of loop
                    } catch (IOException e) {
                        // Handle other I/O errors
                        //System.out.println("Error: " + e.getMessage());
                        continue; // Skip to next iteration of loop
                    }
                }
            }
            //removes nodes that we cant connect to
            for (String s : dummyNodes) {
                visitedNodes.remove(s);
            }


            deleteDuplicates(visitedNodes);
            finalNodes = visitedNodes;

            ArrayList<String> tempList = finalNodes;
            // System.out.println(tempList);
            tempList = containKey(tempList);
            HashMap<Integer, Integer> threeNearNodes = compareNodes(tempList, convertedKey);
            // System.out.println(finalNodes);
            System.out.println("NEAREST NODES: " + threeNearNodes);
            System.out.println("INDEX: " + finalNodes);
            int countt = 0;

            System.out.println("Nearest 3 VALID Nodes: ");
            for (Map.Entry<Integer, Integer> entry : threeNearNodes.entrySet()) {
                //code to store the value  now to all the corresponding nodes

                if (countt < 3) {

                    String[] partf = finalNodes.get(entry.getKey()).split("/");
                    String ipsf = partf[partf.length - 3];
                    String portsf = partf[partf.length - 2];

                    try {
//test if we need to store in the next avaiable node, if some are offline, could count --
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(ipsf, Integer.parseInt(portsf)), 5000);
                        socket.setSoTimeout(500);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        //System.out.println(in.readLine());
                        out.println("HELLO ephemeral");
                        out.println("STORE " + numLines);
                        out.println(trimmedStr);
                        String line;
                        //System.out.println(numLines);
                        //result = in.readLine();
                        if (result.isEmpty()) {

                        }
                        while ((line = in.readLine()) != null) {
                            //
                            System.out.println(line);
                            if (line.startsWith("STORED")) {
                                System.out.println("Stored At: " + finalNodes.get(entry.getKey()));
                                stored = true;
                            }

                            else{
                                System.out.println(line);
                            }
                        }
                        //String a =  nearNodes(in);
                        //System.out.println(countt);
                    } catch (SocketTimeoutException e) {
                        //System.out.println("NOTSTORED : " + finalNodes.get(entry.getKey()));
                        //countt--;
                        // Handle timeout
                        //System.out.println("Error: Connection attempt timed out");
                        //return false;
                    } catch (IOException e) {
                        //System.out.println("NOTSTORED : " + finalNodes.get(entry.getKey()));
                        //
                        // Handle other I/O errors
                        //System.out.println("Error: " + e.getMessage());
                        //return false;
                    }

                    countt++;
                } else {
                    break;
                }
            }


            //System.out.println(convertedKey);

            //System.out.println(tempList);


            //System.out.println(shaValue);
            // Compute the key for the input using the SHA-256 hash.

            // Connect to the DSTHash23 network using startingNodeName.

            // Find the three nodes in the network with the closest IDs to the key.

            // Store the contents of the file on all of the three closest nodes.

            // If this works, return true.

            // If it does not, return false.


        } catch (IndexOutOfBoundsException | IOException e) {
            return false;
        }

        // System.out.println(msg);
        if (stored) {
            return true;
        } else {
            return false;
        }
    }

    public String nearNodes(BufferedReader in) {
        String line = "";
        String id = "";
        while (true) {
            try {
                line = in.readLine();
                if (line == null) {
                    break;
                }
                id = line;

                System.out.println(id);

            } catch (SocketTimeoutException e) {
                // Break the loop if a timeout occurs
                return id;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return id;
    }

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

    public static <T> void deleteDuplicates(ArrayList<T> list) {
        Set<T> set = new HashSet<>(list); // Convert the list to a set to remove duplicates
        list.clear(); // Clear the original list
        list.addAll(set); // Add the unique elements back to the list
    }

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

    public ArrayList<String> containKey(ArrayList<String> fList) {
        ArrayList<String> binaryV = new ArrayList<>();
        for (int j = 0; j < fList.size(); j++) {
            String[] temp = fList.get(j).split("/");
            String id = fList.get(j);

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

    public HashMap<Integer, Integer> compareNodes(ArrayList<String> flistC, String s) {
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
}
