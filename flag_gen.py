#!/usr/bin/env python3

import os

nodes = os.listdir()

result = "hashMapOf(\n"
for f in nodes:
    basename = os.path.basename(f)
    if (len(basename) < 4 or basename[-4:] != '.png'):
        continue
    result += "    \"" + basename[:-4] + "\" to R.drawable." + basename[:-4] + ", \n"
result = result[:-3] + ")"
out = open("out.txt", "w")
out.write(result)
out.close()

