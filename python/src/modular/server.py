import socket

class Server:
    def __init__(self, port, host="localhost"):
        self.host = host
        self.port = port
        self.socket = None
        self.connected = False
        self._connect()
    
    def _connect(self):
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.host, self.port))
            self.connected = True
            print(f"Connected to server on port {self.port}")
        except Exception as e:
            print(f"No server on port {self.port}: {e}")
            self.connected = False

    def send_command(self, message):
        if not self.connected:
            print(f"Not connected. Command was: {message}")
            return False
        try:
            self.socket.sendall((message + "\n").encode())
            print(f"[Port {self.port}] Sent: {message}")
            return True
        except Exception as e:
            print(f"Server disconnected. Command was: {message}")
            self.connected = False
            return False

    def close(self):
        if self.socket:
            self.socket.close()