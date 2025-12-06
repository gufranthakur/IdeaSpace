# MediaPipe hand landmark indices
WRIST = 0
THUMB_TIP = 4
THUMB_IP = 3
INDEX_TIP = 8
INDEX_PIP = 6
MIDDLE_TIP = 12
MIDDLE_PIP = 10
RING_TIP = 16
RING_PIP = 14
PINKY_TIP = 20
PINKY_PIP = 18

# ZOOM IN: Thumb + Index (EXPANDING - fingers move apart)
ZOOM_IN_MIN = 0.30  # Start position (closer)
ZOOM_IN_MAX = 0.50  # End position (farther apart)

# ZOOM OUT: Thumb + Middle (distance 0.000 to 0.200)
ZOOM_OUT_MIN = 0.000  # Fingers touching
ZOOM_OUT_MAX = 0.200  # Fingers close together

# Anti-flicker settings
MOVEMENT_THRESHOLD = 0.008  # Minimum change per frame to register movement
STABLE_FRAMES = 3           # Must be stable for 3 frames
SMOOTHING_FACTOR = 0.7      # Smooth distance changes
IDLE_TIMEOUT = 10           # Frames of no movement before reset

# Scale limits
MIN_SCALE = 0.5
MAX_SCALE = 3.0

# Add these MCP joint indices
INDEX_MCP = 5
MIDDLE_MCP = 9
RING_MCP = 13
PINKY_MCP = 17