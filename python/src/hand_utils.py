import cv2
import math

# Swipe detection variables
swipe_history = []
max_history = 10  # Number of frames to track
swipe_threshold = 70  # Minimum distance to consider a swipe
swipe_detected = False
swipe_direction = ""

def distance_between_points(point1, point2, color, img, lms):
    x1, y1 = lms[point1][1], lms[point1][2]
    x2, y2 = lms[point2][1], lms[point2][2]
    cx, cy = (x1+x2) //2 , (y1+y2) //2

    ## Circle 1
    cv2.circle(img, (x1, y1), 8, color, -1, cv2.LINE_AA) 
    cv2.circle(img, (x1, y1), 8, (255, 255, 255), 2, cv2.LINE_AA)

    ## Circle 2
    cv2.circle(img, (x2, y2), 8, color, -1, cv2.LINE_AA) 
    cv2.circle(img, (x2, y2), 8, (255, 255, 255), 2, cv2.LINE_AA)

    ## Line between them
    cv2.line(img, (x1, y1), (x2, y2), color, 3)

    ## Circle on the line
    cv2.circle(img,(cx, cy), 8, color, -1, cv2.LINE_AA)
    cv2.circle(img,(cx, cy), 8, (255, 255, 255), 2, cv2.LINE_AA)

    ## Calculate length between the points 
    length = math.isqrt((x2 - x1)**2 + (y2 - y1)**2)

    return length

def action_between_threshold(length, threshold_1, threshold2):
    return length > threshold_1 and length < threshold2

def detect_swipe(current_x, current_y):
    global swipe_history, swipe_detected, swipe_direction
    
    # Add current position to history
    swipe_history.append((current_x, current_y))
    
    # Keep only the last max_history positions
    if len(swipe_history) > max_history:
        swipe_history.pop(0)
    
    # Need at least 2 positions to detect movement
    if len(swipe_history) < max_history:
        return None
    
    # Calculate total horizontal and vertical displacement
    start_x, start_y = swipe_history[0]
    end_x, end_y = swipe_history[-1]
    
    dx = end_x - start_x
    dy = end_y - start_y
    
    # Check if horizontal movement is significant and dominant
    if abs(dx) > swipe_threshold and abs(dx) > abs(dy) * 1.5:
        if dx > 0:
            swipe_direction = "RIGHT"
            swipe_detected = True
            swipe_history.clear()  # Reset after detection
            return "RIGHT"
        else:
            swipe_direction = "LEFT"
            swipe_detected = True
            swipe_history.clear()  # Reset after detection
            return "LEFT"
    
    # Check for vertical swipes
    elif abs(dy) > swipe_threshold and abs(dy) > abs(dx) * 1.5:
        if dy > 0:
            swipe_direction = "DOWN"
            swipe_detected = True
            swipe_history.clear()
            return "DOWN"
        else:
            swipe_direction = "UP"
            swipe_detected = True
            swipe_history.clear()
            return "UP"
    
    return None
