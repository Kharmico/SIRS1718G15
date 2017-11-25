import socket               # Import socket module
from sys import getsizeof

host = ''
port = 5560

GateWaySocket = ''

storedValue = "Yo, what's up?"
turnedON = True
myName = "Lamp"

def setupServer():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    print("Socket created.")
    try:
        s.bind((host, port))
    except socket.error as msg:
        print(msg)
    print("Socket bind comPlete.")
    return s

def setupConnection():
    s.listen(1) # Allows one connection at a time.
    print("Waiting for client")
    conn, address = s.accept()
    return conn

def GETSTATUS():
    reply = storedValue +" I'm " + myName
    return reply

def REPEAT(dataMessage):
    reply = dataMessage[1]
    return reply

def switchLamp():
    global turnedON 
    turnedON = not turnedON
    reply = "The lamp has been switched " + ("ON" if turnedON else "OFF")
    return reply
    
def dataTransfer(conn, s):
    # A big loop that sends/receives data until told not to.
    while True:
        # Receive the data
        data = conn.recv(1028) # receive the data
        data = data.decode('utf-8')
        data = data.strip()
        print("data value from client: " + data)
        # Split the data such that you separate the command
        # from the rest of the data.
        command = str(data)
        print("data length from client: " + str(getsizeof(command)))
        reply = ""
        if command == "GETSTATUS":
            reply = GETSTATUS()
            print (command)
            print (reply)
        elif command == 'REPEAT':
            reply = REPEAT(data)
        elif command == 'SWITCH':
            reply = switchLamp()
        elif command == 'EXIT':
            print("Our client has left us :(")
            break
        elif command == 'KILL':
            print("Our server is shutting down.")
            s.close()
            return
        elif command.startswith( 'CONNECT' ):
            global GateWaySocket
            URL = command.split()[1].split(':')
            GateWaySocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            GateWaySocket.connect((URL[0], int(URL[1])))
            print (socket.getaddrinfo(URL[0], int(URL[1])))
            buffer_size = 100            
            reply = myName + '\n'
            GateWaySocket.sendall(bytes(myName, 'utf-8'))
        else:
            reply = 'Unknown Command'
        # Send the reply back to the client
        conn.sendall(bytes(reply, 'utf-8')) 
        print("Data has been sent!")
    conn.close()

s = setupServer()

while True:
    try:
        conn = setupConnection()
        dataTransfer(conn, s)
    except Exception as inst:
        print (inst)
        print (inst.args)
        break