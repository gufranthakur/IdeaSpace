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
    img = cv2.flip(img, 1)  # Flip horizontally
    # Find the hand and its landmarks
    img = detector.find_hands(img)  # Returns only img
    data = []

    # Check if hands were detected via results
    if detector.results and detector.results.multi_hand_landmarks:
        # Process each detected hand
        for hand_num in range(len(detector.results.multi_hand_landmarks)):
            lmList = detector.find_position(img, hand_number=hand_num, draw=False)
            for lm in lmList:
                data.extend([lm[1], lm[2], 0])  # lm[0] is id, lm[1] is x, lm[2] is y

        sock.sendto(str.encode(str(data)), serverAddressPort)

    # Display
    cv2.imshow("Image", img)
    cv2.waitKey(1)
