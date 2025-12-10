import socket
import time

HOST = "localhost"
PORT = 65000

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((HOST, PORT))

def send_command(message):
    try:
        s.sendall((message + "\n").encode())
        print("Data sent: " , message)
    except (BrokenPipeError, ConnectionResetError):
        print("Server disconnected. Command was:", message)
