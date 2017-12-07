import socket               # Import socket module
import _thread
import os
import base64
import time, threading
from sys import getsizeof, argv
from random import randint
from Crypto.Cipher import AES
from Crypto import Random
from Crypto.Hash import HMAC, SHA
from hashlib import sha1



host = ''
port = 0

if (len(argv) == 2):
    port = int(argv[1])

GateWaySocket = ''
GateWaySocketListen = ''
GateWaySocketSendCmds = ''
GateWayOutPort = 0

state = ["OFF", "REFRIGERATING", "COOLING_DOWN"]
curState = 0
myName = "Refrigerator" + str(randint(0,100))
myType = "Fridge"

factoryKey = ''             #bytes
base64FactoryKey = ''       #Base64
hmac_key = ''               #bytes
challenge = os.urandom(4)   #bytes
sessionKey = ''             #bytes

BLOCK_SIZE = 16  # Bytes for AES encryption

pad = lambda s: s + (BLOCK_SIZE - len(s) % BLOCK_SIZE) * \
                    chr(BLOCK_SIZE - len(s) % BLOCK_SIZE)
                
unpad = lambda s: s[:-ord(s[len(s) - 1:])]

def periodicSend(message, socket):
    #
    if(type(message) == bytes):
        #print(message.decode("utf-8"))
        socket.sendall(message)
    else:
        socket.sendall(bytes(message, 'utf-8'))
    #print(message + time.ctime())
    threading.Timer(1, periodicSend, [message,socket]).start()

def generateNewChallenge():
    global challenge
    challenge = os.urandom(4)

def generateFactoryKey():
     key = os.urandom(16)			#128 bits
     encodedkey = base64.b64encode(key)
     print("secret key:" + str(key) + "\nbase64 secret key:" + str(encodedkey))
     decodedkey = base64.b64decode(encodedkey)
     hmac_key = sha1(key).digest()
     #print("Hmac key["+str(getsizeof(hmac_key))+"]:" + str(hmac_key)+"\nbase64HMACKey:" + str(base64.b64encode(hmac_key)))
     return key, encodedkey,hmac_key 

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

def encryptdata(data, secretkey):     
    iv =  Random.new().read(AES.block_size)
    cipher = AES.new(secretkey, AES.MODE_CBC, iv)
    
    paddeddata = pad(data)
    cypherpart = cipher.encrypt(paddeddata)
    
    return cypherpart,iv

def decryptdata(data, secretkey, iv):     
    cipher = AES.new(secretkey, AES.MODE_CBC, iv)
    cypherpart = cipher.decrypt(data)
    
    unpaddeddata = unpad(cypherpart)
    
    if unpaddeddata == '':
        raise Exception("error decrypting")
    return unpaddeddata
    
def Hmac_data_to_send(data, hkey):
    datatosend = pad(data).encode()
    print("hmac'ing"+str(datatosend))
    a = HMAC.new(hkey, datatosend, SHA).digest()
    return a

def calc_Hmac(data, hkey):
    a = HMAC.new(hkey, data, SHA).digest()
    return a

def getCryptogram(data):
    cyphertext = base64.b64encode(data[0]) + ":".encode() + base64.b64encode(data[1]) + ":".encode() + base64.b64encode(data[2])
    print(str(cyphertext))
    #print("Hmac Message: "+ str(base64.b64encode(data[1])))
    return cyphertext

def encMsg(dataMessage, secretkey, hmac_key):
    c,iv = encryptdata(dataMessage, secretkey)
    h = Hmac_data_to_send(dataMessage, hmac_key)
    #print (dataMessage)
    #print(str(h))
    cry = getCryptogram([c,h,iv])
    return cry

def decB64Msg(data):
    data = data.split(':')
    message =[base64.b64decode(data[0]) , base64.b64decode(data[1]),  base64.b64decode(data[2]) ]
    return message

def getSessionKey(data, key):
    global challenge, sessionKey
    cryptogram = base64.b64decode(data.split(":")[0])
    hmac = base64.b64decode(data.split(":")[1])
    iv   = base64.b64decode(data.split(":")[2])
    decryptedgram = decryptdata(cryptogram, key, iv)    #B64SessionKey, B64Challenge
    print("before")
    calcHmac = calc_Hmac(decryptedgram, hmac_key)
    if(calcHmac != hmac):
        raise ValueError('[GETSESSIONKEY]HMACs don\'t match.')
    ch = base64.b64decode(decryptedgram.split(b",")[1])
    if(ch != challenge): 
        raise ValueError('[GETSESSIONKEY]Challenges don\'t match.')
    sessionKey = base64.b64decode(decryptedgram.split(b",")[0])
    
    
    
    
