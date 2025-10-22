class LandmarkSmoother:
    def __init__(self, smoothing_factor=0.5):
        self.smoothing_factor = smoothing_factor  # 0 = no smoothing, 1 = maximum smoothing
        self.prev_landmarks = []
    
    def smooth(self, landmarks):
        """Apply exponential moving average smoothing"""
        if len(self.prev_landmarks) == 0 or len(landmarks) != len(self.prev_landmarks):
            self.prev_landmarks = landmarks.copy()
            return landmarks
        
        smoothed = []
        for i in range(len(landmarks)):
            if len(landmarks[i]) >= 3:
                smooth_x = int(self.prev_landmarks[i][1] * self.smoothing_factor + 
                              landmarks[i][1] * (1 - self.smoothing_factor))
                smooth_y = int(self.prev_landmarks[i][2] * self.smoothing_factor + 
                              landmarks[i][2] * (1 - self.smoothing_factor))
                smoothed.append([landmarks[i][0], smooth_x, smooth_y])
        
        self.prev_landmarks = smoothed
        return smoothed