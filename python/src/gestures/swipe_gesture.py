import math
from config.gestures_config import *
from utils.finger_state import is_finger_extended

class SwipeGesture:
    def __init__(self):
        self.start_x = None
        self.start_y = None
        self.swipe_active = False
        self.swipe_frames = 0
        self.cooldown = 0
        self.swipe_detected = False
        self.validation_failures = 0
        self.MAX_VALIDATION_FAILURES = 5
        self.position_history = []
        self.last_swipe_direction = None
        self.direction_cooldown = 0
        self.MIN_SWIPE_FRAMES = 5
        self.peak_velocity = 0
    
    def detect(self, landmarks):
        """Detect swipe gestures from anywhere in frame"""
        if self.cooldown > 0:
            self.cooldown -= 1
            if not self._validate_swipe_pose(landmarks):
                self.cooldown = 0
                self.swipe_detected = False
            return None
        
        if self.direction_cooldown > 0:
            self.direction_cooldown -= 1
        
        is_valid_pose = self._validate_swipe_pose(landmarks)
        
        if self.swipe_active:
            if not is_valid_pose:
                self.validation_failures += 1
                if self.validation_failures > self.MAX_VALIDATION_FAILURES:
                    self._reset()
                    return None
            else:
                self.validation_failures = 0
        else:
            if not is_valid_pose:
                return None
        
        current_x = landmarks[12].x
        current_y = landmarks[12].y
        
        if not self.swipe_active:
            self.start_x = current_x
            self.start_y = current_y
            self.swipe_active = True
            self.swipe_frames = 0
            self.swipe_detected = False
            self.position_history = [(current_x, current_y)]
            self.peak_velocity = 0
            return None
        
        self.swipe_frames += 1
        self.position_history.append((current_x, current_y))
        
        if self.swipe_frames < self.MIN_SWIPE_FRAMES or self.swipe_detected:
            return None
        
        delta_x = current_x - self.start_x
        delta_y = current_y - self.start_y
        distance = math.sqrt(delta_x**2 + delta_y**2)
        
        # Check velocity
        current_velocity = 0
        if len(self.position_history) >= 5:
            prev_x, prev_y = self.position_history[-5]
            recent_dist = math.sqrt((current_x - prev_x)**2 + (current_y - prev_y)**2)
            current_velocity = recent_dist / 5
            
            if current_velocity > self.peak_velocity:
                self.peak_velocity = current_velocity
            
            if current_velocity < 0.010:  # More lenient
                if self.swipe_frames > 20 and self.peak_velocity < 0.012:  # More lenient
                    self._reset()
                return None
        
        DISTANCE_THRESHOLD = 0.12  # Reduced from 0.15
        VERTICAL_THRESHOLD = 0.12
        RATIO_THRESHOLD = 1.8  # Reduced from 2.0
        
        # SWIPE LEFT: Hand moves RIGHT (positive delta_x)
        if delta_x > DISTANCE_THRESHOLD and abs(delta_y) < VERTICAL_THRESHOLD:
            if abs(delta_x) > abs(delta_y) * RATIO_THRESHOLD:
                if self.peak_velocity < 0.010:  # More lenient velocity
                    return None
                
                if self.last_swipe_direction == "right" and self.direction_cooldown > 0:
                    return None
                    
                self.swipe_detected = True
                self.cooldown = 25  # Reduced cooldown
                self.last_swipe_direction = "left"
                self.direction_cooldown = 10  # Reduced direction cooldown
                self._reset()
                return ("swipe_left", distance)
        
        # SWIPE RIGHT: Hand moves LEFT (negative delta_x)
        elif delta_x < -DISTANCE_THRESHOLD and abs(delta_y) < VERTICAL_THRESHOLD:
            if abs(delta_x) > abs(delta_y) * RATIO_THRESHOLD:
                if self.peak_velocity < 0.010:  # More lenient velocity
                    return None
                
                if self.last_swipe_direction == "left" and self.direction_cooldown > 0:
                    return None
                    
                self.swipe_detected = True
                self.cooldown = 25  # Reduced cooldown
                self.last_swipe_direction = "right"
                self.direction_cooldown = 10  # Reduced direction cooldown
                self._reset()
                return ("swipe_right", distance)
        
        if self.swipe_frames > 50:
            self._reset()
        
        return None
    
    def _validate_swipe_pose(self, landmarks):
        """Validate: thumb + 4 fingers extended AND HORIZONTAL orientation (NOT vertical like drag)"""
        
        thumb_to_wrist_x = abs(landmarks[THUMB_TIP].x - landmarks[WRIST].x)
        thumb_to_wrist_y = abs(landmarks[THUMB_TIP].y - landmarks[WRIST].y)
        
        thumb_extended = thumb_to_wrist_x > 0.10 or thumb_to_wrist_y > 0.18
        
        if not thumb_extended:
            return False
        
        palm_base_x = landmarks[0].x
        palm_base_y = landmarks[0].y
        
        fingers_extended = 0
        for tip_idx in [INDEX_TIP, MIDDLE_TIP, RING_TIP, PINKY_TIP]:
            dist = math.sqrt((landmarks[tip_idx].x - palm_base_x)**2 + 
                           (landmarks[tip_idx].y - palm_base_y)**2)
            if dist > 0.20:
                fingers_extended += 1
        
        if fingers_extended < 3:
            return False
        
        # KEY FIX: Reject VERTICAL fingers (drag pose)
        # For swipe, fingers should be HORIZONTAL (sideways)
        index_tip = landmarks[INDEX_TIP]
        index_mcp = landmarks[INDEX_MCP]
        middle_tip = landmarks[MIDDLE_TIP]
        middle_mcp = landmarks[MIDDLE_MCP]
        
        # Check if fingers are pointing UP (vertical - this is drag pose)
        index_dy = abs(index_tip.y - index_mcp.y)
        middle_dy = abs(middle_tip.y - middle_mcp.y)
        index_dx = abs(index_tip.x - index_mcp.x)
        middle_dx = abs(middle_tip.x - middle_mcp.x)
        
        # REJECT if vertical orientation (drag pose) - less strict now
        # Vertical means: dy > dx AND tips above bases
        fingers_pointing_up = (index_tip.y < index_mcp.y) and (middle_tip.y < middle_mcp.y)
        vertical_dominant = (index_dy > index_dx * 1.8) and (middle_dy > middle_dx * 1.8)  # Less strict
        
        if fingers_pointing_up and vertical_dominant:
            return False  # This is a drag pose, not swipe
        
        return True
    
    def _reset(self):
        self.start_x = None
        self.start_y = None
        self.swipe_active = False
        self.swipe_frames = 0
        self.swipe_detected = False
        self.validation_failures = 0
        self.position_history = []
        self.peak_velocity = 0
    
    def reset(self):
        self._reset()
        self.cooldown = 0
        self.direction_cooldown = 0
        self.last_swipe_direction = None