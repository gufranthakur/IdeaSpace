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
ERASER_SIZE = 50

# Drawing smoothing
DRAWING_SMOOTHING = 3  # Number of points to average

# UI settings
UI_PADDING = 20
UI_SWATCH_SIZE = 40
UI_SPACING = 10

# Gesture thresholds
COLOR_SELECT_HOLD_FRAMES = 5  # Frames to hold gesture before cycling color
DRAW_SMOOTHNESS_THRESHOLD = 5  # Min pixel movement to draw

# History settings
MAX_UNDO_STEPS = 20
