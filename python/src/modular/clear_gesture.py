import math
import sys
sys.path.append('..')

from modular.gesture_config import *


class SplitGesture:
    def __init__(self):
        self.ready_pose_detected = False
        self.ready_pose_frames = 0
        self.initial_thumb_middle_dist = None
        self.split_detected = False
        self.cooldown = 0
        self.READY_FRAMES_REQUIRED = 4
        self.SNAP_THRESHOLD_RATIO = 0.12
        self.MAX_FRAMES_AFTER_READY = 15
        self.frames_since_ready = 0
        self.pose_stability_check = []

    def detect(self, lms, img):
        """Detect split gesture: thumb-middle circle → rapid snap apart"""

        if self.cooldown > 0:
            self.cooldown -= 1
            return None

        if len(lms) == 0:
            return None

        thumb_middle_dist = self._get_distance(lms, THUMB, MIDDLE_FINGER, img)
        is_ready_pose = self._is_ready_pose(lms, thumb_middle_dist, img)

        if not self.ready_pose_detected:
            if is_ready_pose:
                self.ready_pose_frames += 1
                self.pose_stability_check.append(thumb_middle_dist)

                if len(self.pose_stability_check) > 3:
                    self.pose_stability_check.pop(0)
                    dist_variance = max(self.pose_stability_check) - min(self.pose_stability_check)
                    if dist_variance > 0.02:
                        self.ready_pose_frames = 0
                        self.pose_stability_check = []
                        return None

                if self.ready_pose_frames >= self.READY_FRAMES_REQUIRED:
                    self.ready_pose_detected = True
                    self.initial_thumb_middle_dist = thumb_middle_dist
                    self.frames_since_ready = 0
            else:
                self.ready_pose_frames = 0
                self.pose_stability_check = []
            return None

        if self.ready_pose_detected and not self.split_detected:
            self.frames_since_ready += 1
            distance_increase = thumb_middle_dist - self.initial_thumb_middle_dist

            if self.frames_since_ready > self.MAX_FRAMES_AFTER_READY:
                self._reset()
                return None

            if distance_increase > self.SNAP_THRESHOLD_RATIO:
                self.split_detected = True
                self._reset()
                self.cooldown = 10
                return "SPLIT"

        return None

    def _is_ready_pose(self, lms, thumb_middle_dist, img):
        """Check if thumb-middle are touching and other fingers are extended"""

        TOUCH_THRESHOLD = 0.05
        if thumb_middle_dist > TOUCH_THRESHOLD:
            return False

        index_tip_y = lms[INDEX_FINGER][2]
        index_mcp_y = lms[INDEX_POINT][2]
        index_tip_x = lms[INDEX_FINGER][1]
        index_mcp_x = lms[INDEX_POINT][1]

        index_dy = abs(index_tip_y - index_mcp_y)
        index_dx = abs(index_tip_x - index_mcp_x)

        if index_dx > index_dy:
            return False

        index_extended = lms[INDEX_FINGER][2] < lms[INDEX_POINT][2]
        ring_extended = lms[RING_FINGER][2] < lms[RING_POINT][2]
        pinky_extended = lms[PINKY_FINGER][2] < lms[PINKY_POINT][2]

        extended_count = sum([index_extended, ring_extended, pinky_extended])

        if extended_count < 3:
            return False

        thumb_tip_y = lms[THUMB][2]
        thumb_mcp_y = lms[1][2]
        thumb_tip_x = lms[THUMB][1]
        thumb_mcp_x = lms[1][1]

        thumb_dy = abs(thumb_tip_y - thumb_mcp_y)
        thumb_dx = abs(thumb_tip_x - thumb_mcp_x)

        if thumb_dx > thumb_dy * 2:
            return False

        return True

    def _get_distance(self, lms, idx1, idx2, img):
        """Calculate normalized distance between two landmarks"""
        h, w, c = img.shape
        x1, y1 = lms[idx1][1], lms[idx1][2]
        x2, y2 = lms[idx2][1], lms[idx2][2]

        pixel_dist = math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2)
        return pixel_dist / w

    def _reset(self):
        self.ready_pose_detected = False
        self.ready_pose_frames = 0
        self.initial_thumb_middle_dist = None
        self.split_detected = False
        self.frames_since_ready = 0
        self.pose_stability_check = []

    def reset(self):
        self._reset()
        self.cooldown = 0

    def is_active(self):
        return self.ready_pose_detected