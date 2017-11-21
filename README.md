# SIRS2017G15
SIRS Project Repository


Notice the following maven commands:

-Clean the project: mvn clean

-Compile the project: mvn compile

-Install the package: mvn package

-Install and test the package: mvn install

-Execute the project: mvn exec:java

You should have the common package installed in your system before coding or running the project:

mvn clean; mvn compile; mvn package


Before launching a client, you should be launching the server, these are the example commands:

mvn clean; mvn compile; mvn package; mvn exec:java


To launch a single client you only need to call the following commands:

mvn clean; mvn compile; mvn exec:java
