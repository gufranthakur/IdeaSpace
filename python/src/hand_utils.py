import cv2
import math

# Swipe detection variables
swipe_history = []
max_history = 10
swipe_threshold = 70
swipe_detected = False
swipe_direction = ""


def detect_swipe(current_x, current_y):
    global swipe_history, swipe_detected, swipe_direction

    swipe_history.append((current_x, current_y))

    if len(swipe_history) > max_history:
        swipe_history.pop(0)

    if len(swipe_history) < max_history:
        return None

    start_x, start_y = swipe_history[0]
    end_x, end_y = swipe_history[-1]

    dx = end_x - start_x
    dy = end_y - start_y

    if abs(dx) > swipe_threshold and abs(dx) > abs(dy) * 1.5:
        if dx > 0:
            swipe_direction = "RIGHT"
            swipe_detected = True
            swipe_history.clear()
            return "RIGHT"
        else:
            swipe_direction = "LEFT"
            swipe_detected = True
            swipe_history.clear()
            return "LEFT"

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
