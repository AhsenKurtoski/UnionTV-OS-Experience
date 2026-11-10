#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

FRAMES="docs/launch/frames"
OUT="docs/launch/uniontv-launch-15s.mp4"
POSTER="docs/launch/uniontv-launch-poster.png"
SCORE="docs/launch/uniontv-launch-score.wav"

command -v ffmpeg >/dev/null 2>&1 || {
    echo "ffmpeg is required on PATH" >&2
    exit 2
}

for f in "$FRAMES/intro.png" "$FRAMES/home.png" "$FRAMES/apps.png" \
         "$FRAMES/network.png" "$FRAMES/settings.png" "$FRAMES/final.png"; do
    [[ -f "$f" ]] || { echo "missing scene: $f" >&2; exit 2; }
done

if [[ ! -f "$SCORE" ]]; then
    python docs/launch/make_score.py
fi

cp "$FRAMES/final.png" "$POSTER"

ffmpeg -y \
  -loop 1 -framerate 30 -t 2.50 -i "$FRAMES/intro.png" \
  -loop 1 -framerate 30 -t 3.70 -i "$FRAMES/home.png" \
  -loop 1 -framerate 30 -t 3.15 -i "$FRAMES/apps.png" \
  -loop 1 -framerate 30 -t 2.75 -i "$FRAMES/network.png" \
  -loop 1 -framerate 30 -t 2.25 -i "$FRAMES/settings.png" \
  -loop 1 -framerate 30 -t 2.90 -i "$FRAMES/final.png" \
  -i "$SCORE" \
  -filter_complex "\
[0:v]scale=1344:756:flags=lanczos,zoompan=z='min(zoom+0.00034,1.042)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1280x720:fps=30,settb=AVTB,format=yuv420p[v0];\
[1:v]scale=1344:756:flags=lanczos,zoompan=z='min(zoom+0.00032,1.040)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1280x720:fps=30,settb=AVTB,format=yuv420p[v1];\
[2:v]scale=1344:756:flags=lanczos,zoompan=z='min(zoom+0.00030,1.038)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1280x720:fps=30,settb=AVTB,format=yuv420p[v2];\
[3:v]scale=1344:756:flags=lanczos,zoompan=z='min(zoom+0.00028,1.036)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1280x720:fps=30,settb=AVTB,format=yuv420p[v3];\
[4:v]scale=1344:756:flags=lanczos,zoompan=z='min(zoom+0.00026,1.034)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1280x720:fps=30,settb=AVTB,format=yuv420p[v4];\
[5:v]scale=1344:756:flags=lanczos,zoompan=z='min(zoom+0.00030,1.038)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1280x720:fps=30,settb=AVTB,format=yuv420p[v5];\
[v0][v1]xfade=transition=fade:duration=0.45:offset=2.05[x1];\
[x1][v2]xfade=transition=fade:duration=0.45:offset=5.30[x2];\
[x2][v3]xfade=transition=fade:duration=0.45:offset=8.00[x3];\
[x3][v4]xfade=transition=fade:duration=0.45:offset=10.30[x4];\
[x4][v5]xfade=transition=fade:duration=0.45:offset=12.10[vx];\
[vx]fade=t=in:st=0:d=0.45,fade=t=out:st=14.35:d=0.65[vout]" \
  -map "[vout]" -map 6:a -t 15.0 -r 30 \
  -c:v libx264 -preset slow -crf 17 -pix_fmt yuv420p \
  -c:a aac -b:a 192k -ar 48000 -movflags +faststart -shortest \
  "$OUT"

ffmpeg -y -i "$OUT" -vf "fps=1/2.5,scale=426:-1,tile=3x2" \
  -frames:v 1 docs/launch/contact-sheet.png >/dev/null 2>&1

echo "Wrote $OUT"
ffprobe -v error -show_entries stream=codec_name,width,height,r_frame_rate \
  -show_entries format=duration,size -of default=noprint_wrappers=1 "$OUT"
