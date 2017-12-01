import socket               # Import socket module
import _thread
import os
import base64
from sys import getsizeof
from random import randint

host = ''
port = 0

GateWaySocket = ''
GateWaySocketSendCmd = ''
GateWayOutPort = 0

state = ["OFF", "REFRIGERATING", "COOLING_DOWN"]
curState = 0
myName = "Refrigerator " + str(randint(0,10))

factoryKey = ''
base64FactoryKey = ''

def generateFactoryKey():
     key = os.urandom(16)			#128 bits
     encodedkey = base64.b64encode(key)
     print("secret key:" + str(key) + "\nbase64 secret key:" + str(encodedkey))
     decodedkey = base64.b64decode(encodedkey)
     print("decoded base64 secret key:" + str(decodedkey))
     return key, encodedkey 

def setupGatewayServer():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    print("Socket created.")
    try:
        s.bind((host, GateWayOutPort))
    except socket.error as msg:
        print(msg)
    print("GATEWAY Socket bind comPlete. Host:" + s.getsockname()[0]+ ";Port:"+str(s.getsockname()[1]))
    return s

def setupServer():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    print("Socket created.")
    try:
        s.bind((host, port))
    except socket.error as msg:
        print(msg)
    print("Socket bind comPlete. Host:" + s.getsockname()[0]+ ";Port:"+str(s.getsockname()[1]))
    return s

def setupConnection():
    s.listen(1) # Allows one connection at a time.
    print("Waiting for client")
    conn, address = s.accept()
    return conn

def GETSTATUS():
    reply = state[curState]
    return reply

def REPEAT(dataMessage):
    reply = dataMessage[1]
    return reply

def switchState():
    global curState 
    curState = curState + 1 % len(state)
    reply = "The "+ myName +" state has been switched!"
    return reply

def encryption(privateInfo): 
	BLOCK_SIZE = 16 
	PADDING ='{' 
	
	pad = lambda s: s + (BLOCK_SIZE - len(s) % BLOCK_SIZE) * PADDING 
	
	EncodeAES = lambda c, s: base64.b64encode (c.encrypt (pad(s))) 
	
	secret = os.urandom(BLOCK_SIZE) 
	print ('encryption key:'), secret
	
	cipher = AES.new(secret) 
	
	encoded = EncodeAES(cipher, privateInfo) 
	print ('Encrypted string:'), encoded
    
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
            reply = switchState()
        elif command == 'EXIT':
            print("Our client has left us :(")
            break
        elif command == 'KILL':
            print("Our server is shutting down.")
            s.close()
            return
        elif command.startswith( 'CONNECT' ):
            global GateWaySocket, GateWaySocketSendCmds	    
            URL = command.split()[1].split(':')
            GateWaySocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            GateWaySocket.connect((URL[0], int(URL[1])))
            print (socket.getaddrinfo(URL[0], int(URL[1])))
            buffer_size = 100            
            reply = myName + '\n'
            GateWaySocket.sendall(bytes(myName + ":" + str(GateWaySocketSendCmds.getsockname()[1]) , 'utf-8'))
            try:
                _thread.start_new_thread( serveGateway, (GateWaySocket, ) )
            except:
                print ("Error: unable to start thread")
            GateWaySocketSendCmds.accept(1)
            
        else:
            reply = 'Unknown Command'
        # Send the reply back to the client
        conn.sendall(bytes(reply, 'utf-8')) 
        print("Data has been sent!")
    conn.close()
    
    

def serveGateway(conn):
    print ("Handling Gateway")
    while True:
        # Receive the data
        data = conn.recv(1028) # receive the data
        data = data.decode('utf-8')
        data = data.strip()
        print("data value from Gateway: " + data)
        # Split the data such that you separate the command
        # from the rest of the data.
        command = str(data)
        print("data length from Gateway: " + str(getsizeof(command)))
        reply = ""
        if command == "GETSTATUS":
            reply = GETSTATUS()
            print (command)
            print (reply)
        elif command == 'REPEAT':
            reply = REPEAT(data)
        elif command == 'SWITCH':
            reply = switchState()
        elif command == 'EXIT':
            print("Our Gateway has left us :(")
            break
        elif command == 'KILL':
            print("Our device is shutting down.")
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
            reply = '[GATEWAY]Unknown Command'
        conn.sendall(bytes(reply, 'utf-8')) 
        print("[GATEWAY] Data has been sent!")
    conn.close()

s = setupServer()

while True:
    try:
        factoryKey, base64FactoryKey = generateFactoryKey()
        conn = setupConnection()
        GateWaySocketSendCmds = setupGatewayServer()
        dataTransfer(conn, s)
    except Exception as inst:
        print (inst)
        print (inst.args)
        break
