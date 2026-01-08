#!/usr/bin/env python

# SPDX-License-Identifier: BSD-3-Clause
# Copyright (c) 2025, UChicago Argonne, LLC.
# Main author: Kazutomo Yoshii <kazutomo@anl.gov>. See LICENSE in project root.

from skimage.io import imread, imsave
import numpy as np
import math as m
import sys

print(len(sys.argv))
if len(sys.argv) < 2:
    print('Usage: npy2png.py filename')
    sys.exit(1)

datafn=sys.argv[1]
print(f"datafn={datafn}")

images = np.load(datafn)['images']
print(f"images.shape={images.shape}")

imgpxs = images[0].astype(np.uint16)
imsave('check.png', imgpxs)

