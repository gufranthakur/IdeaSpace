from gestures.zoom_gesture import ZoomGesture
from gestures.swipe_gesture import SwipeGesture
from gestures.drag_gesture import DragGesture

class GestureManager:
    def __init__(self):
        self.zoom = ZoomGesture()
        self.swipe = SwipeGesture()
        self.drag = DragGesture()
        self.global_cooldown = 0
        self.last_gesture_type = None
        self.debug_mode = False
    
    def detect(self, landmarks):
        """Detect all gestures with priority"""
    
        if self.global_cooldown > 0:
            self.global_cooldown -= 1
            return None
    
        # Check swipe FIRST (to prevent drag from blocking)
        swipe_result = self.swipe.detect(landmarks)
        if swipe_result:
            self.last_gesture_type = "swipe"
            self.global_cooldown = 10
            self.zoom.reset()
            self.drag.reset()
            return swipe_result
        
        # Check drag SECOND
        drag_result = self.drag.detect(landmarks)
        if drag_result:
            self.last_gesture_type = "drag"
            self.global_cooldown = 10
            self.zoom.reset()
            self.swipe.reset()
            return drag_result
        
        # If drag is in progress, skip zoom check entirely
        if self.drag.is_active():
            self.zoom.reset()
            return None
    
        # Check zoom LAST (only if drag is not active)
        zoom_result = self.zoom.detect(landmarks)
        if zoom_result:
            self.last_gesture_type = "zoom"
            self.global_cooldown = 5
            self.swipe.reset()
            return zoom_result
    
        return None
    
    def reset(self):
        """Reset all gesture detectors"""
        self.zoom.reset()
        self.swipe.reset()
        self.drag.reset()
        self.global_cooldown = 0
        self.last_gesture_type = None