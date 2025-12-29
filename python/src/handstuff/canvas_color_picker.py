import cv2
import numpy as np
from canvas_config import *


class UnifiedColorBrushUI:
    """Combined color picker and brush slider in one box"""

    def __init__(self):
        self.is_active = False
        self.ui_x = 100
        self.ui_y = 100
        self.ui_width = 450
        self.ui_height = 320

        # Color swatches
        self.base_colors = [
            [(0, 0, 255), (0, 255, 0), (255, 0, 0), (0, 255, 255), (255, 0, 255), (255, 255, 0)],
            [(180, 180, 255), (180, 255, 180), (255, 180, 180), (180, 255, 255), (255, 180, 255), (255, 255, 180)],
            [(0, 0, 128), (0, 128, 0), (128, 0, 0), (128, 128, 0), (128, 0, 128), (0, 128, 128)],
            [(64, 64, 64), (128, 128, 128), (192, 192, 192), (255, 255, 255), (0, 0, 0), (255, 128, 0)]
        ]
        self.swatch_size = 38
        self.selected_color = (255, 0, 0)

        # Brush slider
        self.min_size = 2
        self.max_size = 25
        self.current_size = 5
        self.slider_bar_y = 0
        self.slider_bar_x_start = 0
        self.slider_bar_x_end = 0
        self.slider_bar_height = 12

    def show(self, img_width, img_height, current_brush_size, current_color):
        self.is_active = True
        self.current_size = current_brush_size
        self.selected_color = current_color

        # Center the unified UI
        self.ui_x = (img_width - self.ui_width) // 2
        self.ui_y = (img_height - self.ui_height) // 2

        # Slider bar position (bottom of UI)
        self.slider_bar_x_start = self.ui_x + 40
        self.slider_bar_x_end = self.ui_x + self.ui_width - 40
        self.slider_bar_y = self.ui_y + self.ui_height - 50

    def hide(self):
        self.is_active = False

    def handle_point(self, x, y):
        """Handle color selection and brush size adjustment with any finger"""
        if not self.is_active:
            return None

        # Check color swatches
        start_x = self.ui_x + 20
        start_y = self.ui_y + 50
        spacing = 6

        for row_idx, row in enumerate(self.base_colors):
            for col_idx, color in enumerate(row):
                swatch_x = start_x + col_idx * (self.swatch_size + spacing)
                swatch_y = start_y + row_idx * (self.swatch_size + spacing)

                if (swatch_x <= x <= swatch_x + self.swatch_size and
                        swatch_y <= y <= swatch_y + self.swatch_size):
                    self.selected_color = color
                    return ("COLOR", color)

        # Check brush slider
        slider_top = self.slider_bar_y - 30
        slider_bottom = self.ui_y + self.ui_height - 20

        if (self.slider_bar_x_start <= x <= self.slider_bar_x_end and
                slider_top <= y <= slider_bottom):
            x_clamped = max(self.slider_bar_x_start, min(x, self.slider_bar_x_end))
            ratio = (x_clamped - self.slider_bar_x_start) / (self.slider_bar_x_end - self.slider_bar_x_start)
            new_size = int(self.min_size + ratio * (self.max_size - self.min_size))
            self.current_size = new_size
            return ("SIZE", new_size)

        return None

    def draw(self, img):
        if not self.is_active:
            return img

        # Background
        overlay = img.copy()
        cv2.rectangle(overlay,
                      (self.ui_x, self.ui_y),
                      (self.ui_x + self.ui_width, self.ui_y + self.ui_height),
                      (30, 30, 30), -1)
        cv2.addWeighted(overlay, 0.92, img, 0.08, 0, img)

        # Border
        cv2.rectangle(img,
                      (self.ui_x, self.ui_y),
                      (self.ui_x + self.ui_width, self.ui_y + self.ui_height),
                      (200, 200, 200), 3)

        # Title
        cv2.putText(img, "COLOR & BRUSH SIZE",
                    (self.ui_x + 90, self.ui_y + 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        # Draw color swatches
        start_x = self.ui_x + 20
        start_y = self.ui_y + 50
        spacing = 6

        for row_idx, row in enumerate(self.base_colors):
            for col_idx, color in enumerate(row):
                swatch_x = start_x + col_idx * (self.swatch_size + spacing)
                swatch_y = start_y + row_idx * (self.swatch_size + spacing)

                # Shadow
                cv2.rectangle(img,
                              (swatch_x + 2, swatch_y + 2),
                              (swatch_x + self.swatch_size + 2, swatch_y + self.swatch_size + 2),
                              (0, 0, 0), -1)

                # Color swatch
                cv2.rectangle(img,
                              (swatch_x, swatch_y),
                              (swatch_x + self.swatch_size, swatch_y + self.swatch_size),
                              color, -1)

                # Selected indicator
                if color == self.selected_color:
                    cv2.rectangle(img,
                                  (swatch_x - 4, swatch_y - 4),
                                  (swatch_x + self.swatch_size + 4, swatch_y + self.swatch_size + 4),
                                  (0, 255, 255), 3)

                # Border
                cv2.rectangle(img,
                              (swatch_x, swatch_y),
                              (swatch_x + self.swatch_size, swatch_y + self.swatch_size),
                              (150, 150, 150), 1)

        # Brush size label
        cv2.putText(img, f"BRUSH SIZE: {self.current_size}",
                    (self.ui_x + 30, self.slider_bar_y - 15),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

        # Slider bar background
        cv2.rectangle(img,
                      (self.slider_bar_x_start, self.slider_bar_y - self.slider_bar_height // 2),
                      (self.slider_bar_x_end, self.slider_bar_y + self.slider_bar_height // 2),
                      (80, 80, 80), -1)

        # Slider bar fill
        ratio = (self.current_size - self.min_size) / (self.max_size - self.min_size)
        knob_x = int(self.slider_bar_x_start + ratio * (self.slider_bar_x_end - self.slider_bar_x_start))

        cv2.rectangle(img,
                      (self.slider_bar_x_start, self.slider_bar_y - self.slider_bar_height // 2),
                      (knob_x, self.slider_bar_y + self.slider_bar_height // 2),
                      (0, 200, 255), -1)

        # Slider knob
        cv2.circle(img, (knob_x, self.slider_bar_y), 16, (255, 255, 255), -1)
        cv2.circle(img, (knob_x, self.slider_bar_y), 16, (0, 200, 255), 4)

        return img

    def get_color(self):
        return self.selected_color

    def get_size(self):
        return self.current_size
