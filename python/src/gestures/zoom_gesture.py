from config.gestures_config import *
from utils.hand_utils import get_normalized_distance, is_in_range, smooth_value
from utils.finger_state import validate_zoom_in_fingers, validate_zoom_out_fingers

class ZoomGesture:
    def __init__(self):
        self.smooth_thumb_middle = None
        self.smooth_thumb_index = None
        self.active_gesture = None
        self.gesture_start_distance = None
        self.stable_count = 0
        self.stable_pose_count = 0  # Track consecutive valid poses
    
    def detect(self, landmarks):
        """Detect zoom gestures"""
        raw_thumb_middle = get_normalized_distance(landmarks, THUMB_TIP, MIDDLE_TIP)
        raw_thumb_index = get_normalized_distance(landmarks, THUMB_TIP, INDEX_TIP)
        
        self.smooth_thumb_middle = smooth_value(raw_thumb_middle, self.smooth_thumb_middle)
        self.smooth_thumb_index = smooth_value(raw_thumb_index, self.smooth_thumb_index)
        
        zoom_in_fingers_valid = validate_zoom_in_fingers(landmarks)
        zoom_out_fingers_valid = validate_zoom_out_fingers(landmarks)
        
        # Additional check: ensure index is actually EXTENDED for zoom out
        # This prevents fist from triggering zoom out
        if zoom_out_fingers_valid:
            index_tip = landmarks[INDEX_TIP]
            index_pip = landmarks[INDEX_PIP]
            index_mcp = landmarks[INDEX_MCP]
            
            # Index must be clearly extended (tip beyond PIP, and PIP beyond MCP)
            index_extended = (index_tip.y < index_pip.y) and (index_pip.y < index_mcp.y)
            
            if not index_extended:
                zoom_out_fingers_valid = False
        
        # Zoom OUT has priority
        if zoom_out_fingers_valid and is_in_range(self.smooth_thumb_index, ZOOM_OUT_MIN, ZOOM_OUT_MAX):
            return self._handle_zoom_out(self.smooth_thumb_index, landmarks)
        
        # Zoom IN
        elif zoom_in_fingers_valid and is_in_range(self.smooth_thumb_middle, ZOOM_IN_MIN, ZOOM_IN_MAX):
            return self._handle_zoom_in(self.smooth_thumb_middle, landmarks)
        
        else:
            self._reset()
        
        return None
    
    def _handle_zoom_in(self, distance, landmarks):
        if not validate_zoom_in_fingers(landmarks):
            self._reset()
            return None
        
        if self.active_gesture != "zoom_in":
            self.active_gesture = "zoom_in"
            self.gesture_start_distance = distance
            self.stable_count = 0
            self.stable_pose_count = 0
        
        self.stable_count += 1
        self.stable_pose_count += 1
        
        # Require pose to be held longer before triggering
        if self.stable_count >= STABLE_FRAMES and self.stable_pose_count >= 5:
            scale = self._calculate_scale(distance)
            return ("zoom_in", scale, distance)
        
        return None
    
    def _handle_zoom_out(self, distance, landmarks):
        # Stricter validation for zoom out
        if not validate_zoom_out_fingers(landmarks):
            self._reset()
            return None
        
        # Additional check: index must be clearly extended
        index_tip = landmarks[INDEX_TIP]
        index_pip = landmarks[INDEX_PIP]
        index_mcp = landmarks[INDEX_MCP]
        index_extended = (index_tip.y < index_pip.y) and (index_pip.y < index_mcp.y)
        
        if not index_extended:
            self._reset()
            return None
        
        if self.active_gesture != "zoom_out":
            self.active_gesture = "zoom_out"
            self.gesture_start_distance = distance
            self.stable_count = 0
            self.stable_pose_count = 0
        
        self.stable_count += 1
        self.stable_pose_count += 1
        
        # Require stable pose for multiple frames before triggering
        if self.stable_count >= 3 and self.stable_pose_count >= 5:
            scale = self._calculate_scale(distance)
            return ("zoom_out", scale, distance)
        
        return None
    
    def _calculate_scale(self, current_distance):
        if not self.gesture_start_distance or self.gesture_start_distance == 0:
            return 1.0
        scale = current_distance / self.gesture_start_distance
        return max(MIN_SCALE, min(scale, MAX_SCALE))
    
    def _reset(self):
        self.active_gesture = None
        self.gesture_start_distance = None
        self.stable_count = 0
        self.stable_pose_count = 0
    
    def reset(self):
        self.smooth_thumb_middle = None
        self.smooth_thumb_index = None
        self._reset()