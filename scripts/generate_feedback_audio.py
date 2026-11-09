"""Generate the tiny, soft notification chimes bundled by Justagem."""

import math
import os
import struct
import wave


SAMPLE_RATE = 24000
OUTPUT = os.path.join("app", "src", "main", "res", "raw")


def make_wav(filename, notes):
    pcm = bytearray()
    for frequency, duration, gap in notes:
        count = round(SAMPLE_RATE * duration)
        for index in range(count):
            t = index / SAMPLE_RATE
            fade_in = min(1.0, t / 0.012)
            fade_out = min(1.0, (duration - t) / 0.055)
            decay = math.exp(-2.6 * t / duration)
            envelope = max(0.0, min(fade_in, fade_out)) * decay
            fundamental = math.sin(2 * math.pi * frequency * t)
            overtone = 0.16 * math.sin(2 * math.pi * frequency * 2.01 * t)
            sample = round(32767 * 0.38 * envelope * (fundamental + overtone))
            pcm.extend(struct.pack("<h", max(-32768, min(32767, sample))))
        pcm.extend(b"\x00\x00" * round(SAMPLE_RATE * gap))

    with wave.open(os.path.join(OUTPUT, filename), "wb") as audio:
        audio.setnchannels(1)
        audio.setsampwidth(2)
        audio.setframerate(SAMPLE_RATE)
        audio.writeframes(pcm)


os.makedirs(OUTPUT, exist_ok=True)
make_wav("cue_message_sent.wav", [(740, 0.075, 0.0)])
# Response feedback:
# C4 (thinking) - B4 (answer lead-in) - E4 (answer stream) - G4 then C5 (answer end).
# Thinking start/end no longer have a sound; their haptic cues remain in the UI.
make_wav("cue_answer_lead_in.wav", [(493.88, 0.07, 0.0)])
make_wav("cue_answer_completed.wav", [(392.00, 0.13, 0.035), (523.25, 0.18, 0.0)])
make_wav("cue_text_character.wav", [(329.63, 0.042, 0.0)])
make_wav("cue_thought_character.wav", [(261.63, 0.042, 0.0)])