def login(sock):
    generateNewChallenge()
    try:
        auth1 = myName +","+GETSTATUS()+"," +myType+ ","+base64.b64encode(challenge).decode("utf-8")

        print(auth1)
        cryptogram = encMsg(auth1,factoryKey,hmac_key)
        sock.sendall(cryptogram) 
        data = sock.recv(1028)
        data = data.decode('utf-8')
        data = data.strip()
        getSessionKey(data, factoryKey)
        generateNewChallenge()
        auth2 = base64.b64encode(b"ACK").decode("utf-8") + "," +base64.b64encode(challenge).decode("utf-8")
        cryptogram = encMsg(auth2,sessionKey,hmac_key)
        #print("LOGIN KEY:"+str(sessionKey)+ "LOGIN IV:" + 
        sock.sendall(cryptogram)
        
        #print(str(data))
    except Exception as inst:
        print("login error:" + str(inst))
 
def sendACKorNACK(boolean, sock):
    m = ''
    if boolean == True:
        m = base64.b64encode(b"ACK").decode("utf-8") + "," + base64.b64encode(challenge).decode("utf-8")
    else:
        m = base64.b64encode(b"NACK").decode("utf-8")+ ","  + base64.b64encode(challenge).decode("utf-8")
    cryptogram = encMsg(m,sessionKey,hmac_key)
    sock.sendall(cryptogram)
    
def dataTransfer(conn, s):
    global GateWaySocket,GateWaySocketListen, GateWaySocketSendCmds 
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
            GateWaySocket.close()
            s.close()
            GateWaySocketListen.close()
            GateWaySocketSendCmds.close()
            quit()
            return
        elif command.startswith( 'ENCRYPT' ):
            reply = str(encryption("OLA MARCELO", factoryKey))
        elif command.startswith( 'CONNECT' ):
            URL = command.split()[1].split(':')
            GateWaySocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            GateWaySocket.connect((URL[0], int(URL[1])))
            #print (socket.getaddrinfo(URL[0], int(URL[1])))
            buffer_size = 100            
            reply = myName + '\n'
            GateWaySocket.sendall(bytes(myName + ":" + str(GateWaySocketListen.getsockname()[1]) , 'utf-8'))
            try:
                _thread.start_new_thread( serveGateway, (GateWaySocket, ) )
            except:
                print ("Error: unable to start thread")
            GateWaySocketListen.listen(1)
            GateWaySocketSendCmds, address = GateWaySocketListen.accept()
            print("accepted GateWaySocketListen connecion")
            #periodicSend(encMsg("123456789", factoryKey, hmac_key ),GateWaySocketSendCmds)
            login(GateWaySocketSendCmds)
        else:
            reply = 'Unknown Command'
        # Send the reply back to the client
        conn.sendall(bytes (reply, 'utf-8')) 
        print("Data has been sent!")
    conn.close()
    
    

def serveGateway(conn):
    print ("Handling Gateway")
    global sessionKey
    while True:
        # Receive the data
        data = conn.recv(1028) # receive the data
        try:
            data = data.decode('utf-8')
            data = data.strip()
            data = decB64Msg(data)      # [ [c,ch]:h:iv].
        except Exception as e:
            print("Error" + str(e))
            sendACKorNACK(False, conn)
            continue
  
        decryptedgram = ''
        hmac = ''
        try:
            decryptedgram = decryptdata(data[0], sessionKey, data[2])
            if decryptedgram == b'':
                print("[GW]Couldn't decrypt with SessionKey. Decrypting with Device Key")
                decryptedgram = decryptdata(data[0], factoryKey, data[2])
                if decryptedgram == b'':
                    raise Exception()
        except Exception as e:
            print("["+str(type(e))+"]Couldn't decrypt with DeviceKey. Fail")
            sendACKorNACK(False, conn)
            continue
        
        hmac = calc_Hmac(decryptedgram, hmac_key)
        decryptedgram = decryptedgram.split(b",") 
        try:
            if (base64.b64decode(decryptedgram[1]) != challenge):
                print('[GATEWAY CONN]Challenges don\'t match.')
               
            if ( data[1] != hmac):
                print('[GATEWAY CONN]HMACs don\'t match.')
                sendACKorNACK(False, conn)
                continue
        except:
            sendACKorNACK(False, conn)
            continue
        
        generateNewChallenge()
        
        data = decryptedgram[0]
        try:
            command = data.decode()
        except AttributeError as e:
            command = str(data)
            
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
        elif command.startswith( 'RENEW' ):
            try:            
                key = command.split()
                sessionKey = base64.b64decode(key[1])
                reply = 'ACK'
            except Exception as e:
                print(str(e))
                reply = 'NACK'
            #getSessionKey
            
        else:
            reply = '[GATEWAY]Unknown Command'
        reply = base64.b64encode(bytes(reply, 'utf-8')).decode("utf-8") + "," + base64.b64encode(challenge).decode("utf-8")
        reply = encMsg(reply, sessionKey, hmac_key)
        conn.sendall(reply) 
        print("[GATEWAY] Data has been sent!")
    conn.close()

s = setupServer()

while True:
    try:
        factoryKey, base64FactoryKey,hmac_key = generateFactoryKey()
        conn = setupConnection()
        GateWaySocketListen = setupGatewayServer()
        dataTransfer(conn, s)
    except Exception as inst:
        print (inst)
        print (inst.args)
        break
