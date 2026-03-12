"""
drag_menu.py  –  Floating button menu with finger-mouse.
Click = point index finger at button, then SPREAD all 5 fingers open.
"""

import cv2
import time
import math

from gesture_config import (
    INDEX_FINGER, THUMB,
    MIDDLE_FINGER, RING_FINGER, PINKY_FINGER,
    INDEX_POINT, MIDDLE_POINT, RING_POINT, PINKY_POINT,
)

# ── Button definitions ────────────────────────────────────────────────────────
MENU_BUTTONS = [
    {"label": "Raspberry Pi", "id": "RASPBERRY_PI"},
    {"label": "Arduino",      "id": "ARDUINO"},
    {"label": "ESP32",        "id": "ESP32"},
    {"label": "Jetson Nano",  "id": "JETSON_NANO"},
    {"label": "STM32",        "id": "STM32"},
    {"label": "BeagleBone",   "id": "BEAGLEBONE"},
]

# ── Visual ────────────────────────────────────────────────────────────────────
MENU_W         = 270
BUTTON_H       = 50
BUTTON_PADDING = 10
MENU_MARGIN    = 20

COLOR_BG       = (25,  25,  25)
COLOR_BORDER   = (90,  90,  90)
COLOR_BTN      = (55,  55,  55)
COLOR_BTN_HOV  = (60, 120, 210)
COLOR_BTN_SEL  = (30, 190,  80)
COLOR_TEXT     = (230, 230, 230)

CURSOR_DOT_R   = 10
CURSOR_RING_R  = 20
COLOR_DOT      = (0, 255, 180)
COLOR_RING     = (255, 255, 255)
COLOR_DOT_OPEN = (0, 80, 255)      # dot turns orange when open-hand detected

# Open-hand: all 4 fingers extended above their knuckle
FINGER_EXTEND_PX = 15   # tip must be this many px ABOVE its knuckle (smaller y)
CLICK_COOLDOWN   = 0.6


class DragMenu:
    def __init__(self):
        self._open          = False
        self._buttons       = []
        self._hovered       = -1
        self._last_selected = None
        self._close_at      = None
        self._click_cd_until = 0.0
        self._was_open_hand  = False   # edge-detect: fire once per spread

    def open(self):
        self._open           = True
        self._buttons        = []
        self._hovered        = -1
        self._last_selected  = None
        self._close_at       = None
        self._click_cd_until = 0.0
        self._was_open_hand  = False

    def close(self):
        self._open = False

    def is_open(self):
        return self._open

    def update(self, img, lms):
        if not self._open:
            return None

        # delayed close after green flash
        if self._close_at and time.time() >= self._close_at:
            self._open = False
            result = self._last_selected
            self._last_selected = None
            return result

        h, w = img.shape[:2]
        self._compute_buttons(w, h)

        # ── finger position ───────────────────────────────────────────────────
        finger_x = finger_y = None
        if len(lms) > INDEX_FINGER:
            finger_x = lms[INDEX_FINGER][1]
            finger_y = lms[INDEX_FINGER][2]

        # ── open-hand detection ───────────────────────────────────────────────
        is_open_hand = False
        if len(lms) > PINKY_FINGER:
            pairs = [
                (INDEX_FINGER,  INDEX_POINT),
                (MIDDLE_FINGER, MIDDLE_POINT),
                (RING_FINGER,   RING_POINT),
                (PINKY_FINGER,  PINKY_POINT),
            ]
            extended = sum(
                1 for tip, knuckle in pairs
                if lms[tip][2] < lms[knuckle][2] - FINGER_EXTEND_PX
            )
            # thumb: tip x far from wrist x
            thumb_out = abs(lms[THUMB][1] - lms[0][1]) > w * 0.08
            is_open_hand = extended >= 4 and thumb_out

        # edge-detect: rising edge of open hand = click
        now = time.time()
        clicked = False
        if is_open_hand and not self._was_open_hand and now >= self._click_cd_until:
            clicked              = True
            self._click_cd_until = now + CLICK_COOLDOWN
        self._was_open_hand = is_open_hand

        # ── hit-test ──────────────────────────────────────────────────────────
        self._hovered = -1
        if finger_x is not None:
            for i, btn in enumerate(self._buttons):
                bx, by, bw, bh = btn["rect"]
                if bx <= finger_x <= bx + bw and by <= finger_y <= by + bh:
                    self._hovered = i
                    break

        # ── draw panel ────────────────────────────────────────────────────────
        overlay = img.copy()
        panel_x = MENU_MARGIN
        panel_y = MENU_MARGIN
        panel_h = (BUTTON_H + BUTTON_PADDING) * len(MENU_BUTTONS) + BUTTON_PADDING + 36

        cv2.rectangle(overlay, (panel_x, panel_y),
                      (panel_x + MENU_W, panel_y + panel_h), COLOR_BG, -1)
        cv2.rectangle(overlay, (panel_x, panel_y),
                      (panel_x + MENU_W, panel_y + panel_h), COLOR_BORDER, 2)
        cv2.addWeighted(overlay, 0.85, img, 0.15, 0, img)

        cv2.putText(img, "Select Device",
                    (panel_x + 14, panel_y + 24),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.62, (180, 180, 180), 1, cv2.LINE_AA)

        # ── draw buttons ──────────────────────────────────────────────────────
        selected_id = None
        for i, btn in enumerate(self._buttons):
            bx, by, bw, bh = btn["rect"]
            color = COLOR_BTN_HOV if i == self._hovered else COLOR_BTN
            cv2.rectangle(img, (bx, by), (bx + bw, by + bh), color, -1)
            cv2.rectangle(img, (bx, by), (bx + bw, by + bh), COLOR_BORDER, 1)
            cv2.putText(img, btn["label"],
                        (bx + 12, by + bh // 2 + 7),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.56, COLOR_TEXT, 1, cv2.LINE_AA)

            if clicked and i == self._hovered:
                selected_id = btn["id"]
                cv2.rectangle(img, (bx, by), (bx + bw, by + bh), COLOR_BTN_SEL, -1)
                cv2.putText(img, "OK  " + btn["label"],
                            (bx + 12, by + bh // 2 + 7),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.56, (0, 0, 0), 2, cv2.LINE_AA)

        # ── cursor ────────────────────────────────────────────────────────────
        if finger_x is not None:
            dot_color = COLOR_DOT_OPEN if is_open_hand else COLOR_DOT
            cv2.circle(img, (finger_x, finger_y), CURSOR_RING_R, COLOR_RING, 2)
            cv2.circle(img, (finger_x, finger_y), CURSOR_DOT_R,  dot_color,  -1)

        cv2.putText(img, "Point to button, spread hand to select  |  ESC cancel",
                    (panel_x + 6, panel_y + panel_h - 7),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.34, (120, 120, 120), 1, cv2.LINE_AA)

        if selected_id:
            self._last_selected = selected_id
            self._close_at      = time.time() + 0.45
            return None

        return None

    def _compute_buttons(self, frame_w, frame_h):
        if self._buttons:
            return
        x0 = MENU_MARGIN + BUTTON_PADDING
        y0 = MENU_MARGIN + 34
        for i, b in enumerate(MENU_BUTTONS):
            by = y0 + i * (BUTTON_H + BUTTON_PADDING)
            self._buttons.append({
                "label": b["label"],
                "id":    b["id"],
                "rect":  (x0, by, MENU_W - 2 * BUTTON_PADDING, BUTTON_H),
            })
