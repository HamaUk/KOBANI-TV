import re

with open(r'c:\Users\Hama9\Desktop\ultra-tv-main\android-native\app\src\main\kotlin\com\ultratv\tv\nativeapp\ui\player\AdvancedVideoPlayer.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Remove TrackSelectionOverride
content = content.replace("import androidx.media3.exoplayer.trackselection.TrackSelectionOverride\n", "")

# 2. Add material3 imports
imports = """import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
"""
content = content.replace("import androidx.tv.material3.*", "import androidx.tv.material3.*\n" + imports)

# 3. Replace Card with Surface
card_str = '''Card(
                    modifier = Modifier.width(400.dp).clickable(enabled = false) {},
                    colors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                )'''
surface_str = '''Surface(
                    modifier = Modifier.width(400.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )'''
content = content.replace(card_str, surface_str)

with open(r'c:\Users\Hama9\Desktop\ultra-tv-main\android-native\app\src\main\kotlin\com\ultratv\tv\nativeapp\ui\player\AdvancedVideoPlayer.kt', 'w', encoding='utf-8') as f:
    f.write(content)
