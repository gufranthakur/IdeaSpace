import math
import sys
import threading
import time

from gesture_config import *

# ============== GLOBAL CONFIGURATION ==============
# Pinch Zoom (single hand) settings
PINCH_STABLE_FRAMES = 3
PINCH_CLOSE_THRESHOLD = 0.07    # Below this = ZOOM OUT (normalized to frame width)
PINCH_OPEN_THRESHOLD = 0.14     # Above this = ZOOM IN (normalized to frame width)
PINCH_MAX_DISTANCE = 150        # Max pixel distance for valid pinch pose
PINCH_MIN_THUMB_EXTENSION = 30  # Min thumb extension in pixels

# Two-hand Expand gesture settings
EXPAND_STABLE_FRAMES = 3                # Frames to hold pose before tracking
EXPAND_CLOSE_THRESHOLD = 0.30           # Palms "close" if distance < this (normalized) - ~30% of frame width
EXPAND_FAR_THRESHOLD = 0.55             # Palms "far" if distance > this (normalized) - ~55% of frame width
EXPAND_MOVEMENT_THRESHOLD = 0.10        # Min movement to trigger expand (normalized) - reduced for easier trigger
EXPAND_MAX_TIME = 1.0                   # Max seconds to complete expand gesture - increased
EXPAND_COOLDOWN_FRAMES = 15             # Cooldown after triggering expand
EXPAND_HISTORY_SIZE = 5                 # Number of distance samples to track

# Threading settings
THREAD_UPDATE_INTERVAL = 0.016          # ~60 FPS for hand tracking thread
# ==================================================


