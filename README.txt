~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Q1: DSTLOOKUP finds all the nearest nodes to the key through depth first traversal,after they are checked for the value, nearest node checked first then others
    Once value is found it is displayed in the terminal. If no value is found nothing will be returned but an empty array will be shown meaning no near nodes. 
    VALUE will be shown in terminal if not returned

Complie(VM INTELL IDEA TERMINAL): 
          To test and run the class, make sure you complile the testcase along side with DSTLOOKUP. javac "Test case Path here" -> 
          example: javac DSTLookupCmdLine.java
	    this compiles both the main class and the testing class. To run the test case along side with LOOKUP DO:

          java DSTLookupCmdLine 'StartingNodeAddress' + KEY  Example -> 
	    java DSTLookupCmdLine 10.200.51.15/20111/martin.brain@city.ac.uk-reference-implementation 'PUT YOUR 'SHA "256 (HEX) KEY HERE'
          if you have compiled it correctly an output will be displayed in the terminal window. 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Q2: 
    DSTSTORE finds the 3 closeset nodes in the network which are closest to the key provided if the 3 closeset node is the one you start
    the value will be stored in there + 2 other closest Nodes. When running this test case it will display if it was stored on any of 
    the Nodes. A boolean Value will be returned. This uses depth first traversal to find all the nodes in the network. 	

Complie(VM Intellj): 
                      To test and run the class, make sure you complile the testcase along side with DSTLOOKUP. javac "Test case Path here" -> 
                      example: javac DSTStoreCmdLine.java
			    To run the test case -> java DSTStoreCmdLine 'StartingNodeName' PRESS ENTER 
                      then type the text you would like to store 
	                Once finished typing the text press CTRL D and the program should stop listening for inputs and move to the DSTSTORE
			    THe output would be either successful if it managed to store on any of 3 closest nodes or unsuccessfull(false)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Q3: 
   This sets up a node on the network and has the all functioanlity required including hello, ping, findnearest, lookup store and bye
   This Nodes goes further and has a passive and active mapping which finds all nodes in the network and filters them to see if they 
   are responsive or not. Multithreading is used to allow multiple clients to connect to the node which are handled individually. 
   AUTOMATIC FILTER ADDED to delete dummy nodes. 
   GIVE THE NODE SOME TIME TO FIND ALL ACTIVE NODES ON THE NETWORK(30seconds)

   The code is fully commented to help the user understand how all of this is possible. 

   Storing a value on the node will create a txt file which keeps the key and value for future references

SPECIAL FEATURE: After saying HELLO to the node I have added a command 'ALL' which lists all active nodes in the network that work.
                 I have set the update timer to 1 second, this can be increased or decreased. I felt 1 second worked the best. 
		     If a full node connects they are checked as well to see if they have a resposive node. If they do not they are not added 
                 to the active Nodes list(MARTIN's Test Nodes do not remove unresposive nodes so it was difficult to get this working)

Complie: First you will need to get your ip address from the terminal using the command 'hostname -I' Copy the second ip address. The
          port number can be as you like but less then 65500. once you have these you are ready to Compile.
          
          javac 'Testclasshere' -> javac DSTNodeCmdLine
          TO RUN:
          java DSTNodeCmdLine 'input your full node here' 'StartNodeName' ->
	    java DSTNodeCmdLine 10.205.16.120 12345 Davinder.Singh@city.ac.uk 10.200.51.15/20111/martin.brain@city.ac.uk-reference-implementation

If you have complied correctly the node should up and running you can further test this through opening the terminal and conecting to the node ->
nc ipadrr port (of the node you setup)
Then you are able to test the node as you like.  
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~