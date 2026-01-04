import cv2
import numpy as np
import sys
sys.path.append('..')

from collections import deque
from modular.canvas_config import *
from modular.canvas_color_picker import UnifiedColorBrushUI
from modular.server import Server


class CanvasDrawing:
    def __init__(self, img_shape, server):
        self.h, self.w = img_shape[0], img_shape[1]
        self.canvas = np.zeros((self.h, self.w, 3), dtype=np.uint8)
        self.server = server

        self.current_color = COLORS[DEFAULT_COLOR]
        self.current_color_name = DEFAULT_COLOR
        self.current_brush_size = BRUSH_SIZES[DEFAULT_BRUSH_SIZE]
        self.prev_point = None
        self.point_history = deque(maxlen=DRAWING_SMOOTHING)

        self.history = []
        self.history_index = -1
        self.save_state()

        self.is_erasing = False
        self.has_drawn = False

        self.unified_ui = UnifiedColorBrushUI()

        self.ui_just_closed = False
        self.ui_closed_frames = 0
        self.UI_GRACE_FRAMES = 10

        self.stroke_points = []
        self.stroke_start_time = None
        self.shape_autocorrect_enabled = True

        self.is_drawing_active = False

    def draw_point(self, x, y):
        current_point = (x, y)
        self.stroke_points.append([x, y])

        if self.prev_point is not None:
            if self.is_erasing:
                cv2.line(self.canvas, self.prev_point, current_point, (0, 0, 0), ERASER_SIZE, cv2.LINE_AA)
            else:
                cv2.line(self.canvas, self.prev_point, current_point, self.current_color,
                         max(self.current_brush_size * 2, 4), cv2.LINE_AA)
            self.has_drawn = True

        self.prev_point = current_point
        self._send_draw_command(x, y)

    def _send_draw_command(self, x, y):
        mode = "ERASE" if self.is_erasing else "DRAW"
        norm_x = x / self.w
        norm_y = y / self.h
        r, g, b = self.current_color

        size = ERASER_SIZE if self.is_erasing else self.current_brush_size
        command = f"CANVAS {mode} {norm_x:.4f} {norm_y:.4f} {r} {g} {b} {size}"
        self.server.send_command(command)

        if not self.is_drawing_active:
            self.is_drawing_active = True

    def reset_drawing(self):
        if self.has_drawn and len(self.stroke_points) > 10 and self.shape_autocorrect_enabled:
            self._detect_and_correct_shape()

        if self.has_drawn:
            self.save_state()
            self.has_drawn = False

        if self.is_drawing_active:
            self.server.send_command("CANVAS END")
            self.is_drawing_active = False

        self.prev_point = None
        self.point_history.clear()
        self.stroke_points = []
        self.stroke_start_time = None

    def set_erase_mode(self, erasing):
        if self.is_erasing != erasing:
            self.is_erasing = erasing
            self.reset_drawing()

    def open_unified_ui(self):
        self.unified_ui.show(self.w, self.h, self.current_brush_size, self.current_color)

    def handle_ui_point(self, x, y):
        if not self.unified_ui.is_active:
            return False

        result = self.unified_ui.handle_point(x, y)
        if result:
            action_type, value = result
            if action_type == "COLOR":
                self.current_color = value
                self.current_color_name = "CUSTOM"
            elif action_type == "SIZE":
                self.current_brush_size = value
            return True
        return False

    def close_unified_ui(self):
        self.unified_ui.hide()
        self.ui_just_closed = True
        self.ui_closed_frames = 0

    def clear_canvas(self):
        self.canvas = np.zeros((self.h, self.w, 3), dtype=np.uint8)
        self.reset_drawing()
        self.save_state()
        self.server.send_command("CANVAS CLEAR")

    def save_state(self):
        self.history = self.history[:self.history_index + 1]
        self.history.append(self.canvas.copy())
        if len(self.history) > MAX_UNDO_STEPS:
            self.history.pop(0)
        else:
            self.history_index += 1

    def undo(self):
        if self.history_index > 0:
            self.history_index -= 1
            self.canvas = self.history[self.history_index].copy()
            self.reset_drawing()
            return True
        return False

    def _detect_and_correct_shape(self):
        if len(self.stroke_points) < 15 or self.is_erasing:
            return

        points = np.array(self.stroke_points)
        start = points[0]
        end = points[-1]
        line_length = np.sqrt((end[0] - start[0]) ** 2 + (end[1] - start[1]) ** 2)

        if line_length > 50:
            total_dev = sum(np.linalg.norm(
                pt - (start + ((np.dot(pt - start, end - start) / np.dot(end - start, end - start)) * (end - start))))
                            for pt in points)
            avg_dev = total_dev / len(points)

            if avg_dev < 10:
                temp_canvas = self.canvas.copy()
                self.canvas = self.history[self.history_index].copy()
                cv2.line(self.canvas, tuple(start.astype(int)), tuple(end.astype(int)),
                         self.current_color, max(self.current_brush_size * 2, 4), cv2.LINE_AA)
                return

        loop_dist = np.sqrt((end[0] - start[0]) ** 2 + (end[1] - start[1]) ** 2)
        if loop_dist < 60:
            center = np.mean(points, axis=0).astype(int)
            radii = [np.sqrt((pt[0] - center[0]) ** 2 + (pt[1] - center[1]) ** 2) for pt in points]
            avg_radius = np.mean(radii)
            std_radius = np.std(radii)

            if avg_radius > 20 and std_radius < avg_radius * 0.3:
                self.canvas = self.history[self.history_index].copy()
                cv2.circle(self.canvas, tuple(center), int(avg_radius),
                           self.current_color, max(self.current_brush_size * 2, 4), cv2.LINE_AA)
                return

    def get_overlay(self, camera_img):
        gray_canvas = cv2.cvtColor(self.canvas, cv2.COLOR_BGR2GRAY)
        _, mask = cv2.threshold(gray_canvas, 1, 255, cv2.THRESH_BINARY)
        mask_inv = cv2.bitwise_not(mask)

        canvas_fg = cv2.bitwise_and(self.canvas, self.canvas, mask=mask)
        camera_bg = cv2.bitwise_and(camera_img, camera_img, mask=mask_inv)

        result = cv2.add(camera_bg, canvas_fg)
        return result

    def draw_ui(self, img, pointer_pos=None):
        cv2.rectangle(img, (UI_PADDING, UI_PADDING),
                      (UI_PADDING + UI_SWATCH_SIZE, UI_PADDING + UI_SWATCH_SIZE),
                      self.current_color, -1)
        cv2.rectangle(img, (UI_PADDING, UI_PADDING),
                      (UI_PADDING + UI_SWATCH_SIZE, UI_PADDING + UI_SWATCH_SIZE),
                      (255, 255, 255), 2)

        mode_text = "ERASE" if self.is_erasing else "DRAW"
        cv2.putText(img, mode_text,
                    (UI_PADDING, UI_PADDING + UI_SWATCH_SIZE + 25),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 2)

        cv2.putText(img, f"Size: {self.current_brush_size}",
                    (UI_PADDING, UI_PADDING + UI_SWATCH_SIZE + 50),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 2)

        if self.unified_ui.is_active:
            img = self.unified_ui.draw(img)
            if pointer_pos:
                x, y = pointer_pos
                cv2.circle(img, (x, y), 10, (0, 255, 0), 2)
                cv2.circle(img, (x, y), 3, (0, 255, 0), -1)

        return img