from hand_tracking_module import handDetector
import cv2
import socket

cap = cv2.VideoCapture(0)
cap.set(3, 640)
cap.set(4, 480)
success, img = cap.read()
h, w, _ = img.shape
detector = handDetector(maxHands=2, detectionConfidence=0.5)

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
serverAddressPort = ("127.0.0.1", 65000)

while True:
    success, img = cap.read()
    img = cv2.flip(img, 1)
    img = detector.find_hands(img)
    data = []

    if detector.results and detector.results.multi_hand_landmarks:
        for hand_landmarks in detector.results.multi_hand_landmarks:
            for landmark in hand_landmarks.landmark:
                # Send actual x, y, z from MediaPipe
                data.extend([landmark.x * w, landmark.y * h, landmark.z * 200])

        sock.sendto(str.encode(str(data)), serverAddressPort)

    cv2.imshow("Image", img)
    cv2.waitKey(1)
