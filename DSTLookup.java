// IN2011 Computer Networks
// Coursework 2022/2023
//
// Submission by
// DAVINDER SINGH
// 210022278
// YOUR_EMAIL_GOES_HERE

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DSTLookup {

    // Do not change the interface!
    public DSTLookup() {
    }

    // Do not change the interface!
    public String getValue(String startingNodeName, String key) {
        String value = "";
        StringBuilder binaryStrings = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            String hex = key.substring(i, i + 1);
            int num = Integer.parseInt(hex, 16);
            String binary = Integer.toBinaryString(num);
            while (binary.length() < 4) {
                binary = "0" + binary;
            }
            binaryStrings.append(binary);
        }
        String convertedKey = binaryStrings.toString();
        //System.out.println(convertedKey);

        try {
            //keep track of visited nodes in the network;
            ArrayList<String> visitedNodes = new ArrayList<>();

            //nodes that contain the key
            ArrayList<String> finalNodes = new ArrayList<>();

            //track of all nodes that need to be traversed
            Stack<String> Nodes = new Stack<>();

            String[] part = startingNodeName.split("/");
            String ips = part[part.length - 3];
            String ports = part[part.length - 2];
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ips, Integer.parseInt(ports)), 600);
            socket.setSoTimeout(400);
            // Set a timeout for the socket

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("HELLO ephemeral");//Connect with Node
            out.println("FINDNEAREST " + key);
            String result = in.readLine();
            Nodes = nearNodes(in, Nodes, visitedNodes);
            //System.out.println(Nodes);
            //Convert key to binary

            out.println("LOOKUP " + key);
            if (in.readLine().equals("NOTFOUND")) {
            } else {
                finalNodes.add(result.substring(6));
            }


            //Get all the network nodes non duplicates
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
                        socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 400);
                        socket.setSoTimeout(100);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        //System.out.println(in.readLine());
                        out.println("HELLO ephemeral");
                        out.println("FINDNEAREST " + key);
                        result = in.readLine();
                        Nodes = nearNodes(in, Nodes, visitedNodes);
                        //System.out.println(Nodes);

                    } catch (SocketTimeoutException e) {

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
//apply fix here before looking them upo, filter good nodes
            //System.out.println(visitedNodes);
            for (int i = 0; i < visitedNodes.size(); i++) {
                String a = visitedNodes.get(i);
                String[] parts = a.split("/");
                String ip = parts[parts.length - 3];
                String port = parts[parts.length - 2];

                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 400);
                    socket.setSoTimeout(100);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //System.out.println(in.readLine());
                    out.println("HELLO ephemeral");
                    out.println("LOOKUP " + key);
                    result = in.readLine();
                    if (in.readLine().equals("NOTFOUND")) {
                    } else {
                        finalNodes.add(result.substring(6));
                    }


                } catch (SocketTimeoutException e) {
                    // Handle timeout
                    //System.out.println("Error: Connection attempt timed out");
                    continue; // Skip to next iteration of loop
                } catch (IOException e) {
                    // Handle other I/O errors
                    //System.out.println("Error: " + e.getMessage());
                    continue; // Skip to next iteration of loop
                }
            }


            deleteDuplicates(finalNodes);
            // System.out.println(finalNodes);
            ArrayList<String> tempList = finalNodes;
            tempList = containKey(tempList);

            //System.out.println(visitedNodes);
            System.out.println("NODES CONTAINING VALUE: " + finalNodes);
            //System.out.println(finalNodes);
            int index = compareNodes(tempList, convertedKey);
            System.out.println(finalNodes.get(index));
            // System.out.println(index);
            String a = finalNodes.get(index);
            String[] parts = a.split("/");
            String ip = parts[parts.length - 3];
            String port = parts[parts.length - 2];
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 500);
                socket.setSoTimeout(300);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //System.out.println(in.readLine());
                out.println("HELLO ephemeral");
                out.println("LOOKUP " + key);

                result = in.readLine();
                value = nearNodes(in);
                //System.out.println(value);

            } catch (SocketTimeoutException e) {
                // Handle timeout
                //System.out.println("Error: Connection attempt timed out");

            } catch (IOException e) {
                // Handle other I/O errors
                //System.out.println("Error: " + e.getMessage());

            }

            //System.out.println(visitedNodes);


        } catch (IndexOutOfBoundsException | IOException e) {
            return null;
        }

        return "Value Found: \n"+ value;
    }

    public int compareNodes(ArrayList<String> flistC, String s) {
        int count = 0;
        int fcount = 0;
        int Index = 0;
        // System.out.println(flistC);
        for (int i = 0; i < flistC.size(); i++) {
            for (int j = 0; j < flistC.get(i).length(); j++) {
                if (flistC.get(i).charAt(j) == s.charAt(j)) {
                    count++;
                } else {
                    break;
                }
            }

            if (count > fcount) {
                fcount = count;
                Index = i;
            }
            count = 0;
        }

        return Index;
    }


    public static <T> void deleteDuplicates(ArrayList<T> list) {
        Set<T> set = new HashSet<>(list); // Convert the list to a set to remove duplicates
        list.clear(); // Clear the original list
        list.addAll(set); // Add the unique elements back to the list
    }

    public ArrayList<String> containKey(ArrayList<String> fList) {
        ArrayList<String> binaryV = new ArrayList<>();
        for (int j = 0; j < fList.size(); j++) {
            String[] temp = fList.get(j).split("/");
            String id = fList.get(j);

            id = sha(id);


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


    public String nearNodes(BufferedReader in) {
        String line = "";
        String id = "";
        while (true) {
            try {
                line = in.readLine();
                if (line == null) {
                    break;
                }
                id = id + line+"\n";


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
}

