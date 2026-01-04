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
    # Get image frame
    success, img = cap.read()
    # Find the hand and its landmarks
    hands, img = detector.find_hands(img)  # with draw
    # hands = detector.findHands(img, draw=False)  # without draw
    data = []

    if hands:
        # Hand 1
        hand = hands[0]
        lmList = hand["lmList"]  # List of 21 Landmark points
        for lm in lmList:
            data.extend([lm[0], lm[1], lm[2]])

        sock.sendto(str.encode(str(data)), serverAddressPort)

    # Display
    cv2.imshow("Image", img)
    cv2.waitKey(1)
