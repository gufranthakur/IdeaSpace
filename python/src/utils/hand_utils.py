import numpy as np
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from config.gestures_config import WRIST, MIDDLE_PIP, SMOOTHING_FACTOR

def distance_2d(p1, p2):
    """Calculate 2D Euclidean distance"""
    return np.sqrt((p1.x - p2.x)**2 + (p1.y - p2.y)**2)

def get_hand_size(landmarks):
    """Get hand size for normalization"""
    size = distance_2d(landmarks[WRIST], landmarks[MIDDLE_PIP])
    return size if size > 0 else 1.0

def get_normalized_distance(landmarks, idx1, idx2):
    """Get distance normalized by hand size"""
    hand_size = get_hand_size(landmarks)
    dist = distance_2d(landmarks[idx1], landmarks[idx2])
    normalized = dist / hand_size
    return min(normalized, 2.0)

def smooth_value(current, previous, factor=SMOOTHING_FACTOR):
    """Smooth values to reduce flicker"""
    if previous is None:
        return current
    return previous * factor + current * (1 - factor)

def is_in_range(value, min_val, max_val):
    """Check if value is strictly within range"""
    return min_val <= value <= max_val