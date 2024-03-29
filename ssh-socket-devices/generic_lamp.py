import socket               # Import socket module
import _thread
import base64
import os
import time, threading
from sys import getsizeof
from random import randint
from Crypto.Cipher import AES


host = ''
port = 5560

GateWaySocket = ''

turnedON = True
storedValue = str(turnedON)
myName = "Lamp" + str(randint(0,10))

factoryKey = ''
base64FactoryKey = ''



def generateFactoryKey():
     key = os.urandom(16)			#128 bits
     encodedkey = base64.b64encode(key)
     print("secret key:" + str(key) + "\nbase64 secret key:" + str(encodedkey))
     decodedkey = base64.b64decode(encodedkey)
     print("decoded base64 secret key:" + str(decodedkey))
     return key, encodedkey 


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
    reply = ("ON" if turnedON else "OFF")
    return reply

def REPEAT(dataMessage):
    reply = dataMessage[1]
    return reply

def switchLamp():
    global turnedON 
    turnedON = not turnedON
    reply = "The lamp has been switched " + ("ON" if turnedON else "OFF")
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
            try:
                _thread.start_new_thread( serveGateway, (GateWaySocket, ) )
            except:
                print ("Error: unable to start thread")
            
        else:
            reply = 'Unknown Command'
        # Send the reply back to the client
        conn.sendall(bytes(reply, 'utf-8')) 
        print("Data has been sent!")
    conn.close()
    
def periodicSend(message, socket):
	socket.sendall(bytes(message, 'utf-8')) 
	#print(message + time.ctime())
	threading.Timer(1, periodicSend, [message,socket]).start()
	
def serveGateway(conn):
    print ("Handling Gateway")
    periodicSend("boi",conn)
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
            reply = switchLamp()
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
        dataTransfer(conn, s)
    except Exception as inst:
        print (inst)
        print (inst.args)
        break
