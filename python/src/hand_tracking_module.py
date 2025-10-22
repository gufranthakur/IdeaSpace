import cv2
import mediapipe as mp
import time

class handDetector():
    def __init__(self, mode=False, maxHands=2, detectionConfidence=0.5, trackConfidence=0.5):
        self.mode = mode
        self.maxHands = maxHands
        self.detectionConfidence = detectionConfidence
        self.trackConfidence = trackConfidence
        self.mpHands = mp.solutions.hands
        self.hands = self.mpHands.Hands(
            static_image_mode=self.mode,
            max_num_hands=self.maxHands,
            min_detection_confidence=self.detectionConfidence,
            min_tracking_confidence=self.trackConfidence
        )
        self.mpDraw = mp.solutions.drawing_utils
        self.results = None  # Initialize results
    
    def find_hands(self, img, drawHands=True):
        imgRGB = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        self.results = self.hands.process(imgRGB)
        if self.results.multi_hand_landmarks:
            for hand_landmarks in self.results.multi_hand_landmarks:
                if drawHands:
                    self.mpDraw.draw_landmarks(img, hand_landmarks, self.mpHands.HAND_CONNECTIONS)
        return img
    
    def find_position(self, img, hand_number=0, draw=True):
        landmark_list = []
        if self.results.multi_hand_landmarks:
            if hand_number < len(self.results.multi_hand_landmarks):
                myHand = self.results.multi_hand_landmarks[hand_number]
                for id, landmark in enumerate(myHand.landmark):
                    h, w, c = img.shape
                    cx, cy = int(landmark.x * w), int(landmark.y * h)
                    landmark_list.append([id, cx, cy])
                    if draw == True:
                        # Draw filled circle with anti-aliasing
                        cv2.circle(img, (cx, cy), 7, (255, 0, 0), -1, cv2.LINE_AA)
        return landmark_list
    
    def get_handedness(self, hand_number=0):
        """Returns 'Right' or 'Left' for the detected hand"""
        if self.results and self.results.multi_handedness:
            if hand_number < len(self.results.multi_handedness):
                return self.results.multi_handedness[hand_number].classification[0].label
        return None

def main():
    cap = cv2.VideoCapture(0)
    detector = handDetector()
    while True:
        success, img = cap.read()
        img = cv2.flip(img, 1)
        img = detector.find_hands(img)
        
        # Get handedness
        handedness = detector.get_handedness(0)
        
        # Only process if right hand is detected
        if handedness == "Right":
            lms = detector.find_position(img)
            if len(lms) != 0:
                pass
            
            # Display hand type on screen
            cv2.putText(img, f"Hand: {handedness}", (10, 30),
                       cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
        elif handedness:
            # Show that left hand was detected but not processing
            cv2.putText(img, f"Hand: {handedness} (Not tracking)", (10, 30),
                       cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
        
        cv2.imshow("Image", img)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
    
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()