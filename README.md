# SIRS2017G15
SIRS Project Repository

This project was developed with Java8 (java8u151), Python (Python 3.6.2) and fwbuilder (Firewall Builder).
Another tool worthy of mention is Maven which was used for easier compilation and execution of the project. (Commands below)
It was executed on the CentOS provided for the labs, with exactly the same specs. (Just had to make sure to install Java8, upgrade Python and install Apache Maven).
We had 3 Virtual Machines running, 1 to serve as the External machine, 1 to be the Firewall+Gateway and 1 Internal to be the set of devices (only 1 for the devices because it'd weigh a bit if we set up more)
First we install the built Firewall on the Gateway machine.
Then in the External we use Maven to start up the Client, in the FW+GW we use Maven to start up the Server and in the Internal we use Python to run the device applications (There are 2, a lamp and a refrigerator).


----------------------MAVEN COMMANDS------------------------

-Clean the project: mvn clean

-Compile the project: mvn compile

-Install the package: mvn package

-Install and test the package: mvn install

-Execute the project: mvn exec:java

-----------------MAVEN: CLIENT/SERVER COMMANDS----------------

You should have the common-utils package installed in your system before coding or running the project:

mvn clean; mvn compile; mvn package


Before launching a client, you should be launching the server, these are the example commands:

mvn clean; mvn compile; mvn package; mvn exec:java


To launch a single client you only need to call the following commands:

mvn clean; mvn compile; mvn exec:java


--------------------------DEVICES----------------------------

To launch a single device, have the server running and go to ssh-socket-device folder and run the following commands:

python3 generic_lamp.py; javac pcClient.java; java pcClient

In the pcClient interface write "CONNECT {IP:PORT}" where IP is the IP of the server and PORT the respective port
