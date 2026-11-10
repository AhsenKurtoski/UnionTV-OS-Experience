#!/usr/bin/env python3
"""Original 15-second ambient score for the UnionTV launch video.

No samples, no dependencies. Writes a stereo 16-bit WAV next to this script.
The music is a slow four-chord pad with a low root, a soft shimmer and a
gentle scene-change pulse; it is intentionally quiet enough for a product
launch video.
"""

from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 44_100
DURATION = 15.0
CHANNELS = 2
OUT = Path(__file__).resolve().with_name("uniontv-launch-score.wav")

# A minor -> F major -> C major -> G major, 3.75 s each.
CHORDS = [
    (110.00, 164.81, 220.00, 261.63),
    (87.31, 130.81, 174.61, 220.00),
    (130.81, 164.81, 196.00, 261.63),
    (98.00, 146.83, 196.00, 246.94),
]
SCENE_TIMES = (2.45, 6.05, 9.0, 11.55, 13.15)
SEGMENT = DURATION / len(CHORDS)


def smoothstep(x: float) -> float:
    x = max(0.0, min(1.0, x))
    return x * x * (3.0 - 2.0 * x)


def envelope(t: float, chord_start: float) -> float:
    local = t - chord_start
    attack = smoothstep(local / 0.65)
    release = smoothstep((SEGMENT - local) / 0.65)
    return max(0.0, min(attack, release))


def pad_value(t: float) -> float:
    index = min(len(CHORDS) - 1, int(t / SEGMENT))
    local = t - index * SEGMENT
    current = CHORDS[index]
    nxt = CHORDS[min(len(CHORDS) - 1, index + 1)]
    blend = smoothstep((local - (SEGMENT - 0.5)) / 0.5)
    env = envelope(t, index * SEGMENT)

    total = 0.0
    for note_index, frequency in enumerate(current):
        next_frequency = nxt[note_index]
        f = frequency * (1.0 - blend) + next_frequency * blend
        weight = (0.36, 0.27, 0.20, 0.13)[note_index]
        # Slight detune and a soft second harmonic make the pad less static.
        tone = math.sin(2.0 * math.pi * f * t + note_index * 0.7)
        tone += 0.16 * math.sin(2.0 * math.pi * (f * 2.0) * t + 0.3)
        total += tone * weight
    return total * env


def low_root(t: float) -> float:
    index = min(len(CHORDS) - 1, int(t / SEGMENT))
    frequency = CHORDS[index][0] / 2.0
    env = envelope(t, index * SEGMENT)
    return math.sin(2.0 * math.pi * frequency * t) * env * 0.20


def shimmer(t: float) -> float:
    # A soft, high airy note that blooms at each scene cut.
    value = 0.0
    for cut in SCENE_TIMES:
        dt = t - cut
        if 0.0 <= dt <= 1.8:
            level = math.exp(-dt * 2.1) * (1.0 - math.exp(-dt * 28.0))
            value += math.sin(2.0 * math.pi * (740.0 + 40.0 * dt) * t) * level
    return value * 0.025


def scene_pulse(t: float) -> float:
    value = 0.0
    for cut in SCENE_TIMES:
        dt = t - cut
        if 0.0 <= dt <= 0.8:
            level = math.exp(-dt * 7.0) * (1.0 - math.exp(-dt * 50.0))
            value += math.sin(2.0 * math.pi * 73.42 * t) * level
    return value * 0.16


def main() -> None:
    n = int(SAMPLE_RATE * DURATION)
    frames = bytearray()
    for i in range(n):
        t = i / SAMPLE_RATE
        global_fade = min(1.0, t / 0.8) * min(1.0, (DURATION - t) / 1.1)
        left = (pad_value(t) * 0.54 + low_root(t) + shimmer(t) * 0.8 + scene_pulse(t))
        right = (pad_value(t + 0.0012) * 0.54 + low_root(t + 0.0012)
                 + shimmer(t) + scene_pulse(t + 0.0012))
        left *= global_fade
        right *= global_fade
        # Keep transients below full scale without a limiter.
        left = max(-0.92, min(0.92, left))
        right = max(-0.92, min(0.92, right))
        frames += struct.pack("<hh", int(left * 29_000), int(right * 29_000))

    with wave.open(str(OUT), "wb") as wav:
        wav.setnchannels(CHANNELS)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(frames)
    print(f"Wrote {OUT} ({OUT.stat().st_size:,} bytes, {DURATION:.0f}s)")


if __name__ == "__main__":
    main()
