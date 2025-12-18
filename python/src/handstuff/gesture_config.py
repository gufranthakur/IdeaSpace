from collections import deque

# Finger landmarks
THUMB = 4
INDEX_FINGER = 8
MIDDLE_FINGER = 12
PINKY_FINGER = 20
RING_FINGER = 16

MIDDLE_POINT = 9
INDEX_POINT = 5
RING_POINT = 13
PINKY_POINT = 17

# Modes
VIEW_MODE = 2
EDIT_MODE = 3
CANVAS_MODE = 4

# Colors
RED = (40, 40, 240)
GREEN = (40, 240, 40)
BLUE = (240, 40, 40)

# Performance settings
TARGET_FPS = 30
FRAME_TIME = 1.0 / TARGET_FPS

# Smoothing settings
SMOOTHING_WINDOW = 5

# Movement settings
STABLE_FRAMES = 2
MOVEMENT_THRESHOLD = 0.8
DEAD_ZONE = 3


# Swipe detection settings
SWIPE_ANGLE_THRESHOLD = 0.12  # Now a ratio of frame width (12%)
SWIPE_MAX_TIME = 1.0
SWIPE_COOLDOWN = 0.5

# Command throttling
COMMAND_SEND_INTERVAL = 0.05
