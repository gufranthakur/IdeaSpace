import socket
import time

HOST = "localhost"
PORT = 65000
try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((HOST, PORT))
except:
    print("No server ueueueueueueuue")

def send_command(message):
    try:
        s.sendall((message + "\n").encode())
        print("Data sent: " , message)
    except:
        print("Server disconnected. Command was:", message)
