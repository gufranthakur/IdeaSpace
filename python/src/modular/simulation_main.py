from hand_tracking_module import handDetector
import cv2
import socket
import time

cap = cv2.VideoCapture(0)
cap.set(3, 640)
cap.set(4, 480)

success, img = cap.read()
h, w, _ = img.shape
detector = handDetector(maxHands=2, detectionConfidence=0.7)

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
serverAddressPort = ("127.0.0.1", 65000)
leftAddressPort = {"127.0.0.1", 65005}

# FPS control threshold (adjust between 15-60)
target_fps = 15
frame_time = 1.0 / target_fps
last_time = time.time()

while True:
    success, img = cap.read()
    img = cv2.flip(img, 1)
    img = detector.find_hands(img)

    # Check if hands are detected
    if detector.results and detector.results.multi_hand_landmarks:
        # Zip landmarks and handedness together to process them in pairs
        for hand_landmarks, handedness in zip(detector.results.multi_hand_landmarks,
                                             detector.results.multi_handedness):

            data = []
            for landmark in hand_landmarks.landmark:
                # Landmark scaling
                data.extend([landmark.x * w, landmark.y * h, landmark.z * 200])

            # Get the label (MediaPipe returns "Left" or "Right")
            hand_label = handedness.classification[0].label

            # Route to specific ports
            if hand_label == "Right":
                sock.sendto(str.encode(str(data)), serverAddressPort)
            elif hand_label == "Left":
                # Note: Changed your set {} to a tuple () to match socket requirements
                sock.sendto(str.encode(str(data)), ("127.0.0.1", 65005))

    # FPS throttling
    elapsed = time.time() - last_time
    if elapsed < frame_time:
        time.sleep(frame_time - elapsed)
    last_time = time.time()
