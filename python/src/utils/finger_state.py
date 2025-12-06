import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from config.gestures_config import *

def is_finger_extended(landmarks, tip_idx, pip_idx):
    """Check if finger is extended (tip above PIP joint)"""
    return landmarks[tip_idx].y < landmarks[pip_idx].y

def is_finger_curled(landmarks, tip_idx, pip_idx):
    """Check if finger is curled (tip below PIP joint)"""
    return landmarks[tip_idx].y > landmarks[pip_idx].y

def validate_zoom_in_fingers(landmarks):
    """Zoom In: Thumb + Middle extended, Ring + Pinky curled"""
    thumb_extended = landmarks[THUMB_TIP].x < landmarks[THUMB_IP].x
    middle_extended = is_finger_extended(landmarks, MIDDLE_TIP, MIDDLE_PIP)
    ring_curled = is_finger_curled(landmarks, RING_TIP, RING_PIP)
    pinky_curled = is_finger_curled(landmarks, PINKY_TIP, PINKY_PIP)
    
    return thumb_extended and middle_extended and ring_curled and pinky_curled

def validate_zoom_out_fingers(landmarks):
    """Zoom Out: Thumb + Index, Ring + Pinky curled"""
    index_extended = is_finger_extended(landmarks, INDEX_TIP, INDEX_PIP)
    ring_curled = is_finger_curled(landmarks, RING_TIP, RING_PIP)
    pinky_curled = is_finger_curled(landmarks, PINKY_TIP, PINKY_PIP)
    
    return index_extended and ring_curled and pinky_curled