import cv2
import mediapipe as mp
from mediapipe.tasks.python import BaseOptions
from mediapipe.tasks.python.vision import HandLandmarker, HandLandmarkerOptions, RunningMode
import time

import os
_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(_DIR, "hand_landmarker.task")
# Download from:
# https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task


class _FakeLandmark:
    """Mimics mediapipe NormalizedLandmark — has .x .y .z"""
    __slots__ = ("x", "y", "z")

    def __init__(self, x, y, z=0.0):
        self.x = x
        self.y = y
        self.z = z


class _FakeHandLandmarks:
    """Mimics mediapipe HandLandmarks — iterable, has .landmark list"""
    def __init__(self, landmarks):
        self.landmark = landmarks   # list of _FakeLandmark

    def __iter__(self):
        return iter(self.landmark)


class _FakeClassification:
    def __init__(self, label):
        self.label = label


class _FakeHandedness:
    """Mimics mediapipe ClassificationList — has .classification[0].label"""
    def __init__(self, label):
        self.classification = [_FakeClassification(label)]


class _FakeResults:
    """
    Mimics the old mp.solutions.hands result object.
    Fields used externally:
      .multi_hand_landmarks  -> list of _FakeHandLandmarks  (or None)
      .multi_handedness      -> list of _FakeHandedness     (or None)
    """
    def __init__(self, multi_hand_landmarks=None, multi_handedness=None):
        self.multi_hand_landmarks = multi_hand_landmarks
        self.multi_handedness = multi_handedness


class handDetector:
    def __init__(self, mode=False, maxHands=2, detectionConfidence=0.5, trackConfidence=0.5):
        self.maxHands = maxHands
        self.results = _FakeResults()   # always populated after find_hands()

        options = HandLandmarkerOptions(
            base_options=BaseOptions(model_asset_path=MODEL_PATH),
            running_mode=RunningMode.VIDEO,
            num_hands=maxHands,
            min_hand_detection_confidence=detectionConfidence,
            min_hand_presence_confidence=detectionConfidence,
            min_tracking_confidence=trackConfidence,
        )
        self._landmarker = HandLandmarker.create_from_options(options)
        self._frame_ts = 0      # monotonic ms counter for VIDEO mode

        # Hand connections for drawing (hardcoded — mp.solutions removed in 0.10.x)
        self._HAND_CONNECTIONS = [
            (0,1),(1,2),(2,3),(3,4),         # thumb
            (0,5),(5,6),(6,7),(7,8),         # index
            (5,9),(9,10),(10,11),(11,12),    # middle
            (9,13),(13,14),(14,15),(15,16),  # ring
            (13,17),(17,18),(18,19),(19,20), # pinky
            (0,17),                          # palm
        ]

    def find_hands(self, img, drawHands=True):
        rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)

        # VIDEO mode requires a strictly increasing timestamp in ms
        self._frame_ts += 1
        detection = self._landmarker.detect_for_video(mp_image, self._frame_ts)

        if detection.hand_landmarks:
            fake_lms_list = []
            fake_hand_list = []

            for hand_lms, handedness_list in zip(detection.hand_landmarks,
                                                  detection.handedness):
                # Build fake landmark objects
                fake_lms = _FakeHandLandmarks(
                    [_FakeLandmark(lm.x, lm.y, lm.z) for lm in hand_lms]
                )
                fake_lms_list.append(fake_lms)

                # Tasks API handedness label is flipped vs the old API — fix it
                raw_label = handedness_list[0].category_name   # "Left" or "Right"
                corrected = "Left" if raw_label == "Right" else "Right"
                fake_hand_list.append(_FakeHandedness(corrected))

                if drawHands:
                    h, w, _ = img.shape
                    # Draw connections manually — no proto needed
                    pts = [(int(lm.x * w), int(lm.y * h)) for lm in hand_lms]
                    for a, b in self._HAND_CONNECTIONS:
                        cv2.line(img, pts[a], pts[b], (0, 255, 0), 2)
                    for pt in pts:
                        cv2.circle(img, pt, 4, (255, 0, 0), -1)

            self.results = _FakeResults(fake_lms_list, fake_hand_list)
        else:
            self.results = _FakeResults()

        return img

    def find_position(self, img, hand_number=0, draw=True):
        landmark_list = []
        if self.results.multi_hand_landmarks:
            if hand_number < len(self.results.multi_hand_landmarks):
                h, w, _ = img.shape
                for id, lm in enumerate(self.results.multi_hand_landmarks[hand_number].landmark):
                    cx, cy = int(lm.x * w), int(lm.y * h)
                    landmark_list.append([id, cx, cy])
                    if draw:
                        cv2.circle(img, (cx, cy), 7, (255, 0, 0), -1, cv2.LINE_AA)
        return landmark_list

    def get_handedness(self, hand_number=0):
        """Returns 'Right' or 'Left' for the detected hand"""
        if self.results and self.results.multi_handedness:
            if hand_number < len(self.results.multi_handedness):
                return self.results.multi_handedness[hand_number].classification[0].label
        return None

    def close(self):
        """Call when done to release the landmarker"""
        self._landmarker.close()


def main():
    cap = cv2.VideoCapture(0)
    detector = handDetector()

    while True:
        success, img = cap.read()
        if not success:
            continue
        img = cv2.flip(img, 1)
        img = detector.find_hands(img)

        handedness = detector.get_handedness(0)
        if handedness == "Right":
            lms = detector.find_position(img)
            cv2.putText(img, f"Hand: {handedness}", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
        elif handedness:
            cv2.putText(img, f"Hand: {handedness} (Not tracking)", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

        cv2.imshow("Image", img)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    detector.close()
    cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
