import cv2
import numpy as np
import math
from canvas_config import *

# Interpolation settings
MAX_POINT_DISTANCE = 15  # If points are further apart, interpolate between them


class CanvasDrawing:
    def __init__(self, img_shape, server, debug_mode=False):
        self.h, self.w = img_shape[0], img_shape[1]
        self.server = server
        self.debug_mode = debug_mode

        # Only create canvas in debug mode
        if self.debug_mode:
            self.canvas = np.zeros((self.h, self.w, 3), dtype=np.uint8)

        self.prev_point = None
        self.is_erasing = False
        self.is_drawing_active = False

    def draw_point(self, x, y):
        """Draw a point/line - only draws locally in debug mode, always sends to server"""
        current_point = (x, y)

        if self.prev_point is not None:
            dist = math.sqrt((x - self.prev_point[0]) ** 2 + (y - self.prev_point[1]) ** 2)

            if dist > MAX_POINT_DISTANCE:
                # Interpolate points
                num_points = int(dist / MAX_POINT_DISTANCE) + 1
                for i in range(1, num_points + 1):
                    t = i / num_points
                    interp_x = int(self.prev_point[0] + t * (x - self.prev_point[0]))
                    interp_y = int(self.prev_point[1] + t * (y - self.prev_point[1]))
                    if self.debug_mode:
                        self._draw_segment(self.prev_point, (interp_x, interp_y))
                    self._send_draw_command(interp_x, interp_y)
                    self.prev_point = (interp_x, interp_y)
            else:
                if self.debug_mode:
                    self._draw_segment(self.prev_point, current_point)
                self._send_draw_command(x, y)

        self.prev_point = current_point

        if not self.is_drawing_active:
            self.is_drawing_active = True

    def _draw_segment(self, p1, p2):
        """Draw a single line segment on canvas (debug mode only)"""
        if self.is_erasing:
            cv2.line(self.canvas, p1, p2, (0, 0, 0), ERASER_SIZE)
        else:
            cv2.line(self.canvas, p1, p2, DRAW_COLOR, BRUSH_SIZE)

    def _send_draw_command(self, x, y):
        """Send draw command to server"""
        mode = "ERASE" if self.is_erasing else "DRAW"
        norm_x = x / self.w
        norm_y = y / self.h
        r, g, b = DRAW_COLOR
        size = ERASER_SIZE if self.is_erasing else BRUSH_SIZE

        command = f"CANVAS {mode} {norm_x:.4f} {norm_y:.4f} {r} {g} {b} {size}"
        self.server.send_command(command)

    def reset_drawing(self):
        """Reset drawing state (called when finger lifted)"""
        if self.is_drawing_active:
            self.server.send_command("CANVAS END")
            self.is_drawing_active = False

        self.prev_point = None

    def set_erase_mode(self, erasing):
        """Switch between draw and erase mode"""
        if self.is_erasing != erasing:
            self.is_erasing = erasing
            self.reset_drawing()

    def clear_canvas(self):
        """Clear the canvas"""
        if self.debug_mode:
            self.canvas = np.zeros((self.h, self.w, 3), dtype=np.uint8)
        self.reset_drawing()
        self.server.send_command("CANVAS CLEAR")

    def get_overlay(self, camera_img):
        """Overlay canvas on camera image (debug mode only)"""
        gray_canvas = cv2.cvtColor(self.canvas, cv2.COLOR_BGR2GRAY)
        _, mask = cv2.threshold(gray_canvas, 1, 255, cv2.THRESH_BINARY)
        mask_inv = cv2.bitwise_not(mask)

        canvas_fg = cv2.bitwise_and(self.canvas, self.canvas, mask=mask)
        camera_bg = cv2.bitwise_and(camera_img, camera_img, mask=mask_inv)

        return cv2.add(camera_bg, canvas_fg)

    def draw_debug_ui(self, img):
        """Draw minimal debug info"""
        mode_text = "ERASE" if self.is_erasing else "DRAW"
        cv2.putText(img, mode_text, (20, 65),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 2)
        return img
