import cv2
import mediapipe as mp
from gesture_manager import GestureManager

# Initialize MediaPipe Hands
mp_hands = mp.solutions.hands
mp_drawing = mp.solutions.drawing_utils
hands = mp_hands.Hands(
    static_image_mode=False,
    max_num_hands=1,
    min_detection_confidence=0.7,
    min_tracking_confidence=0.7
)

# Initialize gesture manager
gesture_manager = GestureManager()

# Camera setup
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

# Display state
last_gesture_text = ""
last_details_text = ""

print("=" * 60)
print("HAND GESTURE RECOGNITION")
print("=" * 60)
print("ZOOM IN:    Thumb + Middle finger (EXPAND)")
print("ZOOM OUT:   Thumb + Index finger (CONTRACT)")
print("SWIPE:      All 5 fingers straight, hand sideways")
print("            Move hand LEFT/RIGHT for swipe gestures")
print("DRAG:       Open hand on right -> Make fist -> Move left")
print()
print("Press 'q' to quit")
print("=" * 60)

while cap.isOpened():
    success, frame = cap.read()
    if not success:
        continue
    
    frame = cv2.flip(frame, 1)
    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    
    results = hands.process(rgb_frame)
    
    if results.multi_hand_landmarks:
        for hand_landmarks in results.multi_hand_landmarks:
            mp_drawing.draw_landmarks(
                frame, 
                hand_landmarks, 
                mp_hands.HAND_CONNECTIONS
            )
            
            gesture = gesture_manager.detect(hand_landmarks.landmark)
            
            if gesture:
                gesture_name = gesture[0]
                
                # Handle different gesture types
                if "swipe" in gesture_name or "drag" in gesture_name:
                    distance = gesture[1]
                    print(f"{gesture_name.upper()}")
                    last_gesture_text = f"{gesture_name.replace('_', ' ').upper()}"
                    last_details_text = f"Distance: {distance:.3f}"
                else:  # zoom gestures
                    scale = gesture[1]
                    distance = gesture[2]
                    print(f"{gesture_name.upper()}")
                    last_gesture_text = f"{gesture_name.replace('_', ' ').upper()}"
                    last_details_text = f"Distance: {distance:.3f} | Scale: {scale:.2f}x"
            else:
                last_gesture_text = ""
                last_details_text = ""
    else:
        gesture_manager.reset()
        last_gesture_text = ""
        last_details_text = ""
    
    # Display text
    if last_gesture_text:
        cv2.putText(frame, last_gesture_text, (10, 40), 
                   cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 255, 0), 3)
        cv2.putText(frame, last_details_text, (10, 80), 
                   cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
    
    cv2.imshow('Hand Gesture Recognition', frame)
    
    if cv2.waitKey(10) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
hands.close()
print("\nApplication closed")