class ZoomGesture:
    def __init__(self):
        # Pinch zoom state (single hand - thumb + index finger)
        self.pinch_active = False
        self.pinch_stable_frames = 0
        self.last_pinch_action = None
        self.pinch_cooldown = 0
        
        # Two-hand expand state
        self.expand_active = False
        self.expand_stable_frames = 0
        self.expand_start_distance = None
        self.expand_start_time = None
        self.expand_cooldown = 0
        self.distance_history = []
        self.last_expand_action = None
        
        # Threading for two-hand detection
        self.left_lms = []
        self.right_lms = []
        self.img_shape = (480, 640, 3)
        self.lock = threading.Lock()
        self.running = False
        self.thread = None
        self.expand_result = None

    def detect_pinch_zoom(self, lms, img):
        """
        Detect single-hand pinch zoom using thumb and INDEX finger.
        Middle, ring, pinky must be closed.
        Based on ABSOLUTE distance:
        - Thumb and index CLOSE together = ZOOM OUT
        - Thumb and index FAR apart = ZOOM IN
        """
        if len(lms) == 0:
            self._reset_pinch()
            return None

        if self.pinch_cooldown > 0:
            self.pinch_cooldown -= 1
            return None

        h, w, c = img.shape

        if not self._is_pinch_pose(lms):
            self._reset_pinch()
            return None

        thumb_x, thumb_y = lms[THUMB][1], lms[THUMB][2]
        index_x, index_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

        current_distance = math.sqrt((index_x - thumb_x) ** 2 + (index_y - thumb_y) ** 2) / w

        self.pinch_stable_frames += 1
        if self.pinch_stable_frames < PINCH_STABLE_FRAMES:
            return None

        self.pinch_active = True

        if current_distance < PINCH_CLOSE_THRESHOLD:
            if self.last_pinch_action != "ZOOM OUT":
                self.last_pinch_action = "ZOOM OUT"
            return "ZOOM OUT"

        elif current_distance > PINCH_OPEN_THRESHOLD:
            if self.last_pinch_action != "ZOOM IN":
                self.last_pinch_action = "ZOOM IN"
            return "ZOOM IN"

        return None

    def _is_pinch_pose(self, lms):
        """
        Check if hand is in a valid pinch pose:
        - Thumb and INDEX finger are the active pinch fingers
        - Middle, Ring, and Pinky must be CLOSED/curled
        - Thumb and Index must be CLOSE ENOUGH to be a pinch (not pointing)
        """
        if len(lms) < 21:
            return False

        middle_tip_y = lms[MIDDLE_FINGER][2]
        middle_mcp_y = lms[MIDDLE_POINT][2]
        ring_tip_y = lms[RING_FINGER][2]
        ring_mcp_y = lms[RING_POINT][2]
        pinky_tip_y = lms[PINKY_FINGER][2]
        pinky_mcp_y = lms[PINKY_POINT][2]

        middle_closed = middle_tip_y > middle_mcp_y - 20
        ring_closed = ring_tip_y > ring_mcp_y - 20
        pinky_closed = pinky_tip_y > pinky_mcp_y - 20

        if not (middle_closed and ring_closed and pinky_closed):
            return False

        thumb_tip_x, thumb_tip_y = lms[THUMB][1], lms[THUMB][2]
        index_tip_x, index_tip_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

        thumb_index_dist = math.sqrt((thumb_tip_x - index_tip_x) ** 2 + (thumb_tip_y - index_tip_y) ** 2)

        if thumb_index_dist > PINCH_MAX_DISTANCE:
            return False

        thumb_mcp_x, thumb_mcp_y = lms[2][1], lms[2][2]
        thumb_extension = math.sqrt((thumb_tip_x - thumb_mcp_x) ** 2 + (thumb_tip_y - thumb_mcp_y) ** 2)

        if thumb_extension < PINCH_MIN_THUMB_EXTENSION:
            return False

        return True

    def _reset_pinch(self):
        self.pinch_active = False
        self.pinch_stable_frames = 0
        self.last_pinch_action = None

    def reset(self):
        self._reset_pinch()
        self._reset_expand()

    def is_active(self):
        return self.pinch_active or self.expand_active

    # ============== TWO-HAND EXPAND GESTURE ==============
    
    def start_expand_thread(self):
        """Start background thread for two-hand expand detection"""
        if not self.running:
            self.running = True
            self.thread = threading.Thread(target=self._expand_thread_loop, daemon=True)
            self.thread.start()
    
    def stop_expand_thread(self):
        """Stop the background thread"""
        self.running = False
        if self.thread:
            self.thread.join(timeout=1.0)
            self.thread = None
    
    def update_hands(self, left_lms, right_lms, img_shape):
        """Update hand landmarks from main thread (thread-safe)"""
        with self.lock:
            self.left_lms = left_lms.copy() if left_lms else []
            self.right_lms = right_lms.copy() if right_lms else []
            self.img_shape = img_shape
    
    def get_expand_result(self):
        """Get expand result from background thread (thread-safe)"""
        with self.lock:
            result = self.expand_result
            self.expand_result = None
            return result
    
    def _expand_thread_loop(self):
        """Background thread loop for expand detection"""
        while self.running:
            with self.lock:
                left = self.left_lms
                right = self.right_lms
                shape = self.img_shape
            
            result = self._detect_expand(left, right, shape)
            
            if result:
                with self.lock:
                    self.expand_result = result
            
            time.sleep(THREAD_UPDATE_INTERVAL)
    
    def _detect_expand(self, left_lms, right_lms, img_shape):
        """
        Detect two-hand expand gesture:
        - Both hands open, palms facing each other
        - Hands close together -> move apart = "EXPAND OUT"
        - Hands far apart -> move together = "EXPAND IN"
        """
        if self.expand_cooldown > 0:
            self.expand_cooldown -= 1
            return None
        
        if len(left_lms) == 0 or len(right_lms) == 0:
            self._reset_expand()
            return None
        
        h, w = img_shape[0], img_shape[1]
        
        # Check if both hands are in valid expand pose (open palms)
        if not (self._is_open_palm(left_lms) and self._is_open_palm(right_lms)):
            self._reset_expand()
            return None
        
        # Get palm center points (using middle finger MCP - landmark 9)
        left_palm_x = left_lms[MIDDLE_POINT][1]
        left_palm_y = left_lms[MIDDLE_POINT][2]
        right_palm_x = right_lms[MIDDLE_POINT][1]
        right_palm_y = right_lms[MIDDLE_POINT][2]
        
        # Calculate normalized distance between palms
        current_distance = math.sqrt(
            (right_palm_x - left_palm_x) ** 2 + 
            (right_palm_y - left_palm_y) ** 2
        ) / w
        
        # Track distance history
        self.distance_history.append(current_distance)
        if len(self.distance_history) > EXPAND_HISTORY_SIZE:
            self.distance_history.pop(0)
        
        # Need stable frames before starting to track
        if not self.expand_active:
            self.expand_stable_frames += 1
            if self.expand_stable_frames >= EXPAND_STABLE_FRAMES:
                self.expand_active = True
                self.expand_start_distance = current_distance
                self.expand_start_time = time.time()
            return None
        
        # Check for timeout
        if time.time() - self.expand_start_time > EXPAND_MAX_TIME:
            # Reset but keep tracking if hands still visible
            self.expand_start_distance = current_distance
            self.expand_start_time = time.time()
            self.distance_history = [current_distance]
            return None
        
        # Calculate movement from start
        distance_change = current_distance - self.expand_start_distance
        
        # Detect expand direction based on movement
        if abs(distance_change) >= EXPAND_MOVEMENT_THRESHOLD:
            # Started close, now far = EXPAND OUT
            if self.expand_start_distance < EXPAND_CLOSE_THRESHOLD and distance_change > 0:
                self._reset_expand()
                self.expand_cooldown = EXPAND_COOLDOWN_FRAMES
                return "EXPAND OUT"
            
            # Started far, now close = EXPAND IN
            elif self.expand_start_distance > EXPAND_FAR_THRESHOLD and distance_change < 0:
                self._reset_expand()
                self.expand_cooldown = EXPAND_COOLDOWN_FRAMES
                return "EXPAND IN"
            
            # Movement detected but didn't start from correct position
            # Reset and start tracking from current position
            self.expand_start_distance = current_distance
            self.expand_start_time = time.time()
        
        return None
    
    def _is_open_palm(self, lms):
        """Check if hand is an open palm (at least 4 fingers extended)"""
        if len(lms) < 21:
            return False
        
        # Check finger extension (tip higher than MCP joint)
        index_extended = lms[INDEX_FINGER][2] < lms[INDEX_POINT][2] + 20
        middle_extended = lms[MIDDLE_FINGER][2] < lms[MIDDLE_POINT][2] + 20
        ring_extended = lms[RING_FINGER][2] < lms[RING_POINT][2] + 20
        pinky_extended = lms[PINKY_FINGER][2] < lms[PINKY_POINT][2] + 20
        
        extended_count = sum([index_extended, middle_extended, ring_extended, pinky_extended])
        return extended_count >= 3
    
    def _reset_expand(self):
        """Reset expand gesture state"""
        self.expand_active = False
        self.expand_stable_frames = 0
        self.expand_start_distance = None
        self.expand_start_time = None
        self.distance_history = []
    
    def is_expand_active(self):
        """Check if expand gesture is being tracked"""
        return self.expand_active