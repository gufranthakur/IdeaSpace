import math
from config.gestures_config import *
from utils.finger_state import is_finger_extended

class DragGesture:
    def __init__(self):
        self.start_x = None
        self.open_hand_detected = False
        self.fist_made = False
        self.drag_active = False
        self.cooldown = 0
        self.initial_fist_x = None
        self.idle_frames = 0
        self.MAX_IDLE_FRAMES = 60
        self.fist_confidence = 0
    
    def is_active(self):
        return self.open_hand_detected or self.fist_made or self.drag_active
        
    def detect(self, landmarks):
        if self.cooldown > 0:
            self.cooldown -= 1
            return None
        
        current_x = landmarks[12].x
        hand_open = self._is_hand_open(landmarks)
        hand_fist = self._is_fist(landmarks)
        
        if hand_open or hand_fist:
            self.idle_frames = 0
        else:
            self.idle_frames += 1
            if self.idle_frames > self.MAX_IDLE_FRAMES:
                self._reset()
            return None
        
        # State 1: Detect open hand
        if not self.open_hand_detected and hand_open:
            self.open_hand_detected = True
            self.start_x = current_x
            self.fist_confidence = 0
            return None
        
        # State 2: Detect fist after open hand
        if self.open_hand_detected and not self.fist_made:
            if hand_fist:
                self.fist_confidence += 1
                if self.fist_confidence >= 2:
                    self.fist_made = True
                    self.initial_fist_x = current_x
                    self.drag_active = True
            else:
                self.fist_confidence = 0
            return None
        
        # State 3: Detect leftward movement with fist
        if self.drag_active and self.fist_made and self.initial_fist_x is not None:
            delta_x = current_x - self.initial_fist_x
            
            if delta_x < -0.06:
                distance = abs(delta_x)
                self._reset()
                self.cooldown = 15
                return ("drag", distance)
            
            if hand_open:
                self._reset()
                return None
            
            if current_x > self.initial_fist_x + 0.03:
                self._reset()
        
        return None
    
    def _is_hand_open(self, landmarks):
        """Check if all 5 fingers extended WITH VERTICAL orientation"""
        
        thumb_tip = landmarks[THUMB_TIP]
        wrist = landmarks[WRIST]
        
        thumb_dist = math.dist(
            (thumb_tip.x, thumb_tip.y),
            (wrist.x, wrist.y)
        )
        thumb_extended = thumb_dist > 0.15
        
        palm_x = landmarks[0].x
        palm_y = landmarks[0].y
        open_fingers = 0
        
        for tip in [INDEX_TIP, MIDDLE_TIP, RING_TIP, PINKY_TIP]:
            d = math.dist((landmarks[tip].x, landmarks[tip].y), (palm_x, palm_y))
            if d > 0.18:
                open_fingers += 1
        
        if not (thumb_extended and open_fingers >= 3):
            return False
        
        index_tip = landmarks[INDEX_TIP]
        index_mcp = landmarks[INDEX_MCP]
        middle_tip = landmarks[MIDDLE_TIP]
        middle_mcp = landmarks[MIDDLE_MCP]
        
        index_dy = abs(index_tip.y - index_mcp.y)
        middle_dy = abs(middle_tip.y - middle_mcp.y)
        index_dx = abs(index_tip.x - index_mcp.x)
        middle_dx = abs(middle_tip.x - middle_mcp.x)
        
        if index_dx > index_dy or middle_dx > middle_dy:
            return False
        
        fingers_pointing_up = (index_tip.y < index_mcp.y) and (middle_tip.y < middle_mcp.y)
        
        return fingers_pointing_up and index_dy > 0.08 and middle_dy > 0.08
    
    def _is_fist(self, landmarks):
        """Check if hand is closed - MORE LENIENT"""
        
        wrist = landmarks[WRIST]
        palm_x = landmarks[0].x
        palm_y = landmarks[0].y
        
        closed_fingers = 0
        for tip in [INDEX_TIP, MIDDLE_TIP, RING_TIP, PINKY_TIP]:
            d = math.dist((landmarks[tip].x, landmarks[tip].y), (palm_x, palm_y))
            if d < 0.18:
                closed_fingers += 1
        
        fingers_curled = 0
        for tip, mcp in [(INDEX_TIP, INDEX_MCP), (MIDDLE_TIP, MIDDLE_MCP), 
                         (RING_TIP, RING_MCP), (PINKY_TIP, PINKY_MCP)]:
            tip_mcp_dist = math.dist(
                (landmarks[tip].x, landmarks[tip].y),
                (landmarks[mcp].x, landmarks[mcp].y)
            )
            if tip_mcp_dist < 0.12:
                fingers_curled += 1
        
        thumb_dist = math.dist(
            (landmarks[THUMB_TIP].x, landmarks[THUMB_TIP].y),
            (wrist.x, wrist.y)
        )
        thumb_tucked = thumb_dist < 0.16
        
        return (closed_fingers >= 3 or 
                fingers_curled >= 3 or 
                (closed_fingers >= 2 and thumb_tucked) or
                (fingers_curled >= 2 and thumb_tucked))
    
    def _reset(self):
        self.start_x = None
        self.open_hand_detected = False
        self.fist_made = False
        self.drag_active = False
        self.initial_fist_x = None
        self.idle_frames = 0
        self.fist_confidence = 0
    
    def reset(self):
        self._reset()
        self.cooldown = 0