def smooth_position(position_history, np, current_pos):
    """Apply moving average smoothing to position"""
    position_history.append(current_pos)
    if len(position_history) < 2:
        return current_pos

    positions = np.array(position_history)
    return tuple(np.mean(positions, axis=0).astype(int))


def calculate_velocity(current_pos, prev_pos):
    """Calculate velocity for more responsive movement"""
    if prev_pos is None:
        return 0, 0
    dx = current_pos[0] - prev_pos[0]
    dy = current_pos[1] - prev_pos[1]
    return dx, dy


def get_direction_from_velocity(np, DEAD_ZONE, dx, dy):
    """Determine direction with velocity-based approach"""
    magnitude = np.sqrt(dx ** 2 + dy ** 2)

    if magnitude < DEAD_ZONE:
        return None

    angle = np.arctan2(dy, dx) * 180 / np.pi

    if -22.5 <= angle < 22.5:
        return "RIGHT"
    elif 22.5 <= angle < 67.5:
        return "BOTTOM-RIGHT"
    elif 67.5 <= angle < 112.5:
        return "BOTTOM"
    elif 112.5 <= angle < 157.5:
        return "BOTTOM-LEFT"
    elif angle >= 157.5 or angle < -157.5:
        return "LEFT"
    elif -157.5 <= angle < -112.5:
        return "TOP-LEFT"
    elif -112.5 <= angle < -67.5:
        return "TOP"
    elif -67.5 <= angle < -22.5:
        return "TOP-RIGHT"

    return None
