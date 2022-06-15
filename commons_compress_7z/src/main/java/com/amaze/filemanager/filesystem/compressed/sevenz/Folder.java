/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.compressed.sevenz;

import java.util.LinkedList;

/** The unit of solid compression. */
class Folder {
  /// List of coders used in this folder, eg. one for compression, one for encryption.
  Coder[] coders;
  /// Total number of input streams across all coders.
  /// this field is currently unused but technically part of the 7z API
  long totalInputStreams;
  /// Total number of output streams across all coders.
  long totalOutputStreams;
  /// Mapping between input and output streams.
  BindPair[] bindPairs;
  /// Indeces of input streams, one per input stream not listed in bindPairs.
  long[] packedStreams;
  /// Unpack sizes, per each output stream.
  long[] unpackSizes;
  /// Whether the folder has a CRC.
  boolean hasCrc;
  /// The CRC, if present.
  long crc;
  /// The number of unpack substreams, product of the number of
  /// output streams and the nuber of non-empty files in this
  /// folder.
  int numUnpackSubStreams;

  /**
   * Sorts Coders using bind pairs.
   *
   * <p>The first coder reads from the packed stream (we currently only support single input stream
   * decoders), the second reads from the output of the first and so on.
   */
  Iterable<Coder> getOrderedCoders() {
    final LinkedList<Coder> l = new LinkedList<>();
    int current = (int) packedStreams[0]; // more that 2^31 coders?
    while (current != -1) {
      l.addLast(coders[current]);
      final int pair = findBindPairForOutStream(current);
      current = pair != -1 ? (int) bindPairs[pair].inIndex : -1;
    }
    return l;
  }

  int findBindPairForInStream(final int index) {
    for (int i = 0; i < bindPairs.length; i++) {
      if (bindPairs[i].inIndex == index) {
        return i;
      }
    }
    return -1;
  }

  int findBindPairForOutStream(final int index) {
    for (int i = 0; i < bindPairs.length; i++) {
      if (bindPairs[i].outIndex == index) {
        return i;
      }
    }
    return -1;
  }

  long getUnpackSize() {
    if (totalOutputStreams == 0) {
      return 0;
    }
    for (int i = ((int) totalOutputStreams) - 1; i >= 0; i--) {
      if (findBindPairForOutStream(i) < 0) {
        return unpackSizes[i];
      }
    }
    return 0;
  }

  long getUnpackSizeForCoder(final Coder coder) {
    if (coders != null) {
      for (int i = 0; i < coders.length; i++) {
        if (coders[i] == coder) {
          return unpackSizes[i];
        }
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    return "Folder with "
        + coders.length
        + " coders, "
        + totalInputStreams
        + " input streams, "
        + totalOutputStreams
        + " output streams, "
        + bindPairs.length
        + " bind pairs, "
        + packedStreams.length
        + " packed streams, "
        + unpackSizes.length
        + " unpack sizes, "
        + (hasCrc ? "with CRC " + crc : "without CRC")
        + " and "
        + numUnpackSubStreams
        + " unpack streams";
  }
}
