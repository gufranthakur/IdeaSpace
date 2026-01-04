import cv2

# Canvas colors (BGR format for OpenCV)
COLORS = {
    'RED': (0, 0, 255),
    'GREEN': (0, 255, 0),
    'BLUE': (255, 0, 0),
    'YELLOW': (0, 255, 255),
    'MAGENTA': (255, 0, 255),
    'CYAN': (255, 255, 0),
    'WHITE': (255, 255, 255),
    'BLACK': (0, 0, 0),
}

COLOR_NAMES = list(COLORS.keys())

# Brush settings
BRUSH_SIZES = {
    'SMALL': 3,
    'MEDIUM': 5,
    'LARGE': 8,
    'XLARGE': 12
}

BRUSH_SIZE_NAMES = list(BRUSH_SIZES.keys())

# Default settings
DEFAULT_COLOR = 'BLUE'
DEFAULT_BRUSH_SIZE = 'MEDIUM'

# Eraser settings
ERASER_SIZE = 300

# Drawing smoothing
DRAWING_SMOOTHING = 1

# UI settings
UI_PADDING = 20
UI_SWATCH_SIZE = 40
UI_SPACING = 10

# Gesture thresholds
COLOR_SELECT_HOLD_FRAMES = 5
DRAW_SMOOTHNESS_THRESHOLD = 3

# Drawing gesture thresholds
INDEX_EXTENSION_THRESHOLD = 80  # Distance from palm center for index finger to be considered "up"
THUMB_EXTENSION_THRESHOLD = 60  # Distance from palm center for thumb to be considered "up"
FINGER_EXTENSION_THRESHOLD = 80  # Distance from palm center for middle/ring/pinky to be considered extended

# Index finger pointing forward detection (for draw gesture)
# This is the average 2D distance between index finger joints
# Lower value = stricter (finger must point more directly at camera)
# Higher value = more lenient (captures tilted/rotated finger in any direction)
# Recommended range: 50-120
INDEX_POINTING_FORWARD_THRESHOLD = 80

# History settings
MAX_UNDO_STEPS = 